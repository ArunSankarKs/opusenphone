package dev.dacbiet.opusenclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Processes apdu commands.
 * Possibly modifies apdu command responses if necessary.
 */
public class ApduCommandProcessor {

    public static final byte CLS_GET_RESPONSE = -64; // 0xC0
    public static final byte CLS_UPDATE_RECORD = -36; // 0xDC
    public static final byte CLS_WRITE_RECORD = -46; // 0xD2
    public static final byte CLS_CLOSE_SECURE_SESSION = -114; // 0x8E
    public static final byte CLS_OPEN_SECURE_SESSION = -118; // 0x8A
    public static final byte P1_IMMEDIATE_RATIFY = -128; // 0x80
    public static final byte[] FAKE_UPDATE_WRITE_RESPONSE = new byte[] { 105, -123 }; // 69 85

    private final List<ApduCommand> history;
    private boolean isSecureSessionActive;
    private int openSecureSessionCount;
    private int closeSecureSessionCount;

    public ApduCommandProcessor() {
        this.history = new ArrayList<>();
        this.isSecureSessionActive = false;
        this.openSecureSessionCount = 0;
        this.closeSecureSessionCount = 0;
    }

    public boolean getIsSecureSessionActive() {
        return this.isSecureSessionActive;
    }

    /**
     * Processes the command for the card and its response to the command.
     * If non-null array is returned, it should be used as response to server.
     *
     * @param command apdu command bytes
     * @param response response bytes
     * @return null if response is fine for server
     */
    public byte[] process(byte[] command, byte[] response) {
        this.history.add(new ApduCommand(command, response));

        if(command.length > 1) {
            if(command[1] == CLS_OPEN_SECURE_SESSION) {
                this.openSecureSessionCount++;
                this.isSecureSessionActive = true;
            } else if(command[1] == CLS_CLOSE_SECURE_SESSION) {
                this.closeSecureSessionCount++;
                this.isSecureSessionActive = false;
            }

            if(this.openSecureSessionCount == 2 && this.closeSecureSessionCount == 1 && command[1] == CLS_GET_RESPONSE && this.history.size() > 1) {
                // For after write/update, and it asks for Get Response

                ApduCommand lastCmd = this.history.get(this.history.size() - 2);
                // last was 0xD2 or 0xDC and this is Get Response
                if(lastCmd.command[1] == CLS_WRITE_RECORD || lastCmd.command[1] == CLS_UPDATE_RECORD) {
                    return Arrays.copyOf(FAKE_UPDATE_WRITE_RESPONSE, FAKE_UPDATE_WRITE_RESPONSE.length);
                }

            } else if(this.openSecureSessionCount == 2 && this.closeSecureSessionCount == 2 && command[1] == CLS_GET_RESPONSE && this.history.size() > 1) {
                // For after Close Secure Session, and it asks for Get Response
                ApduCommand lastCmd = this.history.get(this.history.size() - 2);
                if(lastCmd.command[1] == CLS_CLOSE_SECURE_SESSION) {
                    return Arrays.copyOf(lastCmd.response, lastCmd.response.length);
                }

            }
        }

        return null;
    }

    /**
     * Checks the command being given to the card and see if we need to modify the parameters.
     * Used for ensuring the secure connection is immediately ratified after closing.
     *
     * @param command apdu command bytes
     */
    public void preProcess(byte[] command) {
        if(command.length > 1 && command[1] == CLS_CLOSE_SECURE_SESSION && command[2] == 0) { // 0x8e
            command[2] = P1_IMMEDIATE_RATIFY;
        }
    }

    public List<ApduCommand> getHistory() {
        return new ArrayList<>(this.history);
    }


    public static class ApduCommand {
        private final byte[] command;
        private final byte[] response;

        public ApduCommand(byte[] command, byte[] response) {
            this.command = command;
            this.response = response;
        }
    }
}
