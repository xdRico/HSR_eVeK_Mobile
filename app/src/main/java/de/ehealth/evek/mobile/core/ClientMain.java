package de.ehealth.evek.mobile.core;


import de.ehealth.evek.mobile.network.ServerConnection;
import de.ehealth.evek.mobile.util.Debug;
import de.ehealth.evek.mobile.util.Log;

public class ClientMain {
    private static final int SERVER_PORT = 12013;
    private static final String SERVER_ADDRESS = "192.168.1.6";
    private static ClientMain clientMain = null;
    private final ServerConnection serverConnection;
    private ClientMain(){
        clientMain = this;
        Log.initLogging();
        Log.setWriteToFile(false);
        Debug.setDebugMode(true);
        Log.sendMessage("Debugging activated!");
        serverConnection = new ServerConnection();
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }
    public void initServerConnection(){
        serverConnection.setServerAddress(SERVER_ADDRESS);
        serverConnection.setServerPort(SERVER_PORT);
        serverConnection.initConnection();
    }

    public static ClientMain instance(){
        return clientMain == null ? new ClientMain() : clientMain;
    }

}
