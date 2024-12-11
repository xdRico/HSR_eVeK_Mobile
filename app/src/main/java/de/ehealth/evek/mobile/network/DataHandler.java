package de.ehealth.evek.mobile.network;

import java.util.ArrayList;
import java.util.List;

import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.entity.User;
import de.ehealth.evek.api.exception.WrongCredentialsException;
import de.ehealth.evek.api.network.IComClientReceiver;
import de.ehealth.evek.api.network.IComClientSender;
import de.ehealth.evek.api.type.Id;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.exception.NoValidUserRoleException;
import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;

public class DataHandler implements IsLoggedInListener, IsInitializedListener{
    private static DataHandler instance;
    public static DataHandler instance(){
        return instance == null ? (instance = new DataHandler()) : instance;
    }

    private User loginUser;

    private static final int SERVER_PORT = 12013;
    private static final String SERVER_ADDRESS = "192.168.1.6";
    private IComClientReceiver receiver;
    private IComClientSender sender;
    private final ServerConnection serverConnection = new ServerConnection();
    private final List<IsLoggedInListener> isLoggedInListeners = new ArrayList<>();

    public DataHandler(){
        addIsLoggedInListener(this);
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }
    public void initServerConnection(){
        serverConnection.setServerAddress(SERVER_ADDRESS);
        serverConnection.setServerPort(SERVER_PORT);
        serverConnection.addIsInitializedListener(this);
        serverConnection.initConnection();
    }

    /**
     * @param isInitialized
     */
    @Override
    public void onInitializedStateChanged(boolean isInitialized) {
        if(!isInitialized)
            return;
        sender = serverConnection.getComClientSender();
        receiver = serverConnection.getComClientReceiver();
    }

    public TransportDetails tryAssignTransport(String input) throws IllegalArgumentException {
        if(!input.matches("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}"))
            throw new IllegalArgumentException("String format does not match!");
        try {
            sender.sendTransportDetails(new TransportDetails.AssignTransportProvider(new Id<>(input), loginUser.serviceProvider()));
            return receiver.receiveTransportDetails();
        } catch (Exception e) {
            Log.sendException(e);
            throw new IllegalArgumentException(e);
        }
    }

    public void addIsLoggedInListener(IsLoggedInListener listener){
        if(!isLoggedInListeners.contains(listener))
            isLoggedInListeners.add(listener);
    }
    public void removeIsLoggedInListener(IsLoggedInListener listener){
        if(!isLoggedInListeners.contains(listener))
            isLoggedInListeners.remove(listener);
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

    /**
     * @param isLoggedIn
     */
    @Override
    public void onLoginStateChanged(Throwable isLoggedIn) {
        if(!(isLoggedIn instanceof UserLoggedInThrowable))
            return;
        loginUser = ((UserLoggedInThrowable) isLoggedIn).getUser();
    }
}
