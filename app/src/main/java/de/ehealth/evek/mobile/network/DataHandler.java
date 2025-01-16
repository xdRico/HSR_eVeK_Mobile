package de.ehealth.evek.mobile.network;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.net.SocketException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.ehealth.evek.api.entity.Address;
import de.ehealth.evek.api.entity.InsuranceData;
import de.ehealth.evek.api.entity.Patient;
import de.ehealth.evek.api.entity.ServiceProvider;
import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.api.entity.User;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.exception.UserNotProvidedException;
import de.ehealth.evek.api.exception.WrongCredentialsException;
import de.ehealth.evek.api.network.IComClientReceiver;
import de.ehealth.evek.api.network.IComClientSender;
import de.ehealth.evek.api.type.Direction;
import de.ehealth.evek.api.type.Id;
import de.ehealth.evek.api.type.PatientCondition;
import de.ehealth.evek.api.type.Reference;
import de.ehealth.evek.api.type.TransportReason;
import de.ehealth.evek.api.type.TransportationType;
import de.ehealth.evek.api.util.COptional;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.core.ClientMain;
import de.ehealth.evek.mobile.exception.NoValidUserRoleException;
import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;
import de.ehealth.evek.mobile.exception.UserLogoutThrowable;

/**
 * Class DataHandler used for handling Communication and Data handling between Backend, Network and Frontend
 *
 * @implements {@link IsLoggedInListener}
 * @implements {@link IsInitializedListener}
 */
public class DataHandler implements IsLoggedInListener, IsInitializedListener{
    private static DataHandler instance;

    /**
     * Method for getting the current {@link DataHandler} instance or else creating a new one
     *
     * @return {@link DataHandler} the current {@link DataHandler}
     */
    public static DataHandler instance(){
        return instance == null ? (instance = new DataHandler()) : instance;
    }

    private static final int SERVER_PORT = PrivateInfo.SERVER_PORT;
    private static final String SERVER_ADDRESS = PrivateInfo.SERVER_ADDRESS;

    private Thread networkThread;
    private Handler networkHandler;

    private User loginUser;
    private IComClientReceiver receiver;
    private IComClientSender sender;
    private SharedPreferences encryptedSharedPreferences;

    private boolean storeNextUser = false;
    private boolean validStoring = false;

    private final ServerConnection serverConnection = new ServerConnection();

    private final List<IsLoggedInListener> isLoggedInListeners = new ArrayList<>();
    private final List<TransportDocumentsChangedListener> transportDocumentsChangedListeners = new ArrayList<>();
    private final List<TransportsChangedListener> transportsChangedListeners = new ArrayList<>();
    private final List<Id<TransportDocument>> transportDocumentIDs = new ArrayList<>();
    private final List<Id<TransportDetails>> transportDetailIDs = new ArrayList<>();

    /**
     * Record used for passing the ConnectionCounter.
     *
     * @param timesToTryConnect maximum times for trying to connect
     * @param timesTriedToConnect current count of tries
     */
    public record ConnectionCounter(int timesToTryConnect, int timesTriedToConnect) {
    }

    /**
     * Interface used for passing changes in the {@link TransportDocument TransportDocuments} to added Listeners.
     */
    public interface TransportDocumentsChangedListener {

        /**
         * Method called on changes in the {@link TransportDocument TransportDocuments}.
         */
        void onTransportDocumentsChanged();
    }

    /**
     * Interface used for passing changes in the {@link TransportDetails Transports} to added Listeners.
     */
    public interface TransportsChangedListener {

        /**
         * Method called on changes in the {@link TransportDetails Transports}.
         */
        void onTransportsChanged();
    }

    /**
     * Method used for calling all {@link TransportDocumentsChangedListener TransportDocumentsChanged Listener's} onTransportDocumentsChanged method.
     */
    private void callOnTransportDocumentsChanged(){
        for(TransportDocumentsChangedListener listener : transportDocumentsChangedListeners)
            if(listener != null)
                listener.onTransportDocumentsChanged();
    }

    /**
     * Method used for calling all {@link TransportsChangedListener TransportsChanged Listener's} onTransportsChanged method.
     */
    private void callOnTransportsChanged(){
        for(TransportsChangedListener listener : transportsChangedListeners){
            if(listener != null)
                listener.onTransportsChanged();
        }
    }

    /**
     * Method for adding {@link IsInitializedListener IsInitializedListeners}
     *
     * @param listener {@link IsInitializedListener} to be added
     */
    public void addIsInitializedListener(IsInitializedListener listener){
        serverConnection.addIsInitializedListener(listener);
    }

    /**
     * Method for adding {@link IsLoggedInListener IsLoggedInListeners}
     *
     * @param listener {@link IsLoggedInListener} to be added
     */
    public void addIsLoggedInListener(IsLoggedInListener listener){
        if(!isLoggedInListeners.contains(listener))
            isLoggedInListeners.add(listener);
    }

    /**
     * Method for adding {@link TransportDocumentsChangedListener TransportDocumentsChangedListeners}
     *
     * @param listener {@link TransportDocumentsChangedListener} to be added
     */
    public void addTransportDocumentsChangedListener(TransportDocumentsChangedListener listener){
        if(!transportDocumentsChangedListeners.contains(listener))
            transportDocumentsChangedListeners.add(listener);
    }

    /**
     * Method for adding {@link TransportsChangedListener TransportsChangedListeners}
     *
     * @param listener {@link TransportsChangedListener} to be added
     */
    public void addTransportsChangedListener(TransportsChangedListener listener){
        if(!transportsChangedListeners.contains(listener))
            transportsChangedListeners.add(listener);
    }

    /**
     * Method for removing {@link IsLoggedInListener IsLoggedInListeners}
     *
     * @param listener {@link IsLoggedInListener} to be removed
     */
    public void removeIsLoggedInListener(IsLoggedInListener listener){
        isLoggedInListeners.remove(listener);
    }

    @Override
    public void onLoginStateChanged(Throwable isLoggedIn) {
        if (!(isLoggedIn instanceof UserLoggedInThrowable))
            return;
        loginUser = ((UserLoggedInThrowable) isLoggedIn).getUser();
    }

    @Override
    public void onInitializedStateChanged(boolean isInitialized) {
        if(!isInitialized)
            return;
        sender = serverConnection.getComClientSender();
        receiver = serverConnection.getComClientReceiver();
    }

    /**
     * Method for getting the current {@link ConnectionCounter}
     *
     * @return {@link ConnectionCounter} - the current {@link ConnectionCounter}
     */
    public ConnectionCounter getConnectionCounter() {
        return new ConnectionCounter(serverConnection.getTimesToTryConnect(), serverConnection.getTimesTriedToConnect());
    }

    /**
     * Method for initializing the Server Connection <br>
     * Creating a new looping Thread accessible with runOnNetworkThread.
     */
    public void initServerConnection(){
        networkThread = new Thread(() -> {
            Looper.prepare();
            networkHandler = new Handler();

            serverConnection.setServerAddress(SERVER_ADDRESS);
            serverConnection.setServerPort(SERVER_PORT);
            serverConnection.addIsInitializedListener(this);
            serverConnection.initConnection();

            // Looper als letzten Aufruf im Thread starten!!
            Looper.loop();
        });
        networkThread.setName("eVeK-NetworkThread");
        networkThread.start();
    }

    /**
     * Method to run a {@link Runnable} on the Network {@link Thread}
     *
     * @param action the {@link Runnable} to run on the Network {@link Thread}
     */
    public final void runOnNetworkThread(Runnable action) {
        if (Thread.currentThread() != networkThread) {
            if(networkHandler.post(action))
                return;
            Log.sendMessage("Runnable could not be added to network Thread!");
            Log.sendMessage("   Run on additional Thread...");
            new Thread(action).start();
        } else {
            action.run();
        }
    }

    /**
     * Method for initializing storing {@link User users} for staying logged in
     */
    public void initStorage(){
        MasterKey masterKey = null;
        SharedPreferences encryptedSharedPreferences = null;
        validStoring = false;
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
            validStoring = true;
        this.encryptedSharedPreferences = encryptedSharedPreferences;
        if (validStoring)
            Log.sendMessage("UserStorage successfully set up!");
    }

    /**
     * Method to get the currently logged in {@link User}
     *
     * @return {@link User} - the currently logged in {@link User}
     */
    public User getLoginUser(){
        return loginUser;
    }

    /**
     * Method to be called when the login state changes <br>
     * Notifying all {@link IsLoggedInListener IsLoggedInListeners}
     *
     * @param loginThrowable throwable representing the login state (generally {@link UserLoggedInThrowable} or {@link UserLogoutThrowable})
     */
    private void changeLoginState(Throwable loginThrowable){
        List<IsLoggedInListener> listeners = new ArrayList<>(this.isLoggedInListeners);
        for(IsLoggedInListener listener : listeners)
            listener.onLoginStateChanged(loginThrowable);
    }

    /**
     * Method to get a {@link TransportDocument} by its {@link Id}
     *
     * @param transportDocID the {@link Id} of the {@link TransportDocument} as String
     *
     * @return {@link TransportDocument} - the {@link TransportDocument} with the given {@link Id}
     *
     * @throws ProcessingException thrown, when the {@link TransportDocument} could not be found
     */
    public TransportDocument getTransportDocumentById(String transportDocID) throws ProcessingException{
        return getTransportDocumentById(new Id<>(transportDocID));
    }

    /**
     * Method to get a {@link TransportDocument} by its {@link Id}
     *
     * @param transportDocID the {@link Id} of the {@link TransportDocument}
     *
     * @return {@link TransportDocument} - the {@link TransportDocument} with the given {@link Id}
     *
     * @throws ProcessingException thrown, when the {@link TransportDocument} could not be found
     */
    public TransportDocument getTransportDocumentById(Id<TransportDocument> transportDocID) throws ProcessingException{
        try {
            sender.sendTransportDocument(new TransportDocument.Get(transportDocID));
            return receiver.receiveTransportDocument();
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to create a new {@link TransportDocument} with the given parameters
     *
     * @param patient the {@link Patient} that the {@link TransportDocument} should be assigned to
     * @param insuranceData the {@link InsuranceData} to use for the {@link TransportDocument}
     * @param transportReason the reason of the {@link TransportDocument}
     * @param startDate the start {@link Date} of the {@link TransportDocument}
     * @param endDate the end {@link Date} of the {@link TransportDocument}
     * @param weeklyFrequency how many transports have to be made each week
     * @param healthcareServiceProvider the {@link ServiceProvider} where the {@link Patient} is treated
     * @param transportationType the type of the transportation (vehicle)
     * @param additionalInfo additional information to be made for the {@link TransportDocument}
     *
     * @return {@link TransportDocument} - the created {@link TransportDocument}
     *
     * @throws ProcessingException thrown, when the {@link TransportDocument} could not be created
     */
    public TransportDocument createTransportDocument(COptional<Reference<Patient>>patient,
                                                     COptional<Reference<InsuranceData>> insuranceData,
                                                     TransportReason transportReason,
                                                     Date startDate,
                                                     COptional<Date> endDate,
                                                     COptional<Integer> weeklyFrequency,
                                                     Reference<ServiceProvider> healthcareServiceProvider,
                                                     TransportationType transportationType,
                                                     COptional<String> additionalInfo) throws ProcessingException {
        try{
            TransportDocument.Create cmd = new TransportDocument.Create(patient, insuranceData, transportReason, startDate, endDate,
                    weeklyFrequency, healthcareServiceProvider,transportationType, additionalInfo, Reference.to(loginUser.id().value()));
            sender.sendTransportDocument(cmd);
            TransportDocument created = receiver.receiveTransportDocument();
            addTransportDocument(created);
            return created;
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to update the information of a {@link TransportDocument} with the given parameters
     *
     * @param transportDocumentId the {@link Id} of the {@link TransportDocument} to be edited
     * @param transportReason the reason of the {@link TransportDocument}
     * @param startDate the start {@link Date} of the {@link TransportDocument}
     * @param endDate the end {@link Date} of the {@link TransportDocument}
     * @param weeklyFrequency how many transports have to be made each week
     * @param healthcareServiceProvider the {@link ServiceProvider} where the {@link Patient} is treated
     * @param transportationType the type of the transportation (vehicle)
     * @param additionalInfo additional information to be made for the {@link TransportDocument}
     *
     * @return {@link TransportDocument} - the updated {@link TransportDocument}
     *
     * @throws ProcessingException thrown, when the {@link TransportDocument} could not be created
     */
    public TransportDocument updateTransportDocument(Id<TransportDocument> transportDocumentId,
                                                     TransportReason transportReason,
                                                     Date startDate,
                                                     COptional<Date> endDate,
                                                     COptional<Integer> weeklyFrequency,
                                                     Reference<ServiceProvider> healthcareServiceProvider,
                                                     TransportationType transportationType,
                                                     COptional<String> additionalInfo) throws ProcessingException {
        try{
            TransportDocument.Update cmd = new TransportDocument.Update(transportDocumentId, transportReason, startDate, endDate,
                    weeklyFrequency, healthcareServiceProvider,transportationType, additionalInfo, Reference.to(loginUser.id().value()));
            sender.sendTransportDocument(cmd);
            TransportDocument updated = receiver.receiveTransportDocument();
            addTransportDocument(updated);
            return updated;
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to update the information and {@link Patient} of a {@link TransportDocument} with the given parameters
     *
     * @param transportDocumentId the {@link Id} of the {@link TransportDocument} to be edited
     * @param patient the {@link Patient} that the {@link TransportDocument} should be assigned to
     * @param insuranceData the {@link InsuranceData} to use for the {@link TransportDocument}
     * @param transportReason the reason of the {@link TransportDocument}
     * @param startDate the start {@link Date} of the {@link TransportDocument}
     * @param endDate the end {@link Date} of the {@link TransportDocument}
     * @param weeklyFrequency how many transports have to be made each week
     * @param healthcareServiceProvider the {@link ServiceProvider} where the {@link Patient} is treated
     * @param transportationType the type of the transportation (vehicle)
     * @param additionalInfo additional information to be made for the {@link TransportDocument}
     *
     * @return {@link TransportDocument} - the updated {@link TransportDocument}
     *
     * @throws ProcessingException thrown, when the {@link TransportDocument} could not be created
     */
    public TransportDocument updateTransportDocumentWithPatient(Id<TransportDocument> transportDocumentId,
                                                                Reference<Patient>patient,
                                                                Reference<InsuranceData> insuranceData,
                                                                TransportReason transportReason,
                                                                Date startDate,
                                                                COptional<Date> endDate,
                                                                COptional<Integer> weeklyFrequency,
                                                                Reference<ServiceProvider> healthcareServiceProvider,
                                                                TransportationType transportationType,
                                                                COptional<String> additionalInfo) throws ProcessingException {
        try{
            updateTransportDocument(transportDocumentId, transportReason, startDate,
                    endDate, weeklyFrequency, healthcareServiceProvider, transportationType, additionalInfo);
            TransportDocument updated;
            updated = assignTransportDocumentPatient(transportDocumentId, patient, insuranceData);
            addTransportDocument(updated);
            return updated;
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to assign a {@link Patient} to a {@link TransportDocument}
     *
     * @param transportDocumentId the {@link Id} of the {@link TransportDocument} to be updated
     * @param patient the {@link Patient} to be assigned
     * @param insuranceData the {@link InsuranceData} to be assigned
     *
     * @return {@link TransportDocument} - the updated {@link TransportDocument}
     *
     * @throws ProcessingException thrown, when the {@link TransportDocument} could not be updated
     */
    public TransportDocument assignTransportDocumentPatient(Id<TransportDocument> transportDocumentId,
                                                            Reference<Patient>patient,
                                                            Reference<InsuranceData> insuranceData) throws ProcessingException{
        try{
            TransportDocument.AssignPatient cmd = new TransportDocument.AssignPatient(transportDocumentId, patient, insuranceData);
            sender.sendTransportDocument(cmd);
            TransportDocument updated = receiver.receiveTransportDocument();
            addTransportDocument(updated);
            return updated;
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to get the {@link TransportDocument TransportDocuments} of the session as {@link List}
     *
     * @return {@link List<TransportDocument>} - the {@link List} of current {@link TransportDocument TransportDocuments}
     */
    public List<TransportDocument> getTransportDocuments(){
        List<TransportDocument> docs = new ArrayList<>();
        try{
            if(transportDocumentIDs.isEmpty())
                return docs;
            sender.sendTransportDocument(new TransportDocument.GetListByIDList(transportDocumentIDs));
            docs = (List<TransportDocument>) receiver.receiveList();
        } catch(Exception e){
            Log.sendMessage("Could not read TransportDocuments!");
        }
        return docs;
    }

    /**
     * Method to add a {@link TransportDocument} to the current sessions {@link TransportDocument TransportDocuments}
     *
     * @param transportDocument the {@link TransportDocument} to be added
     */
    private void addTransportDocument(TransportDocument transportDocument) {
        for(Id<TransportDocument> doc : transportDocumentIDs){
            if(doc.value().equals(transportDocument.id().value())) {
                return;
            }
        }
        transportDocumentIDs.add(transportDocument.id());

        callOnTransportDocumentsChanged();

        if(!validStoring)
            return;

        List<String> stringIDs = new ArrayList<>();
        for(Id<TransportDocument> documents : transportDocumentIDs)
            stringIDs.add(documents.value());

        Set<String> transportDocuments = Set.copyOf(stringIDs);

        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
        editor.putStringSet("eVeK-transportdocuments", transportDocuments);
        editor.apply();
    }

    /**
     * Method to load the recent {@link TransportDocument TransportDocuments} of the {@link User}
     */
    private void loadTransportDocuments(){
        if(!validStoring)
            return;
        SharedPreferences editor = encryptedSharedPreferences;
        Set<String> ids = editor.getStringSet("eVeK-transportdocuments", null);

        if(ids == null)
            return;

        for(String id : ids)
            transportDocumentIDs.add(new Id<>(id));
    }

    /**
     * Method to get a {@link TransportDetails Transport} by its {@link Id}
     *
     * @param transportID the {@link Id} of the {@link TransportDetails Transport} as String
     *
     * @return {@link TransportDetails TransportDetails} - the {@link TransportDetails Transport} with the given {@link Id}
     *
     * @throws ProcessingException thrown, when the {@link TransportDetails Transport} could not be found
     */
    public TransportDetails getTransportDetailsById(String transportID) throws ProcessingException{
        return getTransportDetailsById(new Id<>(transportID));
    }

    /**
     * Method to get a {@link TransportDetails Transport} by its {@link Id}
     *
     * @param transportID the {@link Id} of the {@link TransportDetails Transport}
     *
     * @return {@link TransportDetails TransportDetails} - the {@link TransportDetails Transport} with the given {@link Id}
     *
     * @throws ProcessingException thrown, when the {@link TransportDetails Transport} could not be found
     */
    public TransportDetails getTransportDetailsById(Id<TransportDetails> transportID) throws ProcessingException{
        try {
            sender.sendTransportDetails(new TransportDetails.Get(transportID));
            return receiver.receiveTransportDetails();
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to create a new {@link TransportDetails Transport} with the given parameters
     *
     * @param transportDocumentId the {@link TransportDocument} the {@link TransportDetails Transport} should be assigned to
     * @param date the {@link Date} of the {@link TransportDetails Transport}
     *
     * @return {@link TransportDetails TransportDetails} - the created {@link TransportDetails Transport}
     *
     * @throws ProcessingException thrown, when the {@link TransportDetails Transport} could not be created
     */
    public TransportDetails createTransport(Reference<TransportDocument> transportDocumentId, Date date) throws ProcessingException {
        try{
            sender.sendTransportDetails(new TransportDetails.Create(transportDocumentId, date));
            TransportDetails created = receiver.receiveTransportDetails();
            addTransport(created);
            return created;
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to get the {@link TransportDetails Transports} of the session as {@link List}
     *
     * @return {@link List<TransportDetails>} - the {@link List} of current {@link TransportDetails Transports}
     */
    public List<TransportDetails> getTransportDetails(){
        List<TransportDetails> det = new ArrayList<>();
        try{
            if(transportDocumentIDs.isEmpty())
                return det;
            sender.sendTransportDetails(new TransportDetails.GetListByIDList(transportDetailIDs));
            det = (List<TransportDetails>) receiver.receiveList();
        } catch(Exception e){
            Log.sendMessage("Could not read TransportDetails!");
        }
        return det;
    }

    /**
     * Method to add a {@link TransportDetails Transport} to the current sessions {@link TransportDetails Transports}
     *
     * @param transportDetails the {@link TransportDetails Transport} to be added
     */
    private void addTransport(TransportDetails transportDetails) {
        for(Id<TransportDetails> detail : transportDetailIDs){
            if(detail.value().equals(transportDetails.id().value())) {
                return;
            }
        }
        transportDetailIDs.add(transportDetails.id());

        callOnTransportsChanged();

        if(!validStoring)
            return;

        List<String> stringIDs = new ArrayList<>();
        for(Id<TransportDetails> detail : transportDetailIDs)
            stringIDs.add(detail.value());

        Set<String> transports = Set.copyOf(stringIDs);

        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
        editor.putStringSet("eVeK-transports", transports);
        editor.apply();
    }

    /**
     * Method to load the recent {@link TransportDetails Transports} of the {@link User}
     */
    private void loadTransports(){
        if(!validStoring)
            return;
        SharedPreferences editor = encryptedSharedPreferences;
        Set<String> ids = editor.getStringSet("eVeK-transports", null);

        if(ids == null)
            return;

        for(String id : ids)
            transportDetailIDs.add(new Id<>(id));
    }

    /**
     * Method to try to assign a {@link TransportDetails Transport} to the current {@link User Users} {@link ServiceProvider}
     *
     * @param input the {@link Id} of the {@link TransportDetails Transport} as {@link String}
     *
     * @return {@link TransportDetails TransportDetails} - the {@link TransportDetails Transport} that were assigned
     *
     * @throws ProcessingException thrown, when The String format does not match or the {@link TransportDetails Transport} could not be assigned
     */
    public TransportDetails tryAssignTransport(String input) throws ProcessingException {
        if(!input.matches("\\w{8}-\\w{4}-\\w{4}-\\w{4}-\\w{12}"))
            throw new IllegalArgumentException("String format does not match!");
        try {
            sender.sendTransportDetails(new TransportDetails.AssignTransportProvider(new Id<>(input), loginUser.serviceProvider()));
            TransportDetails assigned = receiver.receiveTransportDetails();
            addTransport(assigned);
            return assigned;
        } catch (Exception e) {
            /*if(e instanceof SocketException){
                try{
                    serverConnection.resetAndReconnect(loginUser);

                    sender.sendTransportDetails(new TransportDetails.AssignTransportProvider(new Id<>(input), loginUser.user().serviceProvider()));
                    TransportDetails assigned = receiver.receiveTransportDetails();
                    addTransport(assigned);
                    return assigned;
                }catch(Exception ex){
                    Log.sendException(ex);
                    throw new ProcessingException(ex);
                }
            }*/
            if(e instanceof SocketException){
                //TODO Reconnect on every method!
            }
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to update a {@link TransportDetails Transport} with the given parameters
     *
     * @param transportID the {@link Id} of the {@link TransportDetails Transport} to be updated
     * @param tourNumber the tour number for the {@link TransportDetails Transport}
     * @param startAddress the start {@link Address} of the {@link TransportDetails Transport}
     * @param endAddress the end {@link Address} of the {@link TransportDetails Transport}
     * @param direction the {@link Direction} of the {@link TransportDetails Transport}
     * @param patientCondition the condition of the patient whilst the {@link TransportDetails Transport}
     * @param paymentExemption if the patient has a payment exemption
     *
     * @return {@link TransportDetails} - the {@link TransportDetails Transport} with the updated properties
     *
     * @throws ProcessingException thrown, when the {@link TransportDetails Transport} could not be assigned
     */
    public TransportDetails updateTransport(Id<TransportDetails> transportID, COptional<String> tourNumber, Reference<Address> startAddress, Reference<Address> endAddress,
                                            Direction direction, PatientCondition patientCondition, boolean paymentExemption) throws ProcessingException {
        try {
            sender.sendTransportDetails(new TransportDetails.Update(transportID, COptional.of(startAddress), COptional.of(endAddress),
                    COptional.of(direction), COptional.of(patientCondition), tourNumber, COptional.of(paymentExemption)));
            TransportDetails updated = receiver.receiveTransportDetails();
            addTransport(updated);
            return updated;
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to get an {@link Address} by its {@link Id}
     *
     * @param addressId the {@link Id} of the {@link Address} as {@link String}
     *
     * @return {@link Address} - the {@link Address} with the given {@link Id}
     *
     * @throws ProcessingException thrown, when the {@link Address} could not be found
     */
    public Address getAddressById(String addressId) throws ProcessingException {
        return getAddressById(new Id<>(addressId));
    }

    /**
     * Method to get an {@link Address} by its {@link Id}
     *
     * @param addressId the {@link Id} of the {@link Address}
     *
     * @return {@link Address} - the {@link Address} with the given {@link Id}
     *
     * @throws ProcessingException thrown, when the {@link Address} could not be found
     */
    public Address getAddressById(Id<Address> addressId) throws ProcessingException {
        try {
            sender.sendAddress(new Address.Get(addressId));
            return receiver.receiveAddress();
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to create a new {@link Address} with the given parameters
     *
     * @param street the street name of the {@link Address}
     * @param houseNumber the house number of the {@link Address}
     * @param postCode the post code of the {@link Address}
     * @param city the city name of the {@link Address}
     * @param country the country code of the {@link Address}
     *
     * @return {@link Address} - the created {@link Address}
     *
     * @throws ProcessingException thrown, when the {@link Address} could not be created
     */
    public Address createAddress(String street, String houseNumber, String postCode, String city, String country) throws ProcessingException {
        try{
            sender.sendAddress(new Address.Create(COptional.empty(), street, houseNumber, country, postCode, city));
            return receiver.receiveAddress();
        }catch(Exception e){
            Log.sendException(e);
            throw new ProcessingException(e);
        }
    }

    /**
     * Method to set the next Logged in {@link User} to be stored
     *
     * @param toStore if the next {@link User} should be stored
     */
    public void storeNextUser(boolean toStore){
        this.storeNextUser = toStore;
    }

    /**
     * Method to try to login by stored credentials
     *
     * @return {@link Boolean} - if a {@link User} has been logged in
     */
    public boolean tryLoginFromStoredCredentials(){
        if(!validStoring)
            return false;
            // Passwort abrufen
        String savedPassword = encryptedSharedPreferences.getString("eVeK-password", null);
        String savedUser = encryptedSharedPreferences.getString("eVeK-username", null);

        if (savedPassword != null && savedUser != null
                && !savedPassword.isBlank() && !savedUser.isBlank()) {
            storeNextUser(true);
            tryLogin(savedUser, savedPassword);
            return true;
        }
        Log.sendMessage("No Password or Username stored!");
        return false;
    }

    /**
     * Method to try to login by given credentials
     *
     * @param username the username to be tried to be logged in
     * @param password the password to be tried to be logged in
     */
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
                /*if(e instanceof SocketException){
                    try{
                        serverConnection.resetAndReconnect(loginUser);

                        this.sender.loginUser(username, password);
                        User loginUser = this.receiver.receiveUser();
                        if(loginUser != null) {
                            t = switch (loginUser.role()) {
                                case HealthcareAdmin, HealthcareUser, InsuranceAdmin,
                                     InsuranceUser, TransportAdmin, TransportInvoice ->
                                        new NoValidUserRoleException(loginUser.role(), "Mobile (App) Login");
                                case HealthcareDoctor, TransportDoctor, TransportUser, SuperUser -> {
                                    this.loginUser = new LoginUser(loginUser, password);
                                    yield new UserLoggedInThrowable(loginUser);
                                }
                            };
                        }
                    }catch(Exception ex){
                        t = e;
                    }
                }else*/
                if(e instanceof SocketException){
                    //TODO Reconnect on every method!
                }
                t = e;
            }
            SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
            if(!(t instanceof UserLoggedInThrowable)) {
                this.loginUser = null;
                if(validStoring){
                    editor.putString("eVeK-password", null);
                    editor.apply();
                }
                Log.sendMessage("User " + username + " could not be logged in!");
                Log.sendException(t);
            }else{
                Log.sendMessage("User " + username + " successfully logged in!");
                if(validStoring){
                    SharedPreferences pref = encryptedSharedPreferences;
                    String oldUsername;
                    if((oldUsername = pref.getString("eVeK-username", null)) != null
                            && oldUsername.equalsIgnoreCase(username)){
                        loadTransports();
                        loadTransportDocuments();
                    } else clearTransportCache();
                }
                if(storeNextUser){
                    if(validStoring){
                        editor.putString("eVeK-password", password);
                        editor.putString("eVeK-username", username);
                        editor.apply();
                        Log.sendMessage("User " + username + " successfully saved for auto login!");
                    }else{
                        storeNextUser = false;
                        Log.sendMessage("User " + username + " could not be saved for auto login!");
                    }
                }
            }
            changeLoginState(t);
        }).start();
    }

    /**
     * Method to logout the current {@link User}
     *
     * @throws IllegalProcessException thrown, when no {@link User} is logged in or the Server Connection could not be reset
     */
    public void logout() throws IllegalProcessException{
        if(loginUser == null)
            throw new IllegalProcessException(new UserNotProvidedException("No user logged in!"));
        loginUser = null;
        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
        editor.remove("eVeK-password");
        editor.apply();
        clearTransportCache();
        storeNextUser = false;
        try{
            serverConnection.resetConnection();
        }catch(IllegalProcessException e){
            Log.sendException(e);
        }
        changeLoginState(new UserLogoutThrowable());
    }

    /**
     * Method to clear the cache of {@link TransportDetails Transports} and {@link TransportDocument TransportDocuments}
     */
    private void clearTransportCache(){
        if(!validStoring)
            return;
        transportDocumentIDs.clear();
        transportDetailIDs.clear();
        SharedPreferences.Editor editor = encryptedSharedPreferences.edit();
        editor.remove("eVeK-transports");
        editor.remove("eVeK-transportdocuments");
        editor.apply();
    }

    /**
     * Method to get a valid {@link Date} from a {@link String}
     *
     * @param input the {@link String} containing a Date
     *
     * @return {@link Date} - the {@link Date} represented in the {@link String}
     *
     * @throws IllegalProcessException thrown, when the {@link String} does not contain a valid formatted {@link Date}
     */
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

    /**
     * Method to get the full representation of a {@link TransportationType} to be displayed
     *
     * @param type the {@link TransportationType} to be displayed
     *
     * @return {@link String} - the {@link TransportationType}'s representation
     */
    public static String getTransportationTypeString(TransportationType type){
        return switch(type) {
            case Taxi -> "Taxi/Mietwagen";
            case KTW -> "KTW, da medizinisch-fachliche Betreuung und/oder Einrichtung notwendig ist (Begründung unter 4. erforderlich)";
            case RTW -> "RTW";
            case NAWorNEF -> "NAW/NEF";
            case Other -> "andere (Spezifizierung unter 4. erforderlich)";
        };
    }

    /**
     * Method to get the compact representation of a {@link TransportationType} to be displayed
     *
     * @param type the {@link TransportationType} to be displayed
     *
     * @return String - the {@link TransportationType}'s representation
     */
    public static String getTransportationTypeCompactString(TransportationType type){
        return switch(type) {
            case Taxi -> "Taxi/Mietwagen";
            case KTW -> "KTW";
            case RTW -> "RTW";
            case NAWorNEF -> "NAW/NEF";
            case Other -> "andere";
        };
    }

    /**
     * Method to get the full representation of a {@link PatientCondition} to be displayed
     *
     * @param patientCondition - the {@link PatientCondition} to be displayed
     *
     * @return String - the {@link PatientCondition}'s representation
     */
    public static String getPatientConditionString(PatientCondition patientCondition){
        return switch(patientCondition) {
            case CarryingChair -> "Tragestuhl";
            case WheelChair -> "Rollstuhl";
            case LyingDown -> "liegend";
        };
    }

    /**
     * Method to get the full representation of a {@link TransportReason} to be displayed
     *
     * @param transportReason - the {@link TransportReason} to be displayed
     *
     * @return String - the {@link TransportReason}'s representation
     */
    public static String getTransportReasonString(TransportReason transportReason){
        return switch(transportReason) {
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

    /**
     * Method to get the compact representation of a {@link TransportReason} to be displayed
     *
     * @param transportReason - the {@link TransportReason} to be displayed
     *
     * @return String - the {@link TransportReason}'s representation
     */
    public static String getTransportReasonCompactString(TransportReason transportReason){
        return switch(transportReason) {
            case EmergencyTransport -> "a1) Notfalltransport";
            case FullPartStationary -> "a2) voll-/teilstationäre Krankenhausbehandlung";
            case PrePostStationary -> "a3) vor-/nachstationäre Behandlung";
            case AmbulantTaxi -> "b) ambulante Behandlung Taxi/Mietwagen";
            case OtherPermitFree -> "c) anderer Grund Genehmigungsfrei";
            case HighFrequent -> "d1) hochfrequente Behandlung";
            case HighFrequentAlike -> "d2) vergleichbarer Ausnahmefall";
            case ContinuousImpairment -> "e) dauerhafte Mobilitätsbeeinträchtigung";
            case OtherKTW -> "f) anderer Grund für Fahrt mit KTW";
        };
    }
}
