package de.ehealth.evek.mobile.network;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.api.entity.User;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.exception.WrongCredentialsException;
import de.ehealth.evek.api.network.IComClientReceiver;
import de.ehealth.evek.api.network.IComClientSender;
import de.ehealth.evek.api.type.Id;
import de.ehealth.evek.api.type.PatientCondition;
import de.ehealth.evek.api.type.TransportReason;
import de.ehealth.evek.api.type.TransportationType;
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
    //private static final String SERVER_ADDRESS = "192.168.1.9";
    private static final String SERVER_ADDRESS = "192.168.1.6";
    //private static final String SERVER_ADDRESS = "192.168.56.1";
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

    public User getLoginUser(){
        return loginUser;
    }

    public void tryLogin(String username, String password){
        new Thread (() -> {
            Throwable t = new WrongCredentialsException();
            try {
                this.sender.loginUser(username, password);
                User loginUser = this.receiver.receiveUser();
                if(loginUser != null) {
                    t = switch (loginUser.role()) {
                        case HealthcareAdmin, HealthcareUser, InsuranceAdmin,
                             InsuranceUser, TransportAdmin, TransportInvoice ->
                                new NoValidUserRoleException(loginUser.role(), "Mobile (App) Login");
                        case HealthcareDoctor, TransportDoctor, TransportUser, SuperUser -> {
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
            }else
                Log.sendMessage("User " + username + " successfully logged in!");
            for(IsLoggedInListener listener : isLoggedInListeners)
                listener.onLoginStateChanged(t);
        }).start();

    }

    @Override
    public void onLoginStateChanged(Throwable isLoggedIn) {
        if (!(isLoggedIn instanceof UserLoggedInThrowable))
            return;
        loginUser = ((UserLoggedInThrowable) isLoggedIn).getUser();
    }

    public TransportDocument createTransportDocument(TransportDocument.Create cmd) throws ProcessingException {
        try{
            sender.sendTransportDocument(cmd);
            return receiver.receiveTransportDocument();
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    public static Date getDate(String input) throws IllegalProcessException {
        String[] possibleFormats = {"dd.MM.yy", "dd.MM.yyyy", "yyyy-MM-dd", "MM/dd/yyyy"};
        Date date = null;

        for (String format : possibleFormats) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.GERMANY);
                date = new Date(dateFormat.parse(input).getTime());
                System.out.println("Detected Format: " + format);
                break; // Format gefunden, Schleife beenden
            } catch (Exception ignored) {
                // Format passt nicht, nächstes ausprobieren
            }
        }

        if (date != null)
            return date;
        throw new IllegalProcessException(new IllegalArgumentException("No valid date format!"));
    }

    public static String getTransportationTypeString(TransportationType type){
        return switch(type) {
            case Taxi -> "Taxi/Mietwagen";
            case KTW -> "KTW, da medizinisch-fachliche Betreuung und/oder Einrichtung notwendig ist (Begründung unter 4. erforderlich)";
            case RTW -> "RTW";
            case NAWorNEF -> "NAW/NEF";
            case Other -> "andere (Spezifizierung unter 4. erforderlich)";
        };
    }

    public static String getPatientConditionString(PatientCondition condition){
        return switch(condition) {
            case CarryingChair -> "Tragestuhl";
            case WheelChair -> "Rollstuhl";
            case LyingDown -> "liegend";
        };
    }

    public static String getTransportReasonString(TransportReason reason){
        return switch(reason) {
            case EmergencyTransport -> "a1) Notfalltransport";
            case FullPartStationary -> "a2) voll-/teilstationäre Krankenhausbehandlung";
            case PrePostStationary -> "a3) vor-/nachstationäre Behandlung";
            case AmbulantTaxi -> "b) ambulante Behandlung (nur Taxi/Mietwagen! - bei Merkzeichen \"aG\", \"Bl\" oder \"H\", Pflegegrad 3 mit dauerhafter Mobilitätsbeeinträchtigung, Pflegegrad 4 oder 5)";
            case OtherPermitFree -> "c) anderer Grund (Genehmigungsfrei, z.B. Fahrten zu Hospizen)";
            case HighFrequent -> "d1) hochfrequente Behandlung (Dialyse, onkol. Chemo- oder Strahlentherapie)";
            case HighFrequentAlike -> "d2) vergleichbarer Ausnahmefall (wie d1, Begründung unter 4. erforderlich)";
            case ContinuousImpairment -> "e) dauerhafte Mobilitätsbeeinträchtigung vergleichbar mit b und Behandlungsdauer mindestens 6 Monate (Begründung unter 4. erforderlich)";
            case OtherKTW -> "f) anderer Grund für Fahrt mit KTW (z.B. fachgerechtes Lagern, Tragen, Heben erforderlich, Begründung unter 3. und ggf. 4. erforderlich)";
        };
    }
}
