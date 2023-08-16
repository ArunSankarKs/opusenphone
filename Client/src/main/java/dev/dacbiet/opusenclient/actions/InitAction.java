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
 * First action to connect to server.
 */
public class InitAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(InitAction.class);

    private final byte msgNum;
    private final int connectionId;
    private final byte type;

    /**
     * Used for first contact with the server.
     *
     * @param client client
     */
    public InitAction(Client client) {
        super(client);
        this.connectionId = client.getConnectionId();
        this.msgNum = 0;
        this.type = -1;
    }

    @Override
    public boolean exec() {
        byte[] cardATRData = this.client.getATR();

        InitConnectionPacket packet = new InitConnectionPacket(this.msgNum, this.connectionId, this.type, Action.TERMINAL_NAME, cardATRData);
        byte[] respInitData = this.client.getSVH().postData(packet.serialize());
        if (respInitData == null || respInitData.length < 5) {
            logger.error("Invalid response init packet!");
            throw new ActionException("Invalid response init packet!");
        }

//        logger.info(Arrays.toString(respInitData));

        DataPacket initRespMsg = new DataPacket();
        try {
            initRespMsg.deserialize(ByteBuffer.wrap(respInitData));
        } catch (BufferUnderflowException e) {
            logger.error("Error occurred while deserializing response. ", e);
            throw new PacketDeserializationException("Unable to deserialize init response packet.");
        }

        if (initRespMsg.getType() == 48) {
            // we must respond to server to complete initialization
            this.client.addAction(new PostInitAction(this.client, respInitData));
        } else {
            logger.error("Error: Init response expected demand type to be 48, but got {}.", initRespMsg.getType());
            throw new ActionException("Invalid init response expected demand type");
        }

        return true;
    }
}
