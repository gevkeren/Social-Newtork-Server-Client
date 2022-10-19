package bgu.spl.net.api.bidi;

import bgu.spl.net.Message;
import bgu.spl.net.User;
import bgu.spl.net.srv.ConnectionHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConnectionsImpl<T> implements Connections<T>{

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> int2Con;//userID - connectionHandler
    private ConcurrentLinkedDeque<User> loginUsers;

    private static class ConnectionsImplHolder {// Singleton
        private static ConnectionsImpl instance = new ConnectionsImpl();
    }
    public static ConnectionsImpl getInstance(){
        if(ConnectionsImplHolder.instance == null){
            ConnectionsImplHolder.instance = new ConnectionsImpl();
        }
        return ConnectionsImplHolder.instance;
    }

    public ConnectionsImpl(){

        this.int2Con = new ConcurrentHashMap<>();
        this.loginUsers = new ConcurrentLinkedDeque<>();
    }
    @Override
    public boolean send(int connectionId, T msg) {
        synchronized (int2Con){
            Message m = (Message) msg;
            if (int2Con.containsKey(connectionId)){
//                System.out.println("sent respond" + m.getOpCode());
                int2Con.get(connectionId).send(msg);
                return true;
            }
            return false;
        }
    }

    @Override
    public void broadcast(T msg) {
        for(ConnectionHandler<T> conH : int2Con.values()){
            conH.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        int2Con.remove(connectionId);
    }

    public void addToCon(Integer id, ConnectionHandler con){
        int2Con.put(id, con);
    }

    public ConcurrentLinkedDeque<User> getLoginUsers() {
        return loginUsers;
    }
    public void connectUser(User user){
        loginUsers.add(user);
    }
    public void disconnectUser(User user){
        loginUsers.remove(user);
    }
}
