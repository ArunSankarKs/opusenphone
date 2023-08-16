package dev.dacbiet.opusenclient.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Packet containing apdu command(s).
 */
public class ApduDataPacket extends DataPacket {

    private int connectionId = 0;
    private byte connectionState = 0;
    private final List<Apdu> commands = new ArrayList<>();

    public int getConnectionId() {
        return this.connectionId;
    }

    public byte getConnectionState() {
        return this.connectionState;
    }

    public List<Apdu> getCommands() {
        return new ArrayList<>(this.commands);
    }

    @Override
    public void deserialize(ByteBuffer buf) {
        super.deserialize(buf);

        this.connectionId = buf.getInt();
        this.connectionState = buf.get();

        // skip next 2 (unused reserved bytes)
        buf.get();
        buf.get();

        byte count = buf.get();
        for (int i = 0; i < count; i++) {
            this.commands.add(new Apdu(buf));
        }
    }

}
