package bgu.spl.net.api;
import java.util.ArrayDeque;
import java.util.Arrays;
import bgu.spl.net.Message;
import java.nio.charset.StandardCharsets;

import bgu.spl.net.User;
import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.impl.rci.ObjectEncoderDecoder;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.LinkedList;

public class MessageEncoderDecoderImpl<T> implements MessageEncoderDecoder<T>{

    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(2);
    private byte[] objectBytes = null;
    private int objectBytesIndex = 0;
    private Message msg;
    private User currentUser;
    String command;

    public MessageEncoderDecoderImpl(){
        this.msg = new Message((short)-1);
        command = "";
    }
    @Override
    public T decodeNextByte(byte nextByte) {
        char next = (char)nextByte;
        if ((nextByte == 32 && msg.getOpCode() == -1) || (nextByte == '\n' && msg.getOpCode() == -1)){//end of opCode part
            msg.setOpCode(createOpCode(command));
            command = "";
            if (msg.getOpCode() == 7 || msg.getOpCode() == 3){
                Message tmp = msg;
                clear();
                return (T) tmp;
            }
            return null;
        }
        else{
            if(msg.getOpCode() == -1){
                if (nextByte != 0) {
                    command = command + next;
                }
                return null;

            }
            else{
                return decodeByCases(next);
            }
        }
    }

    private T decodeByCases(char nextByte) {
        switch (msg.getOpCode()){
            case 3:
            case 7:
                if (nextByte == '\n'){
                    Message tmp = msg;
                    clear();
                    return (T) tmp;
                }
                break;
            case 5:
            case 8:
            case 12:
                //POST, STAT, BLOCK - all have 1 string
                if (nextByte == '\n'){
                    msg.setNextString(command);
                    Message tmp = msg;
                    clear();
                    return (T) tmp;
                }
                else{
                    command = command + nextByte;
                }
                break;
            case 2: //2 strings and captcha
                if (nextByte == ' ' || nextByte == '\n') {
                    if (nextByte == ' '){
                        msg.setNextString(command);
                        command = "";
                    }
                    else{
                        char c = command.charAt(0);
                        msg.setBinaryOp((byte)c);
                        Message tmp = msg;
                        clear();
                        return (T) tmp;
                    }
                }
                else{
                    command = command + nextByte;
                }
                break;
            case 1://3 strings
                if (nextByte == ' ' || nextByte == '\n') {
                    msg.setNextString(command);
                    command = "";
                    if (nextByte == '\n'){
                        Message tmp = msg;
                        clear();
                        return (T) tmp;
                    }
                }
                else{
                    command = command + nextByte;
                }
                break;
            case 6: //2 strings
                if (msg.getStrings()[0] == null) {
                    if (nextByte == ' ' || nextByte == '\n') {
                        msg.setNextString(command);
                        command = "";
                        if (nextByte == '\n') {
                            Message tmp = msg;
                            clear();
                            return (T) tmp;
                        }
                    }
                    else {
                        command = command + nextByte;
                    }
                }
                else{
                    if (nextByte == '\n') {
                        msg.setNextString(command);
                        command = "";
                        Message tmp = msg;
                        clear();
                        return (T) tmp;
                    }
                    else {
                        command = command + nextByte;
                    }
                }
                break;
            case 4: //1 binary, 1 string
                if (nextByte == ' ' || nextByte == '\n') {
                    if (nextByte == ' '){
                        char c = command.charAt(0);
                        msg.setBinaryOp((byte) c);
                        command = "";
                    }
                    else{
                        msg.setNextString(command);
                        Message tmp = msg;
                        clear();
                        return (T) tmp;
                    }
                }
                else{
                    command = command + nextByte;
                }
                break;
        }
        return null;
    }

    @Override
    public byte[] encode(T message) {
        //10 - ack - 2 bytes opCode, 2 bytes messageOpCode
        //11 - error - 2 bytes opCode, 2 bytes messageOpCode
        Message toEncode =  (Message) message;
        short opCode = toEncode.getOpCode();
        switch (opCode){
            case 9: //NOTIFICATION
                return encodeNotification(toEncode);

            case 10: //ACK
                return encodeACK(toEncode);
            case 11: //ERROR
                return encodeERROR(toEncode);
        }
        return new byte[0];
    }
    private byte[] encodeERROR(Message message){
        short opCode = message.getOpCode();
        short msgOpCode = message.getShorts()[0];
        String str = opCode + " " + msgOpCode + ";";
        return str.getBytes();
    }
    private byte[] encodeACK(Message message){
        short opCode = message.getOpCode();
        short msgOpCode = message.getShorts()[0];
        String str = opCode + " " + msgOpCode;
        //checking cases
        if (msgOpCode == 4){//follow/unfollow
            String userName = message.getStrings()[0];
            str = str + " " + userName + ";";
//            System.out.println(str);
            return str.getBytes();
        }
        else{
            if (msgOpCode == 7 || msgOpCode == 8){//logStat|stat
                str = str + " " + message.getShorts()[1] + " " + message.getShorts()[2] + " " + message.getShorts()[3] + " " + message.getShorts()[4] + ";";
                return str.getBytes();
            }
            else{
                str = str + ";";
//                System.out.println(str);
                return str.getBytes();
            }
        }
    }
    private byte[] encodeNotification(Message message){
//        char c = (char) message.getBinaryOp();
        int i = 0;
        if (message.getBinaryOp() == (byte) 0){
            i = 0;
        }
        else{
            i = 1;
        }
        String str = message.getOpCode() + " " + i + " " + message.getStrings()[0] + " " + message.getStrings()[1] + ";";
//        System.out.println(str);
        return str.getBytes();
    }

    public void clear(){
        objectBytes = null;
        msg = new Message((short) -1);
        command = "";
        objectBytesIndex = 0;
    }
    public String arrayToStringConverter(ArrayDeque<Byte> array){
        StringBuilder str = new StringBuilder();
        for (byte b : array){
            str.append(b);
            array.removeFirst();
        }
        return str.toString();
    }
    public byte arrayToByteConverter(ArrayDeque<Byte> array){

        return array.removeFirst();
    }
    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }
    public short bytesToShort(byte[] byteArr)
    {
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }
    public short createOpCode(String str){
        if (str.equals("REGISTER")){
            return  1;
        }
        if (str.equals("LOGIN")){
            return 2;
        }
        if (str.equals("LOGOUT")){
            return 3;
        }
        if (str.equals("FOLLOW")){
            return 4;
        }
        if (str.equals("POST")){
            return 5;
        }
        if (str.equals("PM")){
            return 6;
        }
        if (str.equals("LOGSTAT")){
            return 7;
        }
        if (str.equals("STAT")){
            return 8;
        }
        if (str.equals("NOTIFICATION")){
            return 9;
        }
        if (str.equals("ACK")){
            return 10;
        }
        if (str.equals("ERROR")){
            return 11;
        }
        if (str.equals("BLOCK")){
            return 12;
        }
        return 0;
    }
}
