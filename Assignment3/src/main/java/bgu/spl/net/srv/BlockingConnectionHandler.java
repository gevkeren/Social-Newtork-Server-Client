package bgu.spl.net.srv;

import bgu.spl.net.Message;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessageEncoderDecoderImpl;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.bidi.BidiMessagingProtocolImpl;
import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.api.bidi.ConnectionsImpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {
    //for the tpc
    private final BidiMessagingProtocolImpl<T> protocol;
    private final MessageEncoderDecoderImpl<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private int connectionID;


    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoderImpl<T> reader, BidiMessagingProtocolImpl<T> protocol, int connectionID) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connectionID = connectionID;
        ConnectionsImpl.getInstance();
        protocol.start(connectionID, ConnectionsImpl.getInstance());
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
//                    Message m = (Message) nextMessage;
//                    System.out.println(((Message) nextMessage).getOpCode());
                    protocol.process(nextMessage);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        try{
            out = new BufferedOutputStream(sock.getOutputStream());
            if (msg != null){
                //System.out.println(encdec.encode(msg));
                out.write(encdec.encode(msg));
                out.flush();
            }
        }catch (IOException exception){
            exception.printStackTrace();
        }

    }
}
