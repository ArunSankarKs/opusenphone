package dev.dacbiet.test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import dev.dacbiet.opusenclient.Client;
import dev.dacbiet.opusenclient.ConnectionInfo;
import dev.dacbiet.opusenclient.NfcHandler;

public class ClientManager {

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private Client client;
    private final Lock clientLock = new ReentrantLock(true);

    public void createClient(String url) {
        clientLock.lock();

        try {
            ConnectionInfo connectionInfo = ConnectionInfo.build(url);
            this.client = new Client(connectionInfo);
            this.isRunning.set(false);
        } finally {
            clientLock.unlock();
        }
    }

    public void startClient(NfcHandler nfcHandler) {
        clientLock.lock();
        if(client == null) {
            return;
        }

        try {
            this.client.setDataHandler(nfcHandler);
            this.isRunning.set(true);
        } finally {
            clientLock.unlock();
        }
    }

    public Client getClient() {
        clientLock.lock();

        try {
            return this.client;
        } finally {
            clientLock.unlock();
        }
    }

    public boolean isRunning() {
        return this.isRunning.get();
    }

    public void reset() {
        clientLock.lock();

        try {
            this.client = null;
            this.isRunning.set(false);
        } finally {
            clientLock.unlock();
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
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
