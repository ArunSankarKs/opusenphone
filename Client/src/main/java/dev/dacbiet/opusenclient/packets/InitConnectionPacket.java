package dev.dacbiet.opusenclient.packets;

import java.nio.ByteBuffer;

import static dev.dacbiet.opusenclient.Client.XSCP_VERSION;

/**
 * First packet to send to server.
 */
public class InitConnectionPacket implements Packet {

    private static final String OS = "Windows 10";

    byte msgNum;
    private final int opId;
    short type;
    private final String terminal;
    private final byte[] atr;

    public InitConnectionPacket(byte msgNum, int opId, short type, String terminal, byte[] atr) {
        this.msgNum = msgNum;
        this.opId = opId;
        this.type = type;
        this.terminal = terminal;
        this.atr = atr;
    }


    @Override
    public byte[] serialize() {
        int lenAllReadersStr = terminal.length();

        short dataLen = 4;
        byte extraCount = 0; // how many extras i guess

        if((this.type & 1) != 0) {
            dataLen += 6;
            extraCount++;
        }

        if((this.type & 2) != 0) {
            dataLen += 2 + atr.length;
            extraCount++;
        }

        if((this.type & 4) != 0) {
            dataLen += 2 + terminal.getBytes().length;
            extraCount++;
        }

        if((this.type & 8) != 0) {
            dataLen += 2 + terminal.getBytes().length;
            extraCount++;
        }

        if((this.type & 16) != 0) {
            dataLen += 2 + OS.getBytes().length;
            extraCount++;
        }

        if((this.type & 32) != 0) {
            dataLen += 2 + XSCP_VERSION.getBytes().length;
            extraCount++;
        }


        ByteBuffer buf = ByteBuffer.allocate(dataLen + 4);
        buf.put((byte) 48);
        buf.put((byte) -49);
        buf.put((byte) (dataLen / 256));
        buf.put((byte) dataLen);
        buf.put(this.msgNum);

        buf.put(extraCount);

        if((this.type & 1) != 0) {
            buf.put((byte) 1);
            buf.put((byte) 4);
            Packet.serializeInt(buf, this.opId); // int data[8-11] 4 bytes = opId
        }

        if((this.type & 2) != 0) {
            buf.put((byte) 2);
            buf.put((byte) atr.length);
            buf.put(atr);
        }

        if((this.type & 4) != 0) {
            buf.put((byte) 3);
            buf.put((byte) terminal.getBytes().length);
            buf.put(terminal.getBytes());
        }

        if((this.type & 8) != 0) {
            buf.put((byte) 4);
//        buf.put((byte) 1); // 1 card reader
            buf.put((byte) terminal.getBytes().length);
            buf.put(terminal.getBytes());
        }

        if((this.type & 16) != 0) {
            buf.put((byte) 5);
            buf.put((byte) OS.getBytes().length);
            buf.put(OS.getBytes());
        }

        if((this.type & 32) != 0) {
            buf.put((byte) 6);
            buf.put((byte) XSCP_VERSION.getBytes().length);
            buf.put(XSCP_VERSION.getBytes());
        }

        buf.put((byte) 0); // no comments
        buf.put((byte) 0); // always zero
        return buf.array();
    }

    @Override
    public void deserialize(ByteBuffer buf) {

    }

}
