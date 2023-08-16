package socksviahttp.core.net;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.util.Arrays;

public class SVHConnection {
    private static final Logger logger = LoggerFactory.getLogger(SVHConnection.class);
    static final long TIMEOUT = 180;
    static final String INIT_ID = "USER1:PASS1:" + TIMEOUT;
    public static final byte[] ENCRYPT_KEY = "MY_SOCKS_VIA_HTTP_SECRET_ENCRYPTION_KEY".getBytes();

    private final String endpoint;
    private String route;
    private boolean ready;
    private String connId;
    private final String address;
    private final String port;

    public SVHConnection(String endpoint, String address, String port) {
        this.endpoint = endpoint;
        this.route = null;
        this.ready = false;
        this.connId = "";
        this.address = address;
        this.port = port;
    }

    /**
     * Initializes connection (mocks the SocksViaHTTP) to the server.
     *
     * @return true if successfully initialized connection
     */
    public boolean init() {
        // init packet needs to be sent to get a connection id
        DataPacket packet = new DataPacket();
        packet.type = 11;
        packet.tab = (this.address + ":" + this.port).getBytes();
        packet.encryptData = false;
        packet.zipData = false;
        packet.id = INIT_ID;

        byte[] respData = this._postData(packet.saveToByteArray(), true);
        if (respData == null) {
            logger.error("Failed to initialize connect to STLB!");
            return false;
        }

        DataPacket resp = new DataPacket();
        try {
            resp.loadFromByteArray(respData);
        } catch (BufferUnderflowException e) {
            logger.error("Failed to deserialize response!", e);
            return false;
        }

        // Info obtained sets our id
        String info = new String(resp.tab);
        logger.info("Initialized connection to server: " + info);
        if (resp.type != 12) {
            logger.warn("Response packet from server init is {} instead of 12.", resp.type);
        } else {
            logger.info("Connection ID is: {}", resp.id);
            this.connId = resp.id;
        }

        this.ready = true;
        return true;
    }

    public String getConnId() {
        return this.connId;
    }

    public void closeConnection() {
        // no need to inform server if we didn't establish a successful connection
        if (!this.ready) {
            return;
        }

        DataPacket packet = new DataPacket();
        packet.type = 41;
        packet.tab = new byte[0];
        packet.id = this.connId;
        this._postData(packet.saveToByteArray(), false);
    }

    public byte[] postData(byte[] data) {
        DataPacket packet = new DataPacket();
        packet.type = 31;
        packet.tab = data;
        packet.id = this.connId;
        packet.encryptData = true;
        packet.zipData = true;
        packet.encryptionKey = ENCRYPT_KEY;

        byte[] postResp = this._postData(packet.saveToByteArray(), false);
        return this.parseResponse(postResp);
    }

    private byte[] _postData(byte[] data, boolean init) {
        if (!init && !this.ready) {
            logger.info("Connection to server not ready!");
            return null;
        }

        try {
            URL url = new URL(this.endpoint);

            if (this.route == null) {
                HttpsURLConnection cookieCon = (HttpsURLConnection) url.openConnection();

                try {
                    String val = cookieCon.getHeaderField("Set-Cookie");
                    if (val != null && val.contains("ROUTEID")) {
                        this.route = val;
                        logger.info("Servlet cookie found: {}", val);
                    } else {
                        logger.error("Could not get servlet cookie!");
                    }
                } finally {
                    cookieCon.disconnect();
                }
            }


            // now perform the request
//            System.out.println("LL - " + Arrays.toString(data)); // best debugging
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestProperty("Content-Type", "application/octet-stream");
            con.setRequestProperty("Content-Length", String.valueOf(data.length));
            con.setRequestProperty("Cookie", this.route);

            try(OutputStream out = con.getOutputStream()) {
                out.write(data);
                out.flush();

                int respCode = con.getResponseCode();
                if (respCode != 200) {
                    InputStream eis = con.getErrorStream();
                    byte[] eData = IOUtils.toByteArray(eis);

                    String errorMsg = Arrays.toString(eData);
                    try {
                        errorMsg = new String(eData);
                    } catch (Exception ignored) {}

                    logger.error("Connection response code was not 200! ({}): {}", respCode, errorMsg);
                    return null;
                }

                InputStream is = con.getInputStream();
                return IOUtils.toByteArray(is);
            } finally {
                con.disconnect();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        return null;
    }

    private byte[] parseResponse(byte[] respData) {
        DataPacket resp = new DataPacket();
        resp.encryptionKey = ENCRYPT_KEY;
        resp.loadFromByteArray(respData);

        if (resp.errorCode != 0) {
            logger.error("Non-zero error code ({}): CRC Error. Check your secret encryption key.", resp.errorCode);
            return null;
        }

        if (resp.type == 32) {
            return resp.tab;
        }

        // else error
        switch (resp.type) {
            case 2:
                logger.error("Connection not found ({})", this.connId);
                break;
            case 42:
                logger.info("Client asked to close connection...");
                break;
            case 101:
                logger.error("Error: {}", new String(resp.tab));
                break;
            default:
                logger.error("Unexpected response type : {}", resp.type);
        }
        return null;
    }

}
