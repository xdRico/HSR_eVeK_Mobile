package de.ehealth.evek.mobile.core;


import de.ehealth.evek.mobile.network.ServerConnection;
import de.ehealth.evek.mobile.util.Debug;
import de.ehealth.evek.mobile.util.Log;

public class ClientMain {
    private static final int SERVER_PORT = 12013;
    private static final String SERVER_ADDRESS = "192.168.1.6";
    private final ServerConnection serverConnection;
    public ClientMain(){
        Log.initLogging();
        Log.setWriteToFile(false);
        Debug.setDebugMode(true);
        Log.sendMessage("Debugging activated!");
        serverConnection = new ServerConnection();
        serverConnection.setServerAddress(SERVER_ADDRESS);
        serverConnection.setServerPort(SERVER_PORT);
        if(!serverConnection.initConnection())
            Log.sendMessage("Failed to init Server Connection!");
    }

}
