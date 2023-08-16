package dev.dacbiet.opusenclient.packets;

public class PacketDeserializationException extends PacketException {

    public PacketDeserializationException(String message) {
        super(message);
    }

    public PacketDeserializationException(Throwable cause) {
        super(cause);
    }
}
