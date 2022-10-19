package bgu.spl.net;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;


public class DataBase {
    private ConcurrentHashMap <User, String> user2password;
    private ConcurrentHashMap <User, ConcurrentLinkedDeque<User>> userFollowings;//users the user is following
    private ConcurrentHashMap <User, ConcurrentLinkedDeque<User>> userFollowers;//users that follow the user
    private ConcurrentHashMap <User, ConcurrentLinkedDeque<Message>> unseenMessages;//messages sent when user is loggedOut
    private ConcurrentHashMap <User, Integer> userConnectionID;//user2id
    private ConcurrentHashMap <User, ConcurrentLinkedDeque<Message>> postSent;
    private ConcurrentHashMap <User, ConcurrentLinkedDeque<Message>> pmSent;
    private ConcurrentHashMap <User, ConcurrentLinkedDeque<User>> blocking;//user - blocking
    private ConcurrentLinkedDeque <User> registeredUsers;
    private LinkedList<String> filteredWords;

    public DataBase() {
        this.user2password = new ConcurrentHashMap<>();
        this.userFollowings = new ConcurrentHashMap<>();
        this.userFollowers = new ConcurrentHashMap<>();
        this.unseenMessages = new ConcurrentHashMap<>();
        this.userConnectionID = new ConcurrentHashMap<>();
        this.postSent = new ConcurrentHashMap<>();
        this.pmSent = new ConcurrentHashMap<>();
        this.blocking = new ConcurrentHashMap<>();
        this.registeredUsers = new ConcurrentLinkedDeque<>();
        this.filteredWords = new LinkedList<>();

    }

    public ConcurrentHashMap<User, String> getUser2password() {
        return user2password;
    }

    public ConcurrentHashMap<User, ConcurrentLinkedDeque<User>> getUserFollowings() {
        return userFollowings;
    }

    public ConcurrentHashMap<User, ConcurrentLinkedDeque<User>> getUserFollowers() {
        return userFollowers;
    }

    public ConcurrentHashMap<User, ConcurrentLinkedDeque<Message>> getUnseenMessages() {
        return unseenMessages;
    }

    public ConcurrentHashMap<User, Integer> getUserConnectionID() {
        return userConnectionID;
    }

    public ConcurrentHashMap<User, ConcurrentLinkedDeque<Message>> getPostSent() {
        return postSent;
    }

    public ConcurrentHashMap<User, ConcurrentLinkedDeque<Message>> getPmSent() {
        return pmSent;
    }

    public ConcurrentLinkedDeque<User> getRegisteredUsers() {
        return registeredUsers;
    }
    public LinkedList getFilteredWords(){
        return filteredWords;
    }

    public ConcurrentHashMap<User, ConcurrentLinkedDeque<User>> getBlocking() {
        return blocking;
    }

    public boolean isRegistered(String userName) {
        for (User user : registeredUsers){
            if (user.getUserName().equals(userName)){
                return true;
            }
        }
        return false;
    }
    public User getUserIfRegistered(String userName){
        for (User user : registeredUsers){
            if (user.getUserName().equals(userName)){
                return user;
            }
        }
        return null;
    }
}
