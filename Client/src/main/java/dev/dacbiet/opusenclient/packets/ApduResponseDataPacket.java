package dev.dacbiet.opusenclient.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet containing responses to apdu command(s).
 */
public class ApduResponseDataPacket extends DataPacket {

    private final byte isConnectionActive;
    private final int connectionId;
    private final List<Apdu> responses;

    public ApduResponseDataPacket(boolean isConnectionActive, int connectionId, byte msgNum, List<Apdu> apduResps) {
        super((byte) 16, (byte) -17, msgNum);
        this.isConnectionActive = (byte) (isConnectionActive ? 1 : 0); // 1 if active
        this.connectionId = connectionId;
        this.responses = new ArrayList<>(apduResps);
    }

    @Override
    public byte[] serialize() {
        int bufLen = 11;

        for (Apdu apdu : this.responses) {
            bufLen += apdu.getData().length + 1;
        }

        this.setLength((short) bufLen);
        ByteBuffer buf = ByteBuffer.allocate(bufLen + 4);

        buf.put(super.serialize());

        buf.putInt(this.connectionId);
        buf.put(this.isConnectionActive);

        // reserved bytes (unused)
        buf.put((byte) 0);
        buf.put((byte) 0);

        buf.put((byte) this.responses.size());
        for (Apdu apdu: this.responses) {
            byte[] apduData = apdu.serialize();
            buf.put((byte) apduData.length);
            buf.put(apduData);
        }

        buf.put((byte) 0); // comment none

        buf.put((byte) 0); // (non-zero means issued occurred)

        return Packet.bufToBytes(buf);
    }

}
