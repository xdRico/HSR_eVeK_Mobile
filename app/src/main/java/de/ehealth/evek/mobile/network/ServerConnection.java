package de.ehealth.evek.mobile.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.ehealth.evek.api.entity.User;
import de.ehealth.evek.api.exception.EncryptionException;
import de.ehealth.evek.api.exception.WrongCredentialsException;
import de.ehealth.evek.api.network.IComClientReceiver;
import de.ehealth.evek.api.network.IComClientSender;
import de.ehealth.evek.api.network.ComClientReceiver;
import de.ehealth.evek.api.network.ComClientSender;
import de.ehealth.evek.api.network.ComEncryptionKey;
import de.ehealth.evek.mobile.exception.NoValidUserRoleException;
import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;

import de.ehealth.evek.api.util.Log;

public class ServerConnection implements Runnable, IsInitializedListener {
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
    private Thread networkThread;
    private User loginUser;
    private final List<IsInitializedListener> isInitializedListeners = new ArrayList<>();
    private final List<IsLoggedInListener> isLoggedInListeners = new ArrayList<>();

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
        addIsInitializedListener(this);
        while(timesTriedToConnect < timesToTryConnect){

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
                } catch(IOException ex){
                    Log.sendMessage("Socket could not be closed!");
                }
                if(timesTriedToConnect < timesToTryConnect - 1){
                    try {
                        timesTriedToConnect++;
                        setInitialized(false);
                        //noinspection BusyWait
                        Thread.sleep(msToWaitForReconnect);
                    } catch (InterruptedException ex) {
                        Log.sendException(ex);
                    }
                    continue;
                }
                setInitialized(false);
                Log.sendMessage(String.format(Locale.getDefault(),"Server connection failed to initialize %d times!", timesToTryConnect));
                Log.sendException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addIsInitializedListener(IsInitializedListener listener){
        if(!isInitializedListeners.contains(listener))
            isInitializedListeners.add(listener);
    }
    public void removeIsInitializedListener(IsInitializedListener listener){
        if(!isInitializedListeners.contains(listener))
            isInitializedListeners.remove(listener);
    }
    public void addIsLoggedInListener(IsLoggedInListener listener){
        if(!isLoggedInListeners.contains(listener))
            isLoggedInListeners.add(listener);
    }
    public void removeIsLoggedInListener(IsLoggedInListener listener){
        if(!isLoggedInListeners.contains(listener))
            isLoggedInListeners.remove(listener);
    }

    private void setInitialized(boolean isInitialized){
        this.isInitialized = isInitialized;
        for(IsInitializedListener listener : isInitializedListeners){
            listener.onInitializedStateChanged(isInitialized);
        }
    }
    @Override
    public void onInitializedStateChanged(boolean isInitialized) {
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

    public void tryLogin(String username, String password){
        new Thread (() -> {
            Throwable t = new WrongCredentialsException();
            try {
                this.sender.loginUser(username, password);
                User loginUser = this.receiver.receiveUser();
                if(loginUser != null) {
                    t = switch (loginUser.role()) {
                        case HealthcareAdmin, HealthcareDoctor, HealthcareUser, InsuranceAdmin,
                             InsuranceUser ->
                                new NoValidUserRoleException(loginUser.role(), "Mobile (App) Login");
                        case TransportAdmin, TransportDoctor, TransportInvoice, TransportUser,
                             SuperUser -> {
                            this.loginUser = loginUser;
                            yield new UserLoggedInThrowable(loginUser);
                        }
                    };
                }
            } catch(Exception e){
                t = e;
            }

            if(!(t instanceof UserLoggedInThrowable)) {
                this.loginUser = null;
                Log.sendMessage("User " + username + " could not be logged in!");
                Log.sendException(t);
            }
            for(IsLoggedInListener listener : isLoggedInListeners)
                listener.onLoginStateChanged(t);
        }).start();

    }
}
