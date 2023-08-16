package dev.dacbiet.opusenclient.packets;

import java.nio.ByteBuffer;

/**
 * Base packet for server.
 */
public class DataPacket implements Packet {

    private byte type;
    private byte cmd;
    private short length;
    private byte msgNum;
    private byte[] data;

    public DataPacket() {
    }

    public DataPacket(byte type, byte cmd, byte msgNum) {
        this.type = type;
        this.cmd = cmd;
        this.msgNum = msgNum;
    }


    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getCmd() {
        return cmd;
    }

    public void setCmd(byte cmd) {
        this.cmd = cmd;
    }

    public short getLength() {
        return length;
    }

    public void setLength(short length) {
        this.length = length;
    }

    public byte getMsgNum() {
        return msgNum;
    }

    public void setMsgNum(byte msgNum) {
        this.msgNum = msgNum;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(5);

        buf.put(this.type);
        buf.put(this.cmd);
        buf.put((byte) (this.length / 256));
        buf.put((byte) this.length);
        buf.put(this.msgNum);

        return Packet.bufToBytes(buf);
    }

    @Override
    public void deserialize(ByteBuffer buf) {
        this.type = buf.get();
        this.cmd = buf.get();

        int lenMod = buf.get();
        this.length = (short) (lenMod * 256 + buf.get());
        this.msgNum = buf.get();
        this.data = buf.array();
    }
}
