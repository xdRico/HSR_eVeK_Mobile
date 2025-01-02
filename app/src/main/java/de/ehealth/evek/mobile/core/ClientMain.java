package de.ehealth.evek.mobile.core;

import android.content.Context;

import de.ehealth.evek.api.util.Debug;
import de.ehealth.evek.api.util.Log;

import de.ehealth.evek.mobile.network.DataHandler;

/**
 * Class used for handling backend and network communication
 */
public class ClientMain {
    private static ClientMain clientMain = null;
    private Context context = null;

    /**
     * Constructor for ClientMain<br>
     * Handling backend and network communication
     */
    private ClientMain(){
        Thread.currentThread().setName("eVeK-MainThread");
        clientMain = this;
        Log.initLogging();
        Log.setWriteToFile(false);
        Debug.setDebugMode(true);
        Log.sendMessage("Debugging activated!");
        DataHandler.instance();
    }

    /**
     * Method to get the current ClientMain or else create a new one
     *
     * @param context - Context to set for the current / new ClientMain
     *
     * @return ClientMain - the current ClientMain
     */
    public static ClientMain instance(Context context){
        ClientMain main = new ClientMain();
        main.setContext(context);
        return clientMain == null ? main : clientMain;
    }

    /**
     * Method to get the current ClientMain or else create a new one
     *
     * @return ClientMain - the current ClientMain
     */
    public static ClientMain instance(){
        return clientMain == null ? new ClientMain() : clientMain;
    }

    /**
     * Method to get the Context given to the ClientMain
     *
     * @return Context - the assigned Context
     *
     * @throws IllegalStateException - when no context is given
     */
    public Context getContext() throws IllegalStateException {
        if(context == null)
            throw new IllegalStateException("Context is null!");
        return context;
    }

    /**
     * Method to set the Context for the ClientMain
     *
     * @param context - the Context to set for the ClientMain
     */
    private void setContext(Context context){
        this.context = context;
        DataHandler.instance().initUserStorage();
    }
}
