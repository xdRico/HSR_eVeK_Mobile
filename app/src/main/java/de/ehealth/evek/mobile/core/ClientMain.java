package de.ehealth.evek.mobile.core;

import android.content.Context;

import de.ehealth.evek.api.util.Debug;
import de.ehealth.evek.api.util.Log;

import de.ehealth.evek.mobile.network.DataHandler;

public class ClientMain {
    private static ClientMain clientMain = null;
    private Context context = null;
    private ClientMain(){
        clientMain = this;
        Log.initLogging();
        Log.setWriteToFile(false);
        Debug.setDebugMode(true);
        Log.sendMessage("Debugging activated!");
        DataHandler.instance();
    }
    public static ClientMain instance(Context context){
        ClientMain main = new ClientMain();
        main.setContext(context);
        return clientMain == null ? main : clientMain;
    }

    public static ClientMain instance(){
        return clientMain == null ? new ClientMain() : clientMain;
    }

    public Context getContext() throws IllegalStateException {
        if(context == null)
            throw new IllegalStateException("Context is null!");
        return context;
    }

    private void setContext(Context context){
        this.context = context;
        DataHandler.instance().initUserStorage();
    }
}
