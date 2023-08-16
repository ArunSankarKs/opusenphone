package dev.dacbiet.opusenclient;

/**
 * Could be anything, just needs to perform ISO/IEC 14443 communication or something.
 */
public interface NfcHandler {

    byte[] send(byte[] data);


}
