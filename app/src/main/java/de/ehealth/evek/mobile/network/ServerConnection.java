package de.ehealth.evek.mobile.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.ehealth.evek.api.exception.EncryptionException;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.network.IComClientReceiver;
import de.ehealth.evek.api.network.IComClientSender;
import de.ehealth.evek.api.network.ComClientReceiver;
import de.ehealth.evek.api.network.ComClientSender;

import de.ehealth.evek.api.util.Log;

public class ServerConnection implements IsInitializedListener {
    private int serverPort = 12013;
    private int timesToTryConnect = 15;
    private int timesTriedToConnect = 0;
    private int msToWaitWhileConnecting = 9500;
    private int msToWaitForReconnect = 500;
    private boolean isInitialized;
    private String serverAddress = "localhost";

    private IComClientReceiver receiver;
    private IComClientSender sender;
    private Socket server;
    private final List<IsInitializedListener> isInitializedListeners = new ArrayList<>();

    IComClientSender getComClientSender(){ return sender; }
    IComClientReceiver getComClientReceiver() { return receiver; }
    void setServerAddress (String serverAddress){
        this.serverAddress = serverAddress;
    }
    void setServerPort (int serverPort){
        this.serverPort = serverPort;
    }

    public int getTimesTriedToConnect(){ return timesTriedToConnect +1; }
    public int getTimesToTryConnect(){ return timesToTryConnect; }
    boolean isInitialized(){
        return isInitialized;
    }

    public void addIsInitializedListener(IsInitializedListener listener){
        if(!isInitializedListeners.contains(listener))
            isInitializedListeners.add(listener);
    }
    public void removeIsInitializedListener(IsInitializedListener listener){
        isInitializedListeners.remove(listener);
    }

    private void setInitialized(boolean isInitialized){
        this.isInitialized = isInitialized;
        for(IsInitializedListener listener : isInitializedListeners){
            try {
                listener.onInitializedStateChanged(isInitialized);
            }catch(NullPointerException e){
                Log.sendException(e);
            }
        }
    }

    @Override
    public void onInitializedStateChanged(boolean isInitialized) {
        Log.sendMessage(isInitialized
                ? "Network Thread has been successfully started up!"
                : "Network Thread could not be started up!");
    }

    void initConnection() throws IllegalStateException {
        if (isInitialized)
            throw new IllegalStateException("Connection already initialized!");
        Log.sendMessage("Starting up Network Thread...");
        new Thread(() -> {

            Log.sendMessage("Trying to initialize server connection...");
            addIsInitializedListener(this);
            while (timesTriedToConnect < timesToTryConnect) {

                try {
                    server = new Socket();
                    SocketAddress endpoint = new InetSocketAddress(serverAddress, serverPort);
                    server.connect(endpoint, msToWaitWhileConnecting);
                    sender = new ComClientSender(server);
                    receiver = new ComClientReceiver(server);
                    sender.useEncryption(receiver);
                    setInitialized(true);
                    Log.sendMessage("Server connection has been successfully initialized!");
                    break;
                } catch (EncryptionException e) {
                    Log.sendException(e);
                    Log.sendMessage("Connection not encrypted!");
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    try {
                        server.close();
                    } catch (IOException ex) {
                        Log.sendMessage("Socket could not be closed!");
                    }
                    if (timesTriedToConnect >= timesToTryConnect - 1) {
                        setInitialized(false);
                        Log.sendMessage(String.format(Locale.getDefault(), "Server connection failed to initialize %d times!", timesToTryConnect));
                        Log.sendException(e);
                        return;
                    }
                    try {
                        timesTriedToConnect++;
                        setInitialized(false);
                        //noinspection BusyWait
                        Thread.sleep(msToWaitForReconnect);
                    } catch (InterruptedException ex) {
                        Log.sendException(ex);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    void resetConnection() throws IllegalProcessException {
        if(!isInitialized)
            throw new IllegalProcessException("Connection already initialized!");
        try {
            server.close();
            setInitialized(false);
            isInitializedListeners.clear();
        }catch(IOException e){
            Log.sendException(e);
            throw new IllegalProcessException(e);
        }
    }
}
