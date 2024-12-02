package de.ehealth.evek.mobile.network;

import java.net.InetAddress;
import java.net.Socket;

import de.ehealth.evek.mobile.util.Log;
public class ServerConnection implements Runnable {
    private Socket server;
    private Thread networkThread;
    private String serverAddress = "localhost";
    private int serverPort = 12013;
    private boolean isInitialized = false;

    public boolean initConnection() throws IllegalStateException{
        if(isInitialized)
             throw new IllegalStateException("Connection already initialized!");
        Log.sendMessage("Starting up Network Thread...");
        networkThread = new Thread(this);
        networkThread.setName("ServerConnection-" + serverAddress);
        networkThread.start();
        try{
            int i = 0;
            while(!isInitialized && i < 500){
                Thread.sleep(10);
                i++;
            }
            Log.sendMessage("Network Thread has been successfully started up!");
            return true;

        }catch(InterruptedException e){
            Log.sendException(e);
        }
        Log.sendMessage("Network Thread could not be started up!");
        return false;
    }
    public void setServerAddress (String serverAddress){
        this.serverAddress = serverAddress;
    }
    public void setServerPort (int serverPort){
        this.serverPort = serverPort;
    }
    @Override
    public void run() {
        Log.sendMessage("Trying to initialize server connection...");
        try{
            server = new Socket(InetAddress.getByName(serverAddress), serverPort);
            isInitialized = true;
            Log.sendMessage("Server connection has been successfully initialized!");
        }catch(Exception e){
            Log.sendMessage("Server connection could not be initialized!");
            Log.sendException(e);
        }
    }
}
