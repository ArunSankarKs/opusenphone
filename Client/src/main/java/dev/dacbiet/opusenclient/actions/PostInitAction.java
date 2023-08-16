package dev.dacbiet.opusenclient.actions;

import dev.dacbiet.opusenclient.Client;
import dev.dacbiet.opusenclient.packets.DataPacket;
import dev.dacbiet.opusenclient.packets.InitConnectionPacket;
import dev.dacbiet.opusenclient.packets.PacketDeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Post init action.
 * Used to respond to server's message to finalize the connection.
 */
public class PostInitAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(PostInitAction.class);

    private final byte[] data;

    /**
     * Used to respond to server's message to finalize the connection.
     *
     * @param client client
     * @param data response data from first message
     */
    public PostInitAction(Client client, byte[] data) {
        super(client);

        this.data = data;
    }

    @Override
    public boolean exec() {
        if (this.data.length == 0) {
            logger.error("Invalid post init buffer size!");
            throw new ActionException("Invalid post init buffer size!");
        }

        ByteBuffer buf = ByteBuffer.wrap(this.data);
        DataPacket rp = new DataPacket();
        try {
            rp.deserialize(buf);
        } catch (BufferUnderflowException e) {
            logger.error("Invalid size for post init buffer when deserializing!");
            throw new PacketDeserializationException("Unable to deserialize initialize init packet.");
        }

        short demandType = (short) ((buf.get() << 8) + (buf.get()));
        if (rp.getCmd() != -49) {
            logger.error("Invalid complement type received from first init message: {}", demandType);
            throw new ActionException("Invalid complement type.");
        }

        byte msgNum = rp.getMsgNum();
        int connectionId = this.client.getConnectionId();

        // hopefully ATR is constructed beforehand
        byte[] cardATRData = this.client.getATR();

        InitConnectionPacket packet = new InitConnectionPacket(msgNum, connectionId, demandType, Action.TERMINAL_NAME, cardATRData);
        byte[] respInitData = this.client.getSVH().postData(packet.serialize());
        if (respInitData == null || respInitData.length < 5) {
            logger.error("Invalid response post init packet!");
            throw new ActionException("Invalid post init response packet size.");
        }

        DataPacket initRespMsg = new DataPacket();
        initRespMsg.deserialize(ByteBuffer.wrap(respInitData));

        if (initRespMsg.getType() == 16) {
            this.client.addAction(new ApduCommandAction(this.client, initRespMsg));
        } else {
            logger.error("Error: Post init response expected demand type to be 16, but got {}.", initRespMsg.getType());
            throw new ActionException("Invalid post init response expected demand type.");
        }

        return true;
    }
}
