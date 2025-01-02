package de.ehealth.evek.mobile.frontend;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import de.ehealth.evek.api.entity.InsuranceData;
import de.ehealth.evek.api.entity.Patient;
import de.ehealth.evek.api.entity.ServiceProvider;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.type.Id;
import de.ehealth.evek.api.type.Reference;
import de.ehealth.evek.api.type.TransportReason;
import de.ehealth.evek.api.type.TransportationType;
import de.ehealth.evek.api.util.COptional;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.network.DataHandler;

/**
 * Class belonging to the EditorTransportDocument Fragment
 *
 * @extends Fragment
 *
 * @implements SingleChoiceRecyclerAdapter.ItemClickListener
 */
public class EditorTransportDocumentFragment extends Fragment implements SingleChoiceRecyclerAdapter.ItemClickListener {
    private SingleChoiceRecyclerAdapter<TransportReason> transportReasonAdapter;
    private SingleChoiceRecyclerAdapter<TransportationType> transportationTypeAdapter;
    private Id<TransportDocument> transportDocument = null;
    private TransportReason reason = null;
    private TransportationType type = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String transportDocID;
        if(getArguments() != null
                && (transportDocID = getArguments().getString("transportDocumentID")) != null
                && !transportDocID.isBlank())
            this.transportDocument = new Id<>(transportDocID);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_editor_transport_document, container, false);

        // data to populate the RecyclerView with
        ArrayList<TransportReason> transportReasons = new ArrayList<>(Arrays.asList(TransportReason.values()));
        // set up the RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.rv_transport_reason);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        transportReasonAdapter = new SingleChoiceRecyclerAdapter<>(getActivity(), transportReasons, TransportReason.class);
        transportReasonAdapter.setClickListener(this);
        recyclerView.setAdapter(transportReasonAdapter);

        ArrayList<TransportationType> transportTypes = new ArrayList<>(Arrays.asList(TransportationType.values()));
        // set up the RecyclerView
        recyclerView = view.findViewById(R.id.rv_transportation_type);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        transportationTypeAdapter = new SingleChoiceRecyclerAdapter<>(getActivity(), transportTypes, TransportationType.class);
        transportationTypeAdapter.setClickListener(this);
        recyclerView.setAdapter(transportationTypeAdapter);


        view.findViewById(R.id.btn_save_transport).setOnClickListener((v) -> createTransportDoc(view));

        EditText insuranceNumber = view.findViewById(R.id.et_insurance_number);
        insuranceNumber.addTextChangedListener(new TextWatcher() {
            String old;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                old = s.toString();
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(old.equals(s.toString()))
                    return;
                if(s.length() == 0)
                    return;
                char[] chars = new char[s.length()];

                s.getChars(0, s.length(), chars, 0);
                if((chars[0] < 'a' || chars[0] > 'z')
                && (chars[0] < 'A' || chars[0] > 'Z')){
                    insuranceNumber.setText("");
                    return;
                }
                if(chars.length == 1)
                    return;
                char[] c = chars;
                int offset = 0;
                for(int i = 1; i < s.length(); i++ ){
                    if(chars[i] < '0' || chars[i] > '9'){
                        char[] cs = new char[c.length - 1];
                        System.arraycopy(c, 0, cs, 0, i);
                        c = cs;
                        offset++;
                    }else c[i - offset] = chars[i];
                }
                if(c != chars)
                        insuranceNumber.setText(new String (c));
            }
        });

        //set Active TransportDoc, if given
        if(transportDocument == null)
            return view;
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            try{
                TransportDocument document = handler.getTransportDocumentByID(transportDocument.value());
                if(document == null)
                    throw new IllegalProcessException("Transport with ID " + transportDocument.value() + " not found!");
                if(getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    if(document.patient().isPresent())
                        ((EditText) view.findViewById(R.id.et_insurance_number)).setText(document.patient().get().id().value());
                    transportReasonAdapter.setActiveItem(document.transportReason());
                    ((EditText) view.findViewById(R.id.et_transport_date)).setText(document.startDate().toString());
                    ((EditText) view.findViewById(R.id.et_service_provider)).setText(document.healthcareServiceProvider().id().value());
                    if(document.weeklyFrequency().isPresent() && document.endDate().isPresent()) {
                        ((EditText) view.findViewById(R.id.et_end_date)).setText(document.endDate().toString());
                        ((EditText) view.findViewById(R.id.et_weekly_frequency)).setText(String.format(Locale.GERMANY, "%d", document.weeklyFrequency().get()));
                    }
                    transportationTypeAdapter.setActiveItem(document.transportationType());
                    if(document.additionalInfo().isPresent())
                        ((EditText) view.findViewById(R.id.et_info)).setText(document.additionalInfo().get());
                });
            }catch(IllegalProcessException e){
                Log.sendMessage("Transport konnte nicht geladen werden!");
            }
        });

        return view;
    }

    /**
     * Method used for creating a TransportDocument
     *
     * @param view - The View calling the method
     */
    private void createTransportDoc(View view) {
        DataHandler.instance().runOnNetworkThread(() -> {
            boolean valid = true;
            //TODO insuranceData!

            String patStr = ((EditText) view.findViewById(R.id.et_insurance_number)).getText().toString();
            String spStr = ((EditText) view.findViewById(R.id.et_service_provider)).getText().toString();
            String infoStr = ((EditText) view.findViewById(R.id.et_info)).getText().toString();

            COptional<Reference<Patient>> patient = COptional.empty();
            Reference<ServiceProvider> serviceProvider = Reference.to(spStr);
            COptional<String> info = COptional.empty();
            COptional<Reference<InsuranceData>> insuranceData = COptional.empty();
            COptional<Date> endDate = COptional.empty();
            COptional<Integer> weeklyFrequency = COptional.empty();

            TransportReason reason = this.reason;
            Date startDate = null;
            TransportationType type = this.type;

            if(getActivity() == null)
                return;

            if(!patStr.isBlank())
                patient = COptional.of(Reference.to(patStr));
            if(spStr.isBlank()){
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_service_provider)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            }
            if(!infoStr.isBlank())
                info = COptional.of(infoStr);

            try{
                startDate = DataHandler.getDate(((EditText) view.findViewById(R.id.et_transport_date)).getText().toString());
            }catch(Exception e){
                Log.sendException(e);
                valid = false;
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_date)).setHintTextColor(Color.argb(255, 255, 100, 100)));
            }

            if(!((EditText) view.findViewById(R.id.et_end_date)).getText().toString().isBlank()
                    && !((EditText) view.findViewById(R.id.et_weekly_frequency)).getText().toString().isBlank()){
                try{
                    weeklyFrequency = COptional.of(Integer.parseInt(((EditText) view.findViewById(R.id.et_weekly_frequency)).getText().toString()));
                }catch(NumberFormatException e){
                    Log.sendException(e);
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_weekly_frequency)).setTextColor(Color.argb(255, 255, 100, 100)));
                    valid = false;
                }
                try{
                    endDate = COptional.of(DataHandler.getDate(((EditText) view.findViewById(R.id.et_end_date)).getText().toString()));
                }catch(IllegalProcessException e) {
                    Log.sendException(e);
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_end_date)).setTextColor(Color.argb(255, 255, 100, 100)));
                    valid = false;
                }
            }
            else if(!((EditText) view.findViewById(R.id.et_end_date)).getText().toString().isBlank()
                    || !((EditText) view.findViewById(R.id.et_weekly_frequency)).getText().toString().isBlank()){

                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_end_date)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_weekly_frequency)).setHintTextColor(Color.argb(255, 255, 100, 100)));

                valid = false;
            }

            if(!valid)
                return;

            DataHandler handler = DataHandler.instance();
            TransportDocument transportDocument = null;
            try {
                if(this.transportDocument != null)
                    transportDocument = handler.getTransportDocumentByID(this.transportDocument);

                if(transportDocument != null) {
                    if (patient.isPresent() && insuranceData.isPresent()
                            && (transportDocument.patient().isEmpty() || !(transportDocument.patient().get().id().value().equals(patient.get().id().value()))
                            || transportDocument.insuranceData().isEmpty() || !(transportDocument.insuranceData().get().id().value().equals(insuranceData.get().id().value()))))
                        transportDocument = handler.updateTransportDocumentWithPatient(this.transportDocument, patient.get(), insuranceData.get(), reason, startDate, endDate, weeklyFrequency, serviceProvider, type, info);
                    else
                        transportDocument = handler.updateTransportDocument(this.transportDocument, reason, startDate, endDate, weeklyFrequency, serviceProvider, type, info);
                } else
                    transportDocument = handler.createTransportDocument(patient, insuranceData, reason, startDate, endDate, weeklyFrequency, serviceProvider, type, info);
            }catch(IllegalProcessException | ProcessingException e){
                if(getActivity() == null)
                    return;
                ((MainActivity) getActivity()).exceptionAlert(e, "Transportschein konnte nicht erstellt werden!");
            }
            if(getActivity() == null
                    || transportDocument == null)
                return;

            TransportDocument finalTransportDocument = transportDocument;
            ((MainActivity) getActivity()).choiceAlert("Transportschein wurde erfolgreich mit ID " + transportDocument.id().value() + " erstellt! \n\r\n\rSoll ein Transport für den Transportschein erstellt werden?",
                    "Transportschein wurde erstellt!", "Nein",
                    (dialog, which) -> {
                        if(getActivity() != null){

                            NavController navController = NavHostFragment.findNavController(EditorTransportDocumentFragment.this);
                            if(navController.getCurrentDestination() == null
                                    || navController.getCurrentDestination().getId() != R.id.editorTransportDocumentFragment) return;
                            navController.navigateUp();
                        }
                        dialog.dismiss();
                    },
                    "Ja",
                    (dialog, which) -> {
                        if(getActivity() != null){

                            NavController navController = NavHostFragment.findNavController(EditorTransportDocumentFragment.this);
                            if(navController.getCurrentDestination() == null
                                    || navController.getCurrentDestination().getId() != R.id.editorTransportDocumentFragment) return;
                            Bundle bundle = new Bundle();
                            bundle.putString("transportDocumentID", finalTransportDocument.id().value());
                            navController.navigate(R.id.action_doctorEditorTransportDocFragment_to_editorTransportCreateFragment, bundle);

                        }
                        dialog.dismiss();
                    });
        });
    }

    @Override
    public <T> void onItemClick(T obj, int position) {
        if (obj == TransportReason.class)
            reason = transportReasonAdapter.getItem(position);

            //Debug.sendMessage("Clicked on " + transportReasonAdapter.getItem(position).toString() + " on position " + position);

        else if(obj == TransportationType.class)
            type = transportationTypeAdapter.getItem(position);

            //Debug.sendMessage("Clicked on " + transportationTypeAdapter.getItem(position).toString() + " on position " + position);

    }
}
