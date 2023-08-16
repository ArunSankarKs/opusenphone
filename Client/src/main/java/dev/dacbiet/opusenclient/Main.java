package dev.dacbiet.opusenclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

public class Main {

    static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
//        args = new String[] {
//          ""
//        };

        if(args.length < 1) {
            logger.error("Requires URL for connection (starts with 'smartcard://...')");
            return;
        }

        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();

        if(terminals.size() < 1) {
            System.out.println("No terminals found.");
            return;
        }

        System.out.println(terminals.size() + " terminals found.");
        CardTerminal term = terminals.get(0);
        System.out.println("Terminal name: " + term.getName());

        if (!term.isCardPresent()) {
            System.out.println("No card present.");
            return;
        }

        Card card = term.connect("*");
        CardChannel channel = card.getBasicChannel();

        NfcHandler nfcHandler = (data) -> {
            try {
                ResponseAPDU rspCarte = channel.transmit(new CommandAPDU(data));
                return rspCarte.getBytes();
            } catch (CardException e) {
                e.printStackTrace();
            }
            return null;
        };

        ConnectionInfo connectionInfo = ConnectionInfo.build(args[0]);
        Client client = new Client(connectionInfo);
        client.setDataHandler(nfcHandler);
        client.start();


        client.awaitPreInit();
        logger.info("Begin execution of init actions...");
        while (!client.isFinished() && client.hasActions()) {
            if (client.hasActions()) {
                client.executeAction();
            }
            // sleep
        }


        client.awaitInit();
        logger.info("Begin execution of actions...");
        while (!client.isFinished()) {
            if (client.hasActions()) {
                client.executeAction();
            }
            // sleep
        }
    }

}
