package dev.dacbiet.opusenclient.packets;

public class PacketException extends RuntimeException {

    public PacketException(String message) {
        super(message);
    }

    public PacketException(Throwable cause) {
        super(cause);
    }

}
