package de.ehealth.evek.mobile.frontend;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Date;
import java.text.SimpleDateFormat;
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
 * @extends {@link Fragment}
 *
 * @implements {@link SingleChoiceRecyclerAdapter.ItemClickListener}
 */
public class EditorTransportDocumentFragment extends Fragment implements SingleChoiceRecyclerAdapter.ItemClickListener {
    private SingleChoiceRecyclerAdapter<TransportReason> transportReasonAdapter;
    private SingleChoiceRecyclerAdapter<TransportationType> transportationTypeAdapter;
    private Id<TransportDocument> transportDocument = null;
    private Boolean editPatient = false;
    private TransportReason reason = null;
    private TransportationType type = null;
    private InsuranceData tempInsuranceData = null;
    private String ikNumber = "";
    private int insuranceStatus = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String transportDocID;
        if(getArguments() != null){
            if((transportDocID = getArguments().getString("transportDocumentID")) != null
                    && !transportDocID.isBlank())
                this.transportDocument = new Id<>(transportDocID);
            this.editPatient = getArguments().getBoolean("editPatientData");

        }
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


        view.findViewById(R.id.btn_save_transportdoc).setOnClickListener((v) -> createTransportDoc(view));
        view.findViewById(R.id.btn_edit_transportdoc).setOnClickListener((v) -> setEditable(true, view));

        Switch sw = view.findViewById(R.id.sw_keep_insurance_data);
        EditText ikNumber = view.findViewById(R.id.et_ik_number);
        EditText insuranceStatus = view.findViewById(R.id.et_insurance_status);

        sw.setOnClickListener((v) -> {
            if(sw.isChecked()){
                if(!ikNumber.getText().toString().isBlank())
                    this.ikNumber = ikNumber.getText().toString();
                if(!insuranceStatus.getText().toString().isBlank())
                    this.insuranceStatus = Integer.parseInt(insuranceStatus.getText().toString());
                setInsuranceDataEditable(false, view);
                if(tempInsuranceData == null)
                    return;
                ikNumber.setText(tempInsuranceData.insurance().id().value());
                insuranceStatus.setText(String.valueOf(tempInsuranceData.insuranceStatus()));

            }else {
                setInsuranceDataEditable(true, view);
                ikNumber.setText(this.ikNumber);
                if(this.insuranceStatus >= 0 && this.insuranceStatus < 9999999)
                    insuranceStatus.setText(String.valueOf(this.insuranceStatus));
                else
                    insuranceStatus.setText("");
            }
        });

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
                String insuranceNr = new String(c);
                if(c != chars)
                    insuranceNumber.setText(insuranceNr);

                if(insuranceNr.length() != 10)
                    return;
                setTemporaryInsuranceData(insuranceNr, view);
            }
        });

        //set Active TransportDoc, if given
        if(transportDocument == null)
            return view;
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            try{
                TransportDocument document = handler.getTransportDocumentById(transportDocument.value());
                if(document == null)
                    throw new IllegalProcessException("Transport with ID " + transportDocument.value() + " not found!");
                if(document.insuranceData().isPresent())
                    tempInsuranceData = handler.getInsuranceData(document.insuranceData().get().id());

                if(getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    if(document.patient().isPresent()){
                        ((EditText) view.findViewById(R.id.et_insurance_number)).setText(document.patient().get().id().value());
                        if(tempInsuranceData != null){
                            ((Switch) view.findViewById(R.id.sw_keep_insurance_data)).setChecked(false);
                            ((EditText) view.findViewById(R.id.et_ik_number)).setText(tempInsuranceData.insurance().id().value());
                            ((EditText) view.findViewById(R.id.et_insurance_status)).setText(String.valueOf(tempInsuranceData.insuranceStatus()));

                        } else
                            setTemporaryInsuranceData(document.patient().get().id().value(), view);
                    }
                    transportReasonAdapter.setActiveItem(document.transportReason());
                    ((EditText) view.findViewById(R.id.et_transport_date)).setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(document.startDate().getTime()));
                    ((EditText) view.findViewById(R.id.et_service_provider)).setText(document.healthcareServiceProvider().id().value());
                    if(document.weeklyFrequency().isPresent() && document.endDate().isPresent()) {
                        ((EditText) view.findViewById(R.id.et_end_date)).setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(document.endDate().get().getTime()));
                        ((EditText) view.findViewById(R.id.et_weekly_frequency)).setText(String.format(Locale.GERMANY, "%d", document.weeklyFrequency().get()));
                    }
                    transportationTypeAdapter.setActiveItem(document.transportationType());
                    if(document.additionalInfo().isPresent())
                        ((EditText) view.findViewById(R.id.et_info)).setText(document.additionalInfo().get());
                    setEditable(false, view);
                    setPatientDataEditable(editPatient, view);

                });
            }catch(IllegalProcessException | ProcessingException e){
                Log.sendMessage("Transport konnte nicht geladen werden!");
            }
        });

        return view;
    }

    /**
     * Method to set the temporary {@link InsuranceData Insurance Data} for using on the {@link EditText EditText's}
     *
     * @param insuranceNumber the insurance Number of the {@link Patient} to get the {@link InsuranceData Insurance Data} from
     * @param view the {@link View} calling the method
     */
    private void setTemporaryInsuranceData(String insuranceNumber, View view){
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            try {
                tempInsuranceData = handler.getInsuranceDataByPatient(insuranceNumber);
                if(getActivity() == null)
                    return;
                getActivity().runOnUiThread(() -> {
                    EditText etIKNumber = view.findViewById(R.id.et_ik_number);
                    EditText etInsuranceStatus = view.findViewById(R.id.et_insurance_status);
                    Switch sw = view.findViewById(R.id.sw_keep_insurance_data);
                    if (sw.isChecked()) {
                        etIKNumber.setText(tempInsuranceData.insurance().id().value());
                        etInsuranceStatus.setText(String.valueOf(tempInsuranceData.insuranceStatus()));
                    }
                });
            } catch (ProcessingException e) {
                if(getActivity() != null)
                    getActivity().runOnUiThread(() -> ((MainActivity) getActivity()).exceptionAlert("Fehler!", e));
            }
        });
    }

    /**
     * Method used for setting the edit field for the insurance number editable or uneditable.
     *
     * @param editable if the fields should be editable
     * @param view the {@link View} calling the method
     */
    private void setPatientDataEditable(boolean editable, View view){
        EditText insuranceNumber = view.findViewById(R.id.et_insurance_number);
        Switch sw = view.findViewById(R.id.sw_keep_insurance_data);
        insuranceNumber.setEnabled(editable);
        sw.setEnabled(editable);
        view.findViewById(R.id.constraint_save).setVisibility(editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.constraint_save).setEnabled(editable);
    }

    /**
     * Method used for setting the edit fields for the insurance data editable or uneditable.
     *
     * @param editable if the fields should be editable
     * @param view the {@link View} calling the method
     */
    private void setInsuranceDataEditable(boolean editable, View view){
        EditText ikNumber = view.findViewById(R.id.et_ik_number);
        EditText insuranceStatus = view.findViewById(R.id.et_insurance_status);
        ikNumber.setEnabled(editable);
        insuranceStatus.setEnabled(editable);
    }

    /**
     * Method used for setting the edit fields editable or uneditable.
     *
     * @param editable if the fields should be editable
     * @param view the {@link View} calling the method
     */
    private void setEditable(boolean editable, View view){
        view.findViewById(R.id.et_insurance_number).setEnabled(editable);
        view.findViewById(R.id.rv_transport_reason).setEnabled(editable);
        view.findViewById(R.id.et_transport_date).setEnabled(editable);
        view.findViewById(R.id.et_service_provider).setEnabled(editable);
        view.findViewById(R.id.et_weekly_frequency).setEnabled(editable);
        view.findViewById(R.id.et_end_date).setEnabled(editable);
        view.findViewById(R.id.rv_transportation_type).setEnabled(editable);
        view.findViewById(R.id.et_info).setEnabled(editable);
        view.findViewById(R.id.constraint_save).setVisibility(editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.constraint_save).setEnabled(editable);
        view.findViewById(R.id.constraint_edit).setVisibility(!editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.constraint_edit).setEnabled(!editable);
        transportReasonAdapter.setEditable(editable);
        transportationTypeAdapter.setEditable(editable);
        setPatientDataEditable(editable, view);
    }

    /**
     * Method used for creating a TransportDocument
     *
     * @param view the {@link View} calling the method
     */
    @SuppressLint("PrivateResource")
    private void createTransportDoc(View view) {
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            boolean valid = true;
            //TODO insuranceData!

            String patStr = ((EditText) view.findViewById(R.id.et_insurance_number)).getText().toString();
            String insStatusStr = ((EditText) view.findViewById(R.id.et_insurance_status)).getText().toString();
            String ikStr = ((EditText) view.findViewById(R.id.et_ik_number)).getText().toString();
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

            TypedValue textColor = new TypedValue();
            TypedValue hintColor = new TypedValue();
            TypedValue mistakeColor = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.textColorPrimary, textColor, true);
            getActivity().getTheme().resolveAttribute(android.R.attr.textColorHint, hintColor, true);
            getActivity().getTheme().resolveAttribute(android.R.attr.colorError, mistakeColor, true);

            if(!patStr.isBlank()) {
                patient = COptional.of(Reference.to(patStr));

                if (!((Switch) view.findViewById(R.id.sw_keep_insurance_data)).isChecked()) {
                    if (!insStatusStr.isBlank()) {
                        try {
                            int status = Integer.parseInt(insStatusStr);
                            tempInsuranceData = handler.createInsuranceData(Reference.to(patStr), Reference.to(ikStr), status);
                            insuranceData = COptional.of(Reference.to(tempInsuranceData.id().value()));
                            getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_ik_number)).setHintTextColor(hintColor.data));
                            getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_ik_number)).setTextColor(textColor.data));
                        }catch(Exception e){
                            getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_ik_number)).setHintTextColor(mistakeColor.data));
                            getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_ik_number)).setTextColor(mistakeColor.data));
                            valid = false;
                        }
                    }
                }else{
                    insuranceData = COptional.of(Reference.to(tempInsuranceData.id().value()));
                }
            }
            if(spStr.isBlank()){
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_service_provider)).setHintTextColor(mistakeColor.data));
                valid = false;
            }
            if(!infoStr.isBlank())
                info = COptional.of(infoStr);


            if(((EditText) view.findViewById(R.id.et_transport_date)).getText().toString().isBlank()){
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_date)).setHintTextColor(mistakeColor.data));
                valid = false;
            }else {
                try {
                    startDate = DataHandler.getDate(((EditText) view.findViewById(R.id.et_transport_date)).getText().toString());
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_date)).setTextColor(textColor.data));
                } catch (Exception e) {
                    Log.sendException(e);
                    valid = false;
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_date)).setHintTextColor(mistakeColor.data));
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_date)).setTextColor(mistakeColor.data));
                }
            }

            if(!((EditText) view.findViewById(R.id.et_end_date)).getText().toString().isBlank()
                    && !((EditText) view.findViewById(R.id.et_weekly_frequency)).getText().toString().isBlank()){
                try{
                    weeklyFrequency = COptional.of(Integer.parseInt(((EditText) view.findViewById(R.id.et_weekly_frequency)).getText().toString()));
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_weekly_frequency)).setTextColor(textColor.data));
                }catch(NumberFormatException e){
                    Log.sendException(e);
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_weekly_frequency)).setTextColor(mistakeColor.data));

                    valid = false;
                }
                try{
                    endDate = COptional.of(DataHandler.getDate(((EditText) view.findViewById(R.id.et_end_date)).getText().toString()));
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_end_date)).setTextColor(textColor.data));
                }catch(IllegalProcessException e) {
                    Log.sendException(e);
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_end_date)).setTextColor(mistakeColor.data));
                    valid = false;
                }
            }
            else if(!((EditText) view.findViewById(R.id.et_end_date)).getText().toString().isBlank()
                    || !((EditText) view.findViewById(R.id.et_weekly_frequency)).getText().toString().isBlank()){

                getActivity().runOnUiThread(() -> {
                    ((EditText) view.findViewById(R.id.et_end_date)).setHintTextColor(mistakeColor.data);
                    ((EditText) view.findViewById(R.id.et_end_date)).setTextColor(mistakeColor.data);
                    ((EditText) view.findViewById(R.id.et_weekly_frequency)).setHintTextColor(mistakeColor.data);
                    ((EditText) view.findViewById(R.id.et_weekly_frequency)).setTextColor(mistakeColor.data);
                } );

                valid = false;
            }else
                getActivity().runOnUiThread(() -> {
                    ((EditText) view.findViewById(R.id.et_end_date)).setHintTextColor(hintColor.data);
                    ((EditText) view.findViewById(R.id.et_end_date)).setTextColor(textColor.data);
                    ((EditText) view.findViewById(R.id.et_weekly_frequency)).setHintTextColor(hintColor.data);
                    ((EditText) view.findViewById(R.id.et_weekly_frequency)).setTextColor(textColor.data);
                } );

            if(type == TransportationType.KTW || reason == TransportReason.ContinuousImpairment
                    || reason == TransportReason.HighFrequentAlike || reason == TransportReason.OtherKTW)
                if(info.isEmpty()){
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_info)).setHintTextColor(mistakeColor.data));
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_info)).setTextColor(mistakeColor.data));
                    valid = false;
                }else
                    getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_info)).setTextColor(textColor.data));

            if(type == null){
                transportationTypeAdapter.setValid(false);
                valid = false;
            }else
                transportationTypeAdapter.setValid(true);

            if(reason == null){
                transportReasonAdapter.setValid(false);
                valid = false;
            }else
                transportReasonAdapter.setValid(true);

            if(!valid)
                return;

            TransportDocument transportDocument = null;
            try {
                if(this.transportDocument != null)
                    transportDocument = handler.getTransportDocumentById(this.transportDocument);

                if(transportDocument != null) {
                    if (patient.isPresent()
                            && (transportDocument.patient().isEmpty() || !(transportDocument.patient().get().id().value().equals(patient.get().id().value()))
                            || transportDocument.insuranceData().isEmpty() || !(transportDocument.insuranceData().get().id().value().equals(insuranceData.get().id().value()))))
                            transportDocument = handler.updateTransportDocumentWithPatient(this.transportDocument, patient.get(), insuranceData, reason, startDate, endDate, weeklyFrequency, serviceProvider, type, info);
                    else
                        transportDocument = handler.updateTransportDocument(this.transportDocument, reason, startDate, endDate, weeklyFrequency, serviceProvider, type, info);
                } else
                    transportDocument = handler.createTransportDocument(patient, insuranceData, reason, startDate, endDate, weeklyFrequency, serviceProvider, type, info);
            }catch(ProcessingException e){
                if(getActivity() == null)
                    return;
                ((MainActivity) getActivity()).exceptionAlert("Transportschein konnte nicht erstellt werden!", e);
            }
            if(getActivity() == null
                    || transportDocument == null)
                return;

            TransportDocument finalTransportDocument = transportDocument;
            ((MainActivity) getActivity()).choiceAlert(
                    "Transportschein wurde erstellt!",
                    "Transportschein wurde erfolgreich mit ID " + transportDocument.id().value() + " erstellt! \n\r\n\rSoll ein Transport für den Transportschein erstellt werden?",
                     "Nein",
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
        if (obj == TransportReason.class) {
            reason = transportReasonAdapter.getItem(position);
            if (reason == TransportReason.OtherKTW) {
                transportationTypeAdapter.setActiveItem(TransportationType.KTW);
                type = TransportationType.KTW;
                transportationTypeAdapter.setEditable(false);
            } else {
                transportationTypeAdapter.setEditable(true);
            }
            transportReasonAdapter.setValid(true);
            //Debug.sendMessage("Clicked on " + transportReasonAdapter.getItem(position).toString() + " on position " + position);
        }
        else if(obj == TransportationType.class) {
            type = transportationTypeAdapter.getItem(position);
            transportationTypeAdapter.setValid(true);
            //Debug.sendMessage("Clicked on " + transportationTypeAdapter.getItem(position).toString() + " on position " + position);
        }
    }
}
