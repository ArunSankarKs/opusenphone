package dev.dacbiet.opusenclient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses the connection information from a string.
 */
public class ConnectionInfo {
    private final String hubEndpoint;
    private final String sessionId;
    private final String cookieInfo;
    private final String servletEndpoint;

    public ConnectionInfo(
            String hubEndpoint,
            String sessionId,
            String cookieInfo,
            String servletEndpoint
    ) {
        this.hubEndpoint = hubEndpoint;
        this.sessionId = sessionId;
        this.cookieInfo = cookieInfo;
        this.servletEndpoint = servletEndpoint;
    }

    public String getHubEndpoint() {
        return this.hubEndpoint;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public String getCookieInfo() {
        return this.cookieInfo;
    }

    public String getServletEndpoint() {
        return this.servletEndpoint;
    }

    public static ConnectionInfo build(String s) {
        String info = s.replace("xscpsmartcard://", "").replace("smartcard://", "");
        String[] args = info.split("&");
        Map<String, String> argMap = new HashMap<>();
        for(String a : args) {
            String[] arg = a.split("=");
            String[] vals = Arrays.copyOfRange(arg, 1, arg.length);
            argMap.put(arg[0], String.join("=", vals));
        }

        return new ConnectionInfo(
                argMap.get("XSCPServer"),
                argMap.get("XSCPSessionId"),
                argMap.get("XSCPCookie"),
                argMap.get("XSCPServlet")
        );
    }
}
