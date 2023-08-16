package dev.dacbiet.opusenclient.actions;

import dev.dacbiet.opusenclient.ApduCommandProcessor;
import dev.dacbiet.opusenclient.packets.*;
import dev.dacbiet.opusenclient.Client;
import dev.dacbiet.opusenclient.NfcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Performs communication with the card using NFC (or anything to interface with it).
 * Originally each command was designed to be separate so that the NFC contact could be
 * disconnected, but resume communication after reconnecting.
 * However, this is not likely not possible due to secure sessions that can occur or select
 * operations during communication.
 */
public class ApduCommandAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ApduCommandAction.class);

    private List<Apdu> commandsToRun;
    private List<Apdu> apduResponses;
    private DataPacket initPacket;
    private int connectionId;
    private byte connectionState;

    public ApduCommandAction(Client client, DataPacket initPacket) {
        super(client);

        this.initPacket = initPacket;
        this.commandsToRun = null;
        this.apduResponses = new ArrayList<>();
        this.connectionState = -1;
        this.connectionId = -1;
        this.init();
    }

    private void init() {
        ByteBuffer buf = ByteBuffer.wrap(this.initPacket.getData());

        ApduDataPacket packet = new ApduDataPacket();
        try {
            packet.deserialize(buf);
        } catch (BufferUnderflowException e) {
            throw new PacketDeserializationException(e);
        }

        this.commandsToRun = packet.getCommands();
        this.connectionId = packet.getConnectionId();
        this.connectionState = packet.getConnectionState();
    }

    @Override
    public boolean exec() {
        if (this.initPacket.getCmd() != -17) {
            logger.info("Invalid command specified in init packet response: ({}).", this.initPacket.getCmd());
            throw new ActionException("Invalid command specified in init packet response.");
        }

        if ((this.connectionState & 4) != 0) {
            logger.info("Connection is no longer active! ({})", this.connectionState);
            this.client.shutdown();
            return true;
        }

        NfcHandler nfc = this.client.getNfcHandler();
        ApduCommandProcessor processor = this.client.getApduCommandProcessor();
        while (!this.commandsToRun.isEmpty()) {
            Apdu apdu = this.commandsToRun.get(0);
            byte[] apduCmd = apdu.getData();
            processor.preProcess(apduCmd);
            byte[] resp = nfc.send(apduCmd);

            logger.debug("NFC Sent: " + bytesToHex(apduCmd));
            logger.debug("NFC Received: " + bytesToHex(resp));

            if (resp == null) {
                logger.error("Invalid response from Nfc handler.");
                throw new ActionException("Invalid response from Nfc handler.");
            }

            byte[] pResp = processor.process(apdu.getData(), resp);
            if(pResp != null) {
                resp = pResp;
                logger.info("Faking response from handler!");
            }
            apduResponses.add(new Apdu(resp));
            this.commandsToRun.remove(0);
        }


        // now send back the responses to the server
        ApduResponseDataPacket packet = new ApduResponseDataPacket(
                (this.connectionState & 4) == 0,
                this.connectionId,
                this.initPacket.getMsgNum(),
                this.apduResponses);
        packet.deserialize(ByteBuffer.wrap(this.initPacket.getData()));
        byte[] respInitData = this.client.getSVH().postData(packet.serialize());
        if (respInitData == null || respInitData.length < 5) {
            logger.error("Invalid response packet!");
            throw new ActionException("Invalid response packet!");
        }

        DataPacket initRespMsg = new DataPacket();
        try {
            initRespMsg.deserialize(ByteBuffer.wrap(respInitData));
        } catch (BufferUnderflowException e) {
            throw new PacketDeserializationException(e);
        }

        if ((this.connectionState & 4) != 0) {
            logger.info("Connection no longer active!");
            this.client.shutdown();
            return true;
        }

        if (initRespMsg.getType() == 16) {
            // more actions to do
            this.client.addAction(new ApduCommandAction(this.client, initRespMsg));
        } else if (initRespMsg.getType() == 48) {
            // we must respond to server to complete initialization
            this.client.addAction(new PostInitAction(this.client, respInitData));
        } else {
            logger.error("Unexpected response demand type: {}", initRespMsg.getType());
            throw new ActionException("Unexpected response demand in response.");
        }

        return true;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if(bytes == null) {
            return "";
        }

        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }
}
