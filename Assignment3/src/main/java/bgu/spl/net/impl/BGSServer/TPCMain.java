package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.DataBase;
import bgu.spl.net.api.MessageEncoderDecoderImpl;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;
import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args) {
        DataBase dataBase = new DataBase();
        Server.threadPerClient(Integer.parseInt(args[0]), ()-> new BidiMessagingProtocolImpl(dataBase), ()-> new MessageEncoderDecoderImpl()).serve();
    }
}
