package de.ehealth.evek.mobile.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.ehealth.evek.mobile.util.Log;
public class ServerConnection implements Runnable, IsInitializedListener {
    private Socket server;
    private Thread networkThread;
    private String serverAddress = "localhost";
    private int serverPort = 12013;
    private int timesToTryConnect = 20;
    private int timesTriedToConnect = 0;
    private int msToWaitWhileConnecting = 5000;
    private int msToWaitForReconnect = 500;
    private boolean isInitialized;
    private final List<IsInitializedListener> isInitializedListeners = new ArrayList<>();

    public void initConnection() throws IllegalStateException{
        if(isInitialized)
             throw new IllegalStateException("Connection already initialized!");
        Log.sendMessage("Starting up Network Thread...");
        networkThread = new Thread(this);
        networkThread.setName("ServerConnection-" + serverAddress);
        networkThread.start();

    }
    public void setServerAddress (String serverAddress){
        this.serverAddress = serverAddress;
    }
    public void setServerPort (int serverPort){
        this.serverPort = serverPort;
    }
    public boolean isInitialized(){
        return isInitialized;
    }

    @Override
    public void run() {
        Log.sendMessage("Trying to initialize server connection...");

        while(timesTriedToConnect < timesToTryConnect){

            try (Socket socket = new Socket()) {
                SocketAddress endpoint = new InetSocketAddress(serverAddress, serverPort);
                socket.connect(endpoint, msToWaitWhileConnecting);
                setInitialized(true);
                Log.sendMessage("Server connection has been successfully initialized!");
                this.server = socket;
                break;
            //} catch(SocketTimeoutException e){
            } catch (IOException e) {
                timesTriedToConnect++;
                setInitialized(false);
                if(timesTriedToConnect < timesToTryConnect - 1){
                    try {
                        //noinspection BusyWait
                        Thread.sleep(msToWaitForReconnect);
                    } catch (InterruptedException ex) {
                        Log.sendException(ex);
                    }
                    continue;
                }
                Log.sendMessage(String.format(Locale.getDefault(),"Server connection failed to initialize %d times!", timesToTryConnect));
                Log.sendException(e);
            }
        }
    }

    public void addIsInitializedListener(IsInitializedListener listener){
        isInitializedListeners.add(listener);
    }
    public void removeIsInitializedListener(IsInitializedListener listener){
        isInitializedListeners.remove(listener);
    }
    private void setInitialized(boolean isInitialized){
        this.isInitialized = isInitialized;
        for(IsInitializedListener listener : isInitializedListeners){
            listener.onValueChanged(isInitialized);
        }
    }
    @Override
    public void onValueChanged(boolean isInitialized) {
        if(isInitialized)
            Log.sendMessage("Network Thread has been successfully started up!");
        else{
            Log.sendMessage("Network Thread could not be started up!");
        }
    }

    public int getTimesTriedToConnect(){
        return timesTriedToConnect +1;
    }
    public int getTimesToTryConnect(){
        return timesToTryConnect;
    }
}
