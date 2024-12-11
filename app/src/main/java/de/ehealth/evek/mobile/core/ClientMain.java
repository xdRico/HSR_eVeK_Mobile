package de.ehealth.evek.mobile.core;

import de.ehealth.evek.api.util.Debug;
import de.ehealth.evek.api.util.Log;

import de.ehealth.evek.mobile.network.DataHandler;

public class ClientMain {
    private static ClientMain clientMain = null;
    private ClientMain(){
        clientMain = this;
        Log.initLogging();
        Log.setWriteToFile(false);
        Debug.setDebugMode(true);
        Log.sendMessage("Debugging activated!");
        DataHandler.instance();
    }
    public static ClientMain instance(){
        return clientMain == null ? new ClientMain() : clientMain;
    }

}
