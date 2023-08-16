package dev.dacbiet.opusenclient.packets;

import java.nio.ByteBuffer;

public class Apdu implements Packet {

    private byte[] data;

    /**
     * Apdu data.
     *
     * @param data data
     */
    public Apdu(byte[] data) {
        this.data = data;
    }

    /**
     * Deserialize constructor.
     *
     * @param buf buffer of data
     */
    public Apdu(ByteBuffer buf) {
        this.deserialize(buf);
    }

    public byte[] getData() {
        return this.data;
    }

    @Override
    public byte[] serialize() {
        byte[] out = new byte[this.data.length];
        System.arraycopy(this.data, 0, out, 0, this.data.length);
        return out;
    }

    @Override
    public void deserialize(ByteBuffer buf) {
        byte length = buf.get();
        byte[] data = new byte[length];
        buf.get(data, 0, length);

        this.data = data;
    }
}
