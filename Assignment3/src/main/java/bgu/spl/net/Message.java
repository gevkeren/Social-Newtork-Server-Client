package bgu.spl.net;

import java.util.ArrayDeque;

public class Message {
    private short opCode;
    private byte binaryOp;//necessary for NOTIFICATION, FOLLOW, ERROR, ACK,
    private String[] strings;//UserName or
    private short[] shorts;
    int StrArgumentIndex;
    int shortArgumentIndex;
    private User sentUser;

    public Message(short opCode){
        this.opCode = opCode;
        this.binaryOp = 0;
        this.strings = new String[3];
        this.shorts = new short[5];
        this.StrArgumentIndex = 0;
        this.shortArgumentIndex = 0;
    }
    public void setNextString(String str){
        this.strings[StrArgumentIndex] = str;
        StrArgumentIndex++;
    }
    public void setNextShort(short num){
        this.shorts[shortArgumentIndex] = num;
        shortArgumentIndex++;
    }

    public void setOpCode(short opCode) {
        this.opCode = opCode;
    }

    public void setBinaryOp(byte binaryOp) {
        this.binaryOp = binaryOp;
    }

    public short getOpCode() {
        return opCode;
    }

    public byte getBinaryOp() {
        return binaryOp;
    }

    public String[] getStrings() {
        return strings;
    }

    public short[] getShorts() {
        return shorts;
    }
}
