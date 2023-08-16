package dev.dacbiet.opusenclient.actions;

import dev.dacbiet.opusenclient.Client;
import dev.dacbiet.opusenclient.NfcHandler;
import dev.dacbiet.opusenclient.packets.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Constructs and sets the ATR data for the current client.
 * Will also obtain the card id.
 */
public class ConstructATRAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(ConstructATRAction.class);

    static final byte[] SELECT_CMD = new byte[] { 0, -92, 4, 0, 8, 49, 84, 73, 67, 46, 73, 67, 65 };
    static final byte[] GET_RESPONSE_CMD = new byte[] { 0, -64, 0, 0, 0 };
    static final byte[] BASE_ATR = new byte[] {
            59, 111, 0, 0, -128, 90,    // no idea (3B 6F 00 00 80 5A)
            0, 0, 0, 0, 0, 0,           // some data idk
            0, 0, 0, 0,                 // card id bytes
            -126,                       // idk (0x82)
            -112, 0                     // success (0x90)
    };

    public ConstructATRAction(Client client) {
        super(client);
    }

    @Override
    public boolean exec() throws ActionException {

        NfcHandler nfc = this.client.getNfcHandler();

        byte[] selectResp = nfc.send(SELECT_CMD);
        byte[] data = selectResp;

        if (selectResp == null) {
            return false;
        } else if (selectResp.length == 2 && selectResp[0] == -112) {
            // sometimes it might just respond with OK, sooo
            data = nfc.send(GET_RESPONSE_CMD);
            if (data == null) {
                return false;
            }
        }

        if (data[data.length - 2] != -112 || data[data.length - 1] != 0) {
            throw new ActionException("Unexpected response from card.");
        }

        // it's fine if any of these are out of range
        byte[] cardIdData = new byte[4];
        // card id starts at 23
        System.arraycopy(data, 23, cardIdData, 0, cardIdData.length);

        byte[] cardInfoData = new byte[6];
        // info needed starts at 23
        System.arraycopy(data, 30, cardInfoData, 0, cardInfoData.length);


        String cardId = String.valueOf(Packet.bytesToLong(cardIdData));
        logger.info("Retrieved card id is ({}).", cardId);

        byte[] ourAtr = Arrays.copyOf(BASE_ATR, BASE_ATR.length);
        System.arraycopy(cardIdData, 0, ourAtr, 12, cardIdData.length);
        System.arraycopy(cardInfoData, 0, ourAtr, 6, cardInfoData.length);

        this.client.setCardId(cardId);
        this.client.setATR(ourAtr);

        return true;
    }

}
