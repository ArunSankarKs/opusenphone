package dev.dacbiet.opusenclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.dacbiet.opusenclient.ApduCommandProcessor.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApduCommandProcessorTests {

    private byte[] ossBytes;
    private byte[] cssBytes;

    @BeforeEach
    void setup() {
        this.ossBytes = new byte[] { 0, CLS_OPEN_SECURE_SESSION };
        this.cssBytes = new byte[] { 0, CLS_CLOSE_SECURE_SESSION };
    }

    public static String byteToHex(byte b) {
        return String.format("%02x", b);
    }

    @Test
    @DisplayName("Ensure CLS byte is correct hex")
    void ensureCorrectCLS() {
        // sanity checks
        assertEquals("8a", byteToHex(this.ossBytes[1]));
        assertEquals("8e", byteToHex(this.cssBytes[1]));
        assertEquals("80", byteToHex(P1_IMMEDIATE_RATIFY));

        assertEquals("dc", byteToHex(CLS_UPDATE_RECORD));
        assertEquals("d2", byteToHex(CLS_WRITE_RECORD));

        assertEquals("c0", byteToHex(CLS_GET_RESPONSE));

        assertEquals("69", byteToHex(FAKE_UPDATE_WRITE_RESPONSE[0]));
        assertEquals("85", byteToHex(FAKE_UPDATE_WRITE_RESPONSE[1]));
    }

    @Test
    @DisplayName("Ensure when 2 OSS and 1 CSS, fake response is given for 0xD2")
    void expectFakeResponse0xD2() {
        ApduCommandProcessor p = new ApduCommandProcessor();

        assertArrayEquals(null, p.process(this.ossBytes, new byte[0]));
        assertArrayEquals(null, p.process(this.cssBytes, new byte[0]));
        assertArrayEquals(null, p.process(this.ossBytes, new byte[0]));

        // write cmd
        assertArrayEquals(null, p.process(new byte[] { 0, CLS_WRITE_RECORD }, new byte[0]));

        byte[] getRespCmdResp = p.process(new byte[] { 0, CLS_GET_RESPONSE }, new byte[0]);
        assertArrayEquals(FAKE_UPDATE_WRITE_RESPONSE, getRespCmdResp);
    }

    @Test
    @DisplayName("Ensure when 2 OSS and 1 CSS, fake response is given for 0xDC")
    void expectFakeResponse0xDC() {
        ApduCommandProcessor p = new ApduCommandProcessor();

        assertArrayEquals(null, p.process(this.ossBytes, new byte[0]));
        assertArrayEquals(null, p.process(this.cssBytes, new byte[0]));
        assertArrayEquals(null, p.process(this.ossBytes, new byte[0]));

        // update cmd
        assertArrayEquals(null, p.process(new byte[] { 0, CLS_UPDATE_RECORD }, new byte[0]));

        byte[] getRespCmdResp = p.process(new byte[] { 0, CLS_GET_RESPONSE }, new byte[0]);
        assertArrayEquals(FAKE_UPDATE_WRITE_RESPONSE, getRespCmdResp);
    }

    @Test
    @DisplayName("Ensure when 2 OSS and 2 CSS, fake response is given for Close Secure Session")
    void ensureFakeResponseCloseSS() {
        ApduCommandProcessor p = new ApduCommandProcessor();

        byte[] out = new byte[] { 1, 1, 1 };
        assertArrayEquals(null, p.process(this.ossBytes, new byte[0]));
        assertArrayEquals(null, p.process(this.cssBytes, new byte[0]));
        assertArrayEquals(null, p.process(this.ossBytes, new byte[0]));
        assertArrayEquals(null, p.process(this.cssBytes, out));

        byte[] getRespCmdResp = p.process(new byte[] { 0, CLS_GET_RESPONSE }, new byte[0]);
        assertArrayEquals(out, getRespCmdResp);
    }

    @Test
    @DisplayName("Ensures the command to close secure session also ratifies the session immediately")
    void ensureImmediateRatifyCloseSecureSession() {
        ApduCommandProcessor p = new ApduCommandProcessor();

        byte[] closeCmd = new byte[] { 0, CLS_CLOSE_SECURE_SESSION, 0 };
        p.preProcess(closeCmd);

        assertEquals(P1_IMMEDIATE_RATIFY, closeCmd[2]);
    }

}
