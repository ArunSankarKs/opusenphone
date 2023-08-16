package dev.dacbiet.opusenclient;

import dev.dacbiet.opusenclient.actions.Action;
import dev.dacbiet.opusenclient.actions.ConstructATRAction;
import dev.dacbiet.opusenclient.actions.GetCardIdAction;
import dev.dacbiet.opusenclient.actions.InitAction;
import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.PlatformComponent;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.CookieCredentials;
import microsoft.aspnet.signalr.client.http.HttpConnection;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.transport.WebsocketTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import socksviahttp.core.net.SVHConnection;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Controls all connections to the OPUS en ligne server.
 */
public class Client {

    public static final String XSCP_VERSION = "V01.09";
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    public static final NfcHandler DEFAULT_NFC_HANDLER = (data) -> {
        logger.info("Default NfcHandler: " + Arrays.toString(data));
        return null;
    };

    private final microsoft.aspnet.signalr.client.Logger hubLogger;

    private final ConnectionInfo connectionInfo;
    private HubConnection hubConnection;
    private HubProxy hubProxy;
    private NfcHandler nfcHandler;

    private SVHConnection svh;
    private int connectionId;
    private final AtomicBoolean finished;
    private final ConcurrentLinkedQueue<Action> actions;
    private final CountDownLatch preInitLatch;
    private final CountDownLatch initLatch;
    private String cardId;
    private byte[] atr;
    private final Lock dataHandlerLock;
    private final ApduCommandProcessor apduCommandProcessor;

    public Client(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
        this.svh = null;
        this.connectionId = -1;
        this.finished = new AtomicBoolean(false);
        this.actions = new ConcurrentLinkedQueue<>();
        this.preInitLatch = new CountDownLatch(1);
        this.initLatch = new CountDownLatch(1);
        this.nfcHandler = DEFAULT_NFC_HANDLER;
        this.cardId = "-1";
        this.atr = new byte[0];
        this.dataHandlerLock = new ReentrantLock();
        this.apduCommandProcessor = new ApduCommandProcessor();

        // signalr logger
        this.hubLogger = (s, logLevel) -> {
            switch (logLevel) {
                case Information:
                case Verbose:
                    logger.debug("HUB: {}", s);
                    break;
                case Critical:
                    logger.error("HUB: {}", s);
                    break;
            }
        };

        // mock that we are on windows 10
        logger.debug("Setting platform for Signalr");
        Platform.loadPlatformComponent(new PlatformComponent() {

            @Override
            public HttpConnection createHttpConnection(microsoft.aspnet.signalr.client.Logger logger2) {
                return Platform.createDefaultHttpConnection(logger2);
            }

            @Override
            public String getOSName() {
                logger.debug("Getting OS name from Platform (Signalr)");
                return "windows 10";
            }
        });
    }

    public void setDataHandler(NfcHandler handler) {
        this.dataHandlerLock.lock();
        try {
            this.nfcHandler = handler;
        } finally {
            this.dataHandlerLock.unlock();
        }
    }

    public void removeDataHandler() {
        this.dataHandlerLock.lock();
        try {
            this.nfcHandler = DEFAULT_NFC_HANDLER;
        } finally {
            this.dataHandlerLock.unlock();
        }
    }

    public SVHConnection getSVH() {
        return this.svh;
    }

    public int getConnectionId() {
        return this.connectionId;
    }

    public void addAction(Action action) {
        this.actions.add(action);
    }

    public boolean hasActions() {
        return !this.actions.isEmpty();
    }

    public NfcHandler getNfcHandler() {
        this.dataHandlerLock.lock();
        try {
            return this.nfcHandler;
        } finally {
            this.dataHandlerLock.unlock();
        }
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getCardId() {
        return this.cardId;
    }

    public void setATR(byte[] atr) {
        this.atr = Arrays.copyOf(atr, atr.length);
    }

    public byte[] getATR() {
        return Arrays.copyOf(this.atr, this.atr.length);
    }

    public ApduCommandProcessor getApduCommandProcessor() {
        return this.apduCommandProcessor;
    }

    public void executeAction() {
        try {
            Action action = this.actions.remove();
            logger.info("Executing: {}", action.getClass().getSimpleName());

            if (!action.exec()) {
                logger.info("Issue occurred while executing action!");
                this.actions.add(action);
            }
        } catch (NoSuchElementException e) {
            logger.warn("No actions in the queue!");
        } catch (Exception e) {
            logger.info("Exception occurred while executing action!", e);
            logger.info("Shutting down client...");
            this.shutdown(false);
        }
    }

    public void shutdown(boolean clean) {
        this.finished.set(true);
        this.actions.clear();

        ConnectionState hubState = this.hubConnection.getState();
        if(hubState == ConnectionState.Connected) {
            this.hubProxy.invoke("endtransfer", this.connectionInfo.getSessionId(), clean ? 0 : -1)
                    .done(obj -> logger.info("Invoked endtransfer!"));
        }

        this.svh.closeConnection();
        this.hubConnection.disconnect();
        logger.info("shutdown complete");
    }

    public void shutdown() {
        this.shutdown(true);
    }

    public void awaitPreInit() throws InterruptedException {
        this.preInitLatch.await();
    }

    public void awaitInit() throws InterruptedException {
        this.initLatch.await();
    }

    /**
     * Get the status of the client and whether this client has completed its execution.
     *
     * @return if client is still usable
     */
    public boolean isFinished() {
        return this.finished.get();
    }

    public void start() {
        // must construct ATR
        this.actions.add(new ConstructATRAction(this));

        // do init connection stuff for signalr
        this.hubConnection = new HubConnection(
                this.connectionInfo.getHubEndpoint(),
                this.connectionInfo.getCookieInfo(),
                true,
                this.hubLogger);
        this.hubConnection.setCredentials(new CookieCredentials(this.connectionInfo.getCookieInfo()));

        // setup our hub proxy (idk)
        this.hubProxy = this.hubConnection.createHubProxy("xscphub");
        this.hubProxy.subscribe(new Object() { // what the
            public void messageReceived(final String name, final String message) {
                logger.debug(name + ": " + message);
            }
        });

        this.hubConnection.error(error -> logger.error("Error from hub: {}", error.getMessage()));
        this.hubConnection.connected(() -> logger.info("Connected to hub."));
        this.hubConnection.closed(() -> logger.info("Connection to hub closed."));

        SignalRFuture<Void> startFunc = this.hubConnection.start(new WebsocketTransport(this.hubLogger)).done((o) -> {
            logger.info("Connected to hub.");
            registerEvents();
        });
        this.hubConnection.received((json) -> {
            logger.debug("Raw hub msg: {}", json.toString());
        });

        try {
            startFunc.get();
            logger.info("Complete start.");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
//        } catch (TimeoutException e) {
//            logger.error("Timed out while connecting to hub.");
        }
    }

    /**
     * Register events from the hub proxy and initiates the connection to the hub.
     */
    private void registerEvents() {
        this.hubProxy.on("stopXSCP", () -> {
            logger.info("Server sent stop event.");
            this.shutdown();
        });

        this.hubProxy.on("detectReader", () -> {
            logger.info("Faking reader detection event.");
            this.hubProxy.invoke("enddetectReader", this.connectionInfo.getSessionId(), "1")
                    .done((o) -> logger.info("Invoked enddetectReader."));
        });

        this.hubProxy.on("detectCard", () -> {
            logger.info("Faking card detection event.");
            this.hubProxy.invoke("enddetectCard", this.connectionInfo.getSessionId(), 1) // 1 card
                    .done((o) -> logger.info("Invoked enddetectCard."));
        });

        this.hubProxy.on("activatechannel", (String opId, String address, String port) -> {
            if (opId.isEmpty() || opId.equals("0") || address.isEmpty() || port.isEmpty() || port.equals("0")) {
                this.hubProxy.invoke("endtransfer", this.connectionInfo.getSessionId(), "-2")
                        .done((o) -> logger.info("Invalid data given, sent end event"));
                this.shutdown(false);
            }

            this.connectionId = Integer.parseInt(opId);
            try {
                logger.info("Start SVH connection with args: {} {} {}", opId, address, port);
                this.svh = new SVHConnection(this.connectionInfo.getServletEndpoint(), address, port);
                if (!this.svh.init()) {
                    logger.info("Unable to initialize (mock) socket connection to server!");
                    this.shutdown(false);
                    return;
                }

                logger.info("Queuing first action...");
                this.addAction(new InitAction(this));

                // count down twice in case get card id was never called
                this.preInitLatch.countDown();
                this.initLatch.countDown();
            } catch (Exception e) {
                logger.error("e: ", e);
            }

        }, String.class, String.class, String.class);

        this.hubProxy.on("GetCardSerialNumber", () -> {
            logger.info("Found GetCardSerialNumber event!");

            String cid = this.cardId;
            logger.info("Supplying ({}) as card id.", cid);
            this.preInitLatch.countDown();

            this.hubProxy.invoke("endGetCardSerialNumber", this.connectionInfo.getSessionId(), cid)
                    .done((o) -> logger.info("Invoked endGetCardSerialNumber."));
        });

        this.hubProxy.on("GetCardSerialNumberWithID", (String cmd) -> {
            logger.info("Found GetCardSerialNumberWithID event!");

            this.addAction(new GetCardIdAction(this, (cid) -> {
                logger.info("Retrieved card id is {}.", cid);

                this.hubProxy.invoke("endGetCardSerialNumberWithID", this.connectionInfo.getSessionId(), cid)
                        .done((o) -> logger.info("Invoked endGetCardSerialNumberWithID."));
            }));
            this.preInitLatch.countDown();
        }, String.class);

        // less used ones?
        this.hubProxy.on("DetectXSCP", () -> {
            logger.info("DetectXSCP event found.");
            this.hubProxy.invoke("endDetectXSCP", this.connectionInfo.getSessionId())
                    .done((o) -> logger.info("Invoked endDetectXSCP."));
        });

        this.hubProxy.on("GetReaderName", (String readerNumber) -> {
            logger.info("GetReaderName event found.");
            this.hubProxy.invoke("endGetReaderName", this.connectionInfo.getSessionId(), Action.TERMINAL_NAME)
                    .done((o) -> logger.info("Invoked endGetReaderName."));
        }, String.class);

        this.hubProxy.on("GetVersion", () -> {
            logger.info("GetVersion event found.");
            this.hubProxy.invoke("endGetVersion", this.connectionInfo.getSessionId(), Client.XSCP_VERSION)
                    .done((o) -> logger.info("Invoked endGetVersion."));
        });


        this.hubProxy.invoke("joinRoom", this.connectionInfo.getSessionId(), "java")
                .done((obj) -> logger.info("Joined room with session id: " + this.connectionInfo.getSessionId()));

        this.hubProxy.invoke("endLaunchXSCP", this.connectionInfo.getSessionId(), "0", XSCP_VERSION)
                .done((obj) -> logger.info("Invoked endLaunchXSCP."));



        // other events that are ignored
        // TODO : Implement other events?
        this.hubProxy.on("SendApduxml", (String APDU) -> {
            logger.info("Send Apduxml event ignored.");
        }, String.class);
        this.hubProxy.on("GetStatus", (String server, String port) -> {
            logger.info("GetStatus event ignored.");
        }, String.class, String.class);
    }

}
