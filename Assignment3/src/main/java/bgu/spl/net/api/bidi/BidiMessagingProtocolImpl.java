package bgu.spl.net.api.bidi;

import bgu.spl.net.DataBase;
import bgu.spl.net.Message;
import bgu.spl.net.User;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;

public class BidiMessagingProtocolImpl <T> implements BidiMessagingProtocol<T>{
    private int connectionID;
    private String userName;
    private DataBase dataBase;
    boolean shouldTerminate;
    private Connections connections;
    private User curUser;

    public BidiMessagingProtocolImpl(DataBase dataBase){
        this.dataBase = dataBase;
        shouldTerminate = false;
        userName = "";
        this.curUser = null;
    }
    @Override
    public void start(int connectionId, Connections<T> connections) {
        this.connectionID = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(T message) {
        Message msg = (Message) message;
        switch (msg.getOpCode()){
            case 1: //REGISTER
                if (dataBase.isRegistered(msg.getStrings()[0])){//already registered - error
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 1);
                    connections.send(connectionID, error);
                    break;
                }
                else {//registered succeed - should return an ack
                    User newUser = new User(msg.getStrings()[0], msg.getStrings()[1], msg.getStrings()[2]);
                    synchronized (dataBase.getUser2password()){
                        dataBase.getUser2password().put(newUser, newUser.getPassword());
                        dataBase.getUserFollowings().put(newUser, new ConcurrentLinkedDeque<>());
                        dataBase.getUserFollowers().put(newUser, new ConcurrentLinkedDeque<>());
                        dataBase.getUnseenMessages().put(newUser, new ConcurrentLinkedDeque<>());
                        dataBase.getUserConnectionID().put(newUser, connectionID);
                        dataBase.getPostSent().put(newUser, new ConcurrentLinkedDeque<>());
                        dataBase.getBlocking().put(newUser, new ConcurrentLinkedDeque<>());
                        dataBase.getPmSent().put(newUser, new ConcurrentLinkedDeque<>());
                        dataBase.getRegisteredUsers().add(newUser);
                    }
                    Message ack = new Message((short) 10);
                    ack.setNextShort((short) 1);
                    connections.send(connectionID, ack);
                    break;
                }
            case 2: //LOGIN
                User logInUser = dataBase.getUserIfRegistered(msg.getStrings()[0]);
                String password = msg.getStrings()[1];
                if ( logInUser == null
                        || ! dataBase.getUser2password().get(logInUser).equals(password)
                        || msg.getBinaryOp() == 48
                        || ConnectionsImpl.getInstance().getLoginUsers().contains(logInUser) ){//error
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 2);
                    connections.send(connectionID, error);
                    break;
                }
                else{//LOGIN succeed - ack
                    synchronized (dataBase.getUserConnectionID()){
                        ConnectionsImpl.getInstance().connectUser(logInUser);
                        logInUser.logIn();
                        this.curUser = logInUser;
                        dataBase.getUserConnectionID().put(logInUser, connectionID);
                        Message ack = new Message((short) 10);
                        ack.setNextShort((short) 2);
                        connections.send(connectionID, ack);
                        ConcurrentLinkedDeque<Message> unseenMsg = dataBase.getUnseenMessages().get(logInUser);
                        while (! unseenMsg.isEmpty()){
                            connections.send(connectionID, unseenMsg.poll());
                        }
                    }
                    break;
                }
            case 3: //LOGOUT
                if (curUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser)){//error
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 3);
                    connections.send(connectionID, error);
                    break;
                }
                else{//LOGOUT succeed - ack
                    synchronized (dataBase.getUserConnectionID()){
                        Message ack = new Message((short) 10);
                        ack.setNextShort((short) 3);
                        connections.send(connectionID, ack);
                        ConnectionsImpl.getInstance().disconnectUser(curUser);
                        curUser.logOut();
//                        shouldTerminate = true;
                        break;
                    }
                }

            case 4://FOLLOW/UNFOLLOW
                User user2Follow = dataBase.getUserIfRegistered(msg.getStrings()[0]);
                if (msg.getBinaryOp() == 48){//Follow
                    if (user2Follow == null || curUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser)
                            || dataBase.getUserFollowings().get(curUser).contains(user2Follow)
                            || dataBase.getBlocking().get(curUser).contains(user2Follow)
                            || dataBase.getBlocking().get(user2Follow).contains(curUser)){//error
                        Message error = new Message((short) 11);
                        error.setNextShort((short) 4);
                        connections.send(connectionID, error);
                        break;
                    }
                    else{//ack
                        dataBase.getUserFollowings().get(curUser).add(user2Follow);
                        dataBase.getUserFollowers().get(user2Follow).add(curUser);
                        Message ack = new Message((short) 10);
                        ack.setNextShort((short) 4);
                        ack.setNextString(user2Follow.getUserName());
                        connections.send(connectionID, ack);
                        break;
                    }
                }
                else{//UnFollow
                    if (user2Follow == null || curUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser) || (! dataBase.getUserFollowings().get(curUser).contains(user2Follow)) ){//error
                        Message error = new Message((short) 11);
                        error.setNextShort((short) 4);
                        connections.send(connectionID, error);
                        break;
                    }
                    else{//ack
                        dataBase.getUserFollowings().get(curUser).remove(user2Follow);
                        dataBase.getUserFollowers().get(user2Follow).remove(curUser);
                        Message ack = new Message((short) 10);
                        ack.setNextShort((short) 4);
                        ack.setNextString(user2Follow.getUserName());
                        connections.send(connectionID, ack);
                        break;
                    }
                }
            case 5: //POST
                if (curUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser)){//error
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 5);
                    connections.send(connectionID, error);
                    break;
                }
                else{//ACK & NOTIFICATIO
                    Message notification = new Message((short) 9);
                    notification.setNextString(curUser.getUserName());
                    notification.setNextString(msg.getStrings()[0]);
                    notification.setBinaryOp((byte) 1);

                    sendNotificationToShtrudels(msg, notification);

                    dataBase.getPostSent().get(curUser).add(msg);
                    synchronized (dataBase.getUserConnectionID()) {
                        for (User user : dataBase.getUserFollowers().get(curUser)) {
                            if (!user.isLoggedIn()) {
                                dataBase.getUnseenMessages().get(user).add(notification);
                            } else {
                                connections.send(dataBase.getUserConnectionID().get(user), notification);
                            }
                        }
                    }
                    Message ack = new Message((short) 10);
                    ack.setNextShort((short) 5);
                    connections.send(connectionID, ack);
                    break;
                }
            case 6: //PM
                User pmUser = dataBase.getUserIfRegistered(msg.getStrings()[0]);
                if (pmUser == null || curUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser) || ! dataBase.getUserFollowings().get(curUser).contains(pmUser)){//ERROR
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 6);
                    connections.send(connectionID, error);
                    break;
                }
                else{//ACK & NOTIFICATION
                    Message ack = new Message((short) 10);
                    ack.setNextShort((short) 6);
                    connections.send(connectionID, ack);

                    Message notification = new Message((short) 9);
                    notification.setNextString(curUser.getUserName());
                    String newContent = filterContent(msg);
                    notification.setNextString(newContent);
                    notification.setNextString(msg.getStrings()[2]);
                    notification.setBinaryOp((byte) 0);
                    synchronized (dataBase.getUserConnectionID()) {
                        int userId = dataBase.getUserConnectionID().get(pmUser);
                        if (! pmUser.isLoggedIn()){
                            dataBase.getUnseenMessages().get(pmUser).add(notification);
                        }
                        else{
                            connections.send(userId, notification);
                        }
                    }
                    break;
                }
            case 7: //LOGSTAT
                if (curUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser)){
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 7);
                    connections.send(connectionID, error);
                    break;
                }
                for (User user : dataBase.getRegisteredUsers()){//all registered users
                    if (user.isLoggedIn()){//user is logged in
                        if (! dataBase.getBlocking().get(curUser).contains(user)
                                && ! dataBase.getBlocking().get(user).contains(curUser)){//neither is blocked
                            Message ack = new Message((short) 10);
                            ack.setNextShort((short) 7);
                            ack.setNextShort(user.getAge());
                            ack.setNextShort((short) dataBase.getPostSent().get(user).size());
                            ack.setNextShort((short) dataBase.getUserFollowers().get(user).size());
                            ack.setNextShort((short) dataBase.getUserFollowings().get(user).size());
                            connections.send(connectionID, ack);
                        }
                    }
                }
                break;
            case 8: //STAT
                String usernames = msg.getStrings()[0];
                if (curUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser)){
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 8);
                    connections.send(connectionID, error);
                    break;
                }
                else {
                    LinkedList<Message> acks = new LinkedList<>();
                    for (User user : dataBase.getRegisteredUsers()) {
                        if (usernames.contains(user.getUserName())) {
                            if (dataBase.getBlocking().get(curUser).contains(user)
                                    || dataBase.getBlocking().get(user).contains(curUser)) {
                                Message error = new Message((short) 11);
                                error.setNextShort((short) 8);
                                connections.send(connectionID, error);
                                break;
                            }
                            else{
                                Message ack = new Message((short) 10);
                                ack.setNextShort((short) 8);
                                ack.setNextShort(user.getAge());
                                ack.setNextShort((short) dataBase.getPostSent().get(user).size());
                                ack.setNextShort((short) dataBase.getUserFollowers().get(user).size());
                                ack.setNextShort((short) dataBase.getUserFollowings().get(user).size());
                                acks.add(ack);
                            }
                        }
                    }
                    for (Message m : acks){
                        connections.send(connectionID, m);
                    }
                    break;
                }
            case 12: //BLOCK
                User blockedUser = dataBase.getUserIfRegistered(msg.getStrings()[0]);
                if (blockedUser == null || ! ConnectionsImpl.getInstance().getLoginUsers().contains(curUser)){
                    Message error = new Message((short) 11);
                    error.setNextShort((short) 13);
                    connections.send(connectionID, error);
                    break;
                }
                else {
                    dataBase.getBlocking().get(curUser).add(blockedUser);

                    dataBase.getUserFollowings().get(curUser).remove(blockedUser);
                    dataBase.getUserFollowings().get(blockedUser).remove(curUser);

                    dataBase.getUserFollowers().get(curUser).remove(blockedUser);
                    dataBase.getUserFollowers().get(blockedUser).remove(curUser);

                    Message ack = new Message((short) 10);
                    ack.setNextShort((short) 12);
                    connections.send(connectionID, ack);
                    break;
                }
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
    private String filterContent(Message msg) {
        String content = msg.getStrings()[1];
        String newContent = "";
        String word = "";
        int end = 0;
        int begin = 0;
        int length = content.length();
        boolean addLastChar = true;
        while (end < length) {
            char c = content.charAt(end);
            if (c != ' ' && c != ',' && c != '?' && c != '!' && c != '.' && end != length - 1) {
                end++;
            } else {//end of word
                word = content.substring(begin, end);
                if (end == length - 1 && c != ' ' && c != ',' && c != '?' && c != '!' && c != '.') {
                    word = word + c;
                    addLastChar = false;
                }
                if (dataBase.getFilteredWords().contains(word)) {
                    newContent = newContent + "<filtered>";
                } else {
                    newContent = newContent + word;
                }
                if (end != length - 1 && addLastChar) {
                    newContent = newContent + c;
                }
                end++;
                begin = end;
            }
        }
        return newContent;
    }
    private void sendNotificationToShtrudels(Message msg, Message notification){
        ConcurrentLinkedDeque<String> shtrudel = new ConcurrentLinkedDeque<>();
        String content = msg.getStrings()[0];
        while (content.contains("@")){
            int index = content.indexOf("@");
            content = content.substring(index+1);
            if (content.contains(" ")){
                int endIndex = content.indexOf(" ");
                String name = content.substring(0, endIndex);
                content = content.substring(endIndex+1);
                shtrudel.add(name);
            }
            else{//in case the name is at the end of the message
                String name = content;
                shtrudel.add(name);
            }
        }
        for (String name : shtrudel){
            for (User u : dataBase.getRegisteredUsers()){
                if (u.getUserName().equals(name) && ! dataBase.getBlocking().get(curUser).contains(u)
                        && ! dataBase.getBlocking().get(u).contains(curUser)){
                    if (! u.isLoggedIn()){
                        dataBase.getUnseenMessages().get(u).add(notification);
                    }
                    else{
                        connections.send(dataBase.getUserConnectionID().get(u), notification);
                    }
                }
            }
        }
    }
}
