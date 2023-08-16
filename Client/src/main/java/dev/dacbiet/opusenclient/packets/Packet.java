package dev.dacbiet.opusenclient.packets;

import java.nio.ByteBuffer;

public interface Packet {

    byte[] serialize();
    void deserialize(ByteBuffer buf);
    
    static void serializeInt(ByteBuffer buf, int num) {
        for (int i = 0; i < 4; i++) {
            buf.put((byte) (num >> 24 - i * 8 & 255));
        }
    }

    static byte[] bufToBytes(ByteBuffer buf) {
        byte[] out = new byte[buf.position()];
        buf.rewind();
        buf.get(out);
        return out;
    }

    static long bytesToLong(byte[] bytes) {
        long num = 0;
        for (byte b : bytes) {
            num <<= 8; // shift by 8 bits
            num += b;

            if (b < 0) {
                 num += 256; // offset negative byte
            }
        }

        return num;
    }
}
