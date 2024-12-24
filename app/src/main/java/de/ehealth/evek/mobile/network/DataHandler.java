package de.ehealth.evek.mobile.network;

import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

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
import de.ehealth.evek.api.exception.UserNotProvidedException;
import de.ehealth.evek.api.exception.WrongCredentialsException;
import de.ehealth.evek.api.network.IComClientReceiver;
import de.ehealth.evek.api.network.IComClientSender;
import de.ehealth.evek.api.type.Id;
import de.ehealth.evek.api.type.PatientCondition;
import de.ehealth.evek.api.type.TransportReason;
import de.ehealth.evek.api.type.TransportationType;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.core.ClientMain;
import de.ehealth.evek.mobile.exception.NoValidUserRoleException;
import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;
import de.ehealth.evek.mobile.exception.UserLogoutThrowable;

public class DataHandler implements IsLoggedInListener, IsInitializedListener{
    private static DataHandler instance;
    public static DataHandler instance(){
        return instance == null ? (instance = new DataHandler()) : instance;
    }


    private static final int SERVER_PORT = 12013;
    //private static final String SERVER_ADDRESS = "192.168.1.9";
    //private static final String SERVER_ADDRESS = "192.168.1.6";
    //private static final String SERVER_ADDRESS = "192.168.56.1";
    private static final String SERVER_ADDRESS = "149.172.224.72";

    private User loginUser;
    private IComClientReceiver receiver;
    private IComClientSender sender;
    private SharedPreferences encryptedSharedPreferences;

    private boolean storeNextUser = false;
    private boolean validUserStoring = false;

    private final ServerConnection serverConnection = new ServerConnection();

    private final List<IsLoggedInListener> isLoggedInListeners = new ArrayList<>();

    private final List<TransportDocument> transportDocuments = new ArrayList<>();
    private final List<TransportDetails> transportDetails = new ArrayList<>();
    //TODO

    public ServerConnection getServerConnection() {
        return serverConnection;
    }
    public void initServerConnection(){
        serverConnection.setServerAddress(SERVER_ADDRESS);
        serverConnection.setServerPort(SERVER_PORT);
        serverConnection.addIsInitializedListener(this);
        serverConnection.initConnection();
    }

    public void initUserStorage(){
        MasterKey masterKey = null;
        SharedPreferences encryptedSharedPreferences = null;
        validUserStoring = false;
        try{
            addIsLoggedInListener(this);
            masterKey = new MasterKey.Builder(ClientMain.instance().getContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            // EncryptedSharedPreferences erstellen
            encryptedSharedPreferences = EncryptedSharedPreferences.create(
                    ClientMain.instance().getContext(),
                    "SecurePrefs", // Name der SharedPreferences
                    masterKey, // Der gleiche MasterKey wie beim Speichern
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        }catch(Exception e){

            Log.sendException(e);
        }
        if(masterKey != null && encryptedSharedPreferences != null)
            validUserStoring = true;
        this.encryptedSharedPreferences = encryptedSharedPreferences;
        if (validUserStoring)
            Log.sendMessage("UserStorage successfully set up!");
    }

    @Override
    public void onInitializedStateChanged(boolean isInitialized) {
        if(!isInitialized)
            return;
        sender = serverConnection.getComClientSender();
        receiver = serverConnection.getComClientReceiver();
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

    public List<TransportDocument> getTransportDocuments(){
        return transportDocuments;
    }

    public List<TransportDetails> getTransportDetails(){
        return transportDetails;
    }

    private void addTransportDocument(TransportDocument document) {
        TransportDocument toDelete = null;
        for(TransportDocument doc : transportDocuments){
            if(doc == document) return;
            if(doc.id().value().equals(document.id().value())) {
                toDelete = doc;
                break;
            }
        }
        if(toDelete != null)
            transportDocuments.remove(toDelete);
        transportDocuments.add(document);
    }

    private void addTransport(TransportDetails details) {
        TransportDetails toDelete = null;
        for(TransportDetails det : transportDetails){
            if(det == details) return;
            if(det.id().value().equals(details.id().value())) {
                toDelete = det;
                break;
            }
        }
        if(toDelete != null)
            transportDetails.remove(toDelete);
        transportDetails.add(details);
    }

    public void storeNextUser(boolean toStore){
        this.storeNextUser = toStore;
    }

    public boolean tryLoginFromStoredCredentials(){
        if(!validUserStoring)
            return false;
            // Passwort abrufen
        String savedPassword = encryptedSharedPreferences.getString("eVeKpassword", null);
        String savedUser = encryptedSharedPreferences.getString("eVeKusername", null);

        if (savedPassword != null && savedUser != null
                && !savedPassword.isBlank() && !savedUser.isBlank()) {
            storeNextUser(true);
            tryLogin(savedUser, savedPassword);
            return true;
        }
        Log.sendMessage("No Password or Username stored!");
        return false;
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
                if(validUserStoring){
                    SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
                    editor.putString("eVeKpassword", null);
                    editor.putString("eVeKusername", null);
                    editor.apply();
                }
                Log.sendMessage("User " + username + " could not be logged in!");
                Log.sendException(t);
            }else{
                Log.sendMessage("User " + username + " successfully logged in!");
                if(storeNextUser){
                    if(validUserStoring){
                        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
                        editor.putString("eVeKpassword", password);
                        editor.putString("eVeKusername", username);
                        editor.apply();
                        Log.sendMessage("User " + username + " successfully saved for auto login!");
                    }else{
                        Log.sendMessage("User " + username + " could not be saved for auto login!");
                    }
                    storeNextUser = false;


                }
            }

            for(IsLoggedInListener listener : isLoggedInListeners)
                listener.onLoginStateChanged(t);
        }).start();
    }

    public void logout() throws IllegalProcessException{
        if(loginUser == null)
            throw new IllegalProcessException(new UserNotProvidedException("No user logged in!"));
        loginUser = null;
        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
        editor.putString("eVeKpassword", null);
        editor.putString("eVeKusername", null);
        editor.apply();
        try{
            serverConnection.resetConnection();
        }catch(IllegalProcessException e){
            Log.sendException(e);
        }
        for(IsLoggedInListener listener : isLoggedInListeners)
            listener.onLoginStateChanged(new UserLogoutThrowable());
    }

    public TransportDetails tryAssignTransport(String input) throws IllegalProcessException {
        if(!input.matches("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}"))
            throw new IllegalArgumentException("String format does not match!");
        try {
            sender.sendTransportDetails(new TransportDetails.AssignTransportProvider(new Id<>(input), loginUser.serviceProvider()));
            TransportDetails assigned = receiver.receiveTransportDetails();
            addTransport(assigned);
            return assigned;
        } catch (Exception e) {
            Log.sendException(e);
            throw new IllegalProcessException(e);
        }
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
            TransportDocument created = receiver.receiveTransportDocument();
            addTransportDocument(created);
            return created;
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    public TransportDetails createTransport(TransportDetails.Create cmd) throws ProcessingException {
        try{
            sender.sendTransportDetails(cmd);
            TransportDetails created = receiver.receiveTransportDetails();
            addTransport(created);
            return created;
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
                java.util.Date d = dateFormat.parse(input);
                if(d == null)
                    throw new NullPointerException("Date not provided!");
                date = new Date(d.getTime());
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
