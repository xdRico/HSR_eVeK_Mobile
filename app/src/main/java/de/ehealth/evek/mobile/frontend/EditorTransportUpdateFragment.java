package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import de.ehealth.evek.api.entity.Address;
import de.ehealth.evek.api.entity.Patient;
import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.type.Direction;
import de.ehealth.evek.api.type.Id;
import de.ehealth.evek.api.type.PatientCondition;
import de.ehealth.evek.api.type.Reference;
import de.ehealth.evek.api.util.COptional;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.network.DataHandler;
import se.warting.signatureview.views.SignaturePad;
import se.warting.signatureview.views.SignedListener;

/**
 * Class belonging to the EditorTransportUpdate Fragment
 *
 * @extends {@link Fragment}
 *
 * @implements {@link SingleChoiceRecyclerAdapter.ItemClickListener}
 */
public class EditorTransportUpdateFragment extends Fragment implements SingleChoiceRecyclerAdapter.ItemClickListener {
    private SingleChoiceRecyclerAdapter<PatientCondition> patientConditionAdapter;
    private Id<TransportDetails> transportID = null;
    private PatientCondition patientCondition = null;
    private SignaturePad transporterSignaturePad = null;
    private SignaturePad patientSignaturePad = null;

    private boolean patientValidation = false;
    private boolean transporterValidation = false;
    private boolean finished = false;
    private boolean validSigning = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String transportID;
        if(getArguments() != null
                && (transportID = getArguments().getString("transportID")) != null
                && !transportID.isBlank()){
            this.transportID = new Id<>(transportID);
            this.patientValidation = getArguments().getBoolean("validation");
            this.finished = getArguments().getBoolean("finished");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_editor_transport_update, container, false);

        // data to populate the RecyclerView with
        ArrayList<PatientCondition> patientConditions = new ArrayList<>(Arrays.asList(PatientCondition.values()));
        // set up the RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.rv_patient_condition);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        patientConditionAdapter = new SingleChoiceRecyclerAdapter<>(getActivity(), patientConditions, PatientCondition.class);
        patientConditionAdapter.setClickListener(this);
        recyclerView.setAdapter(patientConditionAdapter);

        view.findViewById(R.id.btn_save_transportdoc).setOnClickListener((v) -> updateTransport(view));
        view.findViewById(R.id.btn_edit_transportdoc).setOnClickListener((v) -> setEditable(true, view));

        transporterSignaturePad = view.findViewById(R.id.sp_transporter);
        patientSignaturePad = view.findViewById(R.id.sp_patient);

        transporterSignaturePad.setOnSignedListener(new SignedListener() {
            @Override
            public void onSigning() {}
            @Override
            public void onStartSigning() {}
            @Override
            public void onClear() { validSigning = false; }
            @Override
            public void onSigned() { validSigning = true; }
        });
        patientSignaturePad.setOnSignedListener(new SignedListener() {
            @Override
            public void onSigning() {}
            @Override
            public void onStartSigning() {}
            @Override
            public void onClear() { validSigning = false; }
            @Override
            public void onSigned() { validSigning = true; }
        });

        view.findViewById(R.id.ib_delete_signature_transporter).setOnClickListener((v) -> transporterSignaturePad.clear());
        view.findViewById(R.id.ib_delete_signature_patient).setOnClickListener((v) -> patientSignaturePad.clear());


        //set Active Transport, if given
        if(transportID == null)
            return view;

        if(patientValidation || transporterValidation) {
            view.findViewById(R.id.constraint_save).setVisibility(View.VISIBLE);
            view.findViewById(R.id.constraint_edit).setVisibility(View.GONE);
            view.findViewById(R.id.ll_patient_signature).setVisibility(patientValidation ? View.VISIBLE : View.GONE);
        }

        EditText startStreet = view.findViewById(R.id.et_address_start_street);
        EditText startHouse = view.findViewById(R.id.et_address_start_housenumber);
        EditText startZip = view.findViewById(R.id.et_address_start_zipcode);
        EditText startCity = view.findViewById(R.id.et_address_start_city);

        EditText endStreet = view.findViewById(R.id.et_address_end_street);
        EditText endHouse = view.findViewById(R.id.et_address_end_housenumber);
        EditText endZip = view.findViewById(R.id.et_address_end_zipcode);
        EditText endCity = view.findViewById(R.id.et_address_end_city);
        //TODO country

        view.findViewById(R.id.tb_direction).setOnClickListener((v) -> {

            String tStreet = startStreet.getText().toString();
            String tHouse = startHouse.getText().toString();
            String tZip = startZip.getText().toString();
            String tCity = startCity.getText().toString();
            //TODO country

            startStreet.setText(endStreet.getText().toString());
            startHouse.setText(endHouse.getText().toString());
            startZip.setText(endZip.getText().toString());
            startCity.setText(endCity.getText().toString());

            endStreet.setText(tStreet);
            endHouse.setText(tHouse);
            endZip.setText(tZip);
            endCity.setText(tCity);
            //TODO country
        });

        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            try{
                TransportDetails transport = handler.getTransportDetailsById(transportID.value());
                TransportDocument document;
                Patient patient;
                Address providerAddress = null;
                Address patientAddress = null;
                try {
                    document = handler.getTransportDocumentById(transport.transportDocument().id());
                    try{
                        if(document.patient().isPresent()) {
                            patient = handler.getPatient(document.patient().get().id());
                            patientAddress = handler.getAddressById(patient.address().id());
                        }
                    }catch(Exception e){
                        Log.sendException(e);
                    }
                    providerAddress = handler.getAddressById(handler.getServiceProviderById(document.healthcareServiceProvider().id()).address().id());

                }catch (Exception e){
                    Log.sendException(e);
                }
                view.findViewById(R.id.btn_transport_doc).setOnClickListener((v) -> {
                    NavController navController = NavHostFragment.findNavController(EditorTransportUpdateFragment.this);
                    Bundle bundle = new Bundle();
                    bundle.putString("transportDocumentID", transport.transportDocument().id().value());
                    bundle.putBoolean("validation", true);
                    navController.navigate(R.id.action_editorTransportUpdateFragment_to_editorTransportDocumentFragment, bundle);
                });

                if(transport == null)
                    throw new IllegalProcessException("Transport with ID " + transportID.value() + " not found!");

                COptional<Address> startAddress = transport.startAddress().isPresent() ? COptional.of(handler.getAddressById(transport.startAddress().get().id())) : COptional.empty();
                COptional<Address> endAddress = transport.endAddress().isPresent() ? COptional.of(handler.getAddressById(transport.endAddress().get().id())) : COptional.empty();

                if(getActivity() == null)
                    return;

                Address finalPatientAddress = patientAddress;
                Address finalProviderAddress = providerAddress;
                getActivity().runOnUiThread(() -> {

                    ((EditText) view.findViewById(R.id.et_transport_date)).setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(transport.transportDate().getTime()));

                    if(transport.tourNumber().isPresent())
                        ((EditText) view.findViewById(R.id.et_tour_number)).setText(transport.tourNumber().get());

                    if(transport.direction().isPresent())
                        ((ToggleButton) view.findViewById(R.id.tb_direction)).setChecked(transport.direction().get() == Direction.Outward);

                    if(startAddress.isPresent()){
                        startStreet.setText(startAddress.get().streetName());
                        startHouse.setText(startAddress.get().houseNumber());
                        startZip.setText(startAddress.get().postCode());
                        startCity.setText(startAddress.get().city());
                        //TODO country
                    }else if(finalPatientAddress != null){
                        startStreet.setText(finalPatientAddress.streetName());
                        startHouse.setText(finalPatientAddress.houseNumber());
                        startZip.setText(finalPatientAddress.postCode());
                        startCity.setText(finalPatientAddress.city());
                    }

                    if(endAddress.isPresent()){
                        endStreet.setText(endAddress.get().streetName());
                        endHouse.setText(endAddress.get().houseNumber());
                        endZip.setText(endAddress.get().postCode());
                        endCity.setText(endAddress.get().city());
                        //TODO country
                    }else if(finalProviderAddress != null){
                        endStreet.setText(finalProviderAddress.streetName());
                        endHouse.setText(finalProviderAddress.houseNumber());
                        endZip.setText(finalProviderAddress.postCode());
                        endCity.setText(finalProviderAddress.city());
                    }

                    if(transport.patientCondition().isPresent())
                        patientConditionAdapter.setActiveItem(transport.patientCondition().get());

                    if(transport.paymentExemption().isPresent())
                        ((CheckBox) view.findViewById(R.id.cb_payment_exemption)).setChecked(transport.paymentExemption().get());

                    if(transport.direction().isPresent()
                            && startAddress.isPresent()
                            && endAddress.isPresent()
                            && transport.patientCondition().isPresent()) {
                        if (transport.transporterSignatureDate().isPresent()
                                && transport.transporterSignature().isPresent()) {
                            patientValidation = true;
                            view.findViewById(R.id.constraint_edit).setVisibility(View.GONE);
                            view.findViewById(R.id.constraint_edit).setEnabled(false);
                        }
                        setEditable(false, view);
                    }
                });
            }catch(IllegalProcessException | ProcessingException e){
                Log.sendMessage("Transport konnte nicht geladen werden!");
            }
        });

        return view;
    }

    /**
     * Method used for setting the edit fields editable or uneditable.
     *
     * @param editable if the fields should be editable
     */
    public void setEditable(boolean editable, View view){
        boolean editableWithValidation = !finished && editable
                && !patientValidation && !transporterValidation;

        view.findViewById(R.id.et_tour_number).setEnabled(editableWithValidation);
        view.findViewById(R.id.tb_direction).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_start_street).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_start_housenumber).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_start_zipcode).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_start_city).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_end_street).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_end_housenumber).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_end_zipcode).setEnabled(editableWithValidation);
        view.findViewById(R.id.et_address_end_city).setEnabled(editableWithValidation);
        view.findViewById(R.id.cb_payment_exemption).setEnabled(editableWithValidation);
        view.findViewById(R.id.constraint_save).setVisibility(!finished ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.constraint_save).setEnabled(!finished);
        view.findViewById(R.id.constraint_edit).setVisibility(!editable && !patientValidation && !transporterValidation && !finished ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.constraint_edit).setEnabled(!editable && !patientValidation && !transporterValidation && !finished);
        view.findViewById(R.id.ib_delete_signature_transporter).setVisibility(editableWithValidation || transporterValidation ? View.VISIBLE : View.GONE);
        if(finished)
            view.findViewById(R.id.ib_delete_signature_patient).setVisibility(View.GONE);
        view.findViewById(R.id.ll_patient_signature).setVisibility(patientValidation || finished ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.ll_patient_signature).setEnabled(patientValidation && !finished);
        patientConditionAdapter.setEditable(editableWithValidation);
        transporterSignaturePad.setEnabled(editableWithValidation || transporterValidation);
        patientSignaturePad.setEnabled(!finished);
    }

    /**
     * Method used for creating a {@link TransportDocument}
     *
     * @param view the {@link View} calling the method
     */
    private void updateTransport(View view) {
        DataHandler.instance().runOnNetworkThread(() -> {

            if (transportID == null) {
                if (getActivity() == null)
                    return;
                ((MainActivity) getActivity()).exceptionAlert("Keine Transport ID übergeben!", new IllegalProcessException("Transport can not be edited due to missing transport id!"));
                return;
            }

            boolean valid = true;

            if (getActivity() == null)
                return;

            String tourNumberStr = ((EditText) view.findViewById(R.id.et_tour_number)).getText().toString();

            String startStreetStr = ((EditText) view.findViewById(R.id.et_address_start_street)).getText().toString();
            String startHouseNrStr = ((EditText) view.findViewById(R.id.et_address_start_housenumber)).getText().toString();
            String startZipCodeStr = ((EditText) view.findViewById(R.id.et_address_start_zipcode)).getText().toString();
            String startCityStr = ((EditText) view.findViewById(R.id.et_address_start_city)).getText().toString();

            String endStreetStr = ((EditText) view.findViewById(R.id.et_address_end_street)).getText().toString();
            String endHouseNrStr = ((EditText) view.findViewById(R.id.et_address_end_housenumber)).getText().toString();
            String endZipCodeStr = ((EditText) view.findViewById(R.id.et_address_end_zipcode)).getText().toString();
            String endCityStr = ((EditText) view.findViewById(R.id.et_address_end_city)).getText().toString();

            TypedValue hintColor = new TypedValue();
            TypedValue mistakeColor = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.textColorHint, hintColor, true);
            getActivity().getTheme().resolveAttribute(android.R.attr.colorError, mistakeColor, true);

            if (startStreetStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_street)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_street)).setHintTextColor(hintColor.data));

            if (startHouseNrStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_housenumber)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_housenumber)).setHintTextColor(hintColor.data));

            if (startZipCodeStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_zipcode)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_zipcode)).setHintTextColor(hintColor.data));

            if (startCityStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_city)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_city)).setHintTextColor(hintColor.data));

            if (endStreetStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_street)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_street)).setHintTextColor(hintColor.data));

            if (endHouseNrStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_housenumber)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_housenumber)).setHintTextColor(hintColor.data));

            if (endZipCodeStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_zipcode)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_zipcode)).setHintTextColor(hintColor.data));

            if (endCityStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_city)).setHintTextColor(mistakeColor.data));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_city)).setHintTextColor(hintColor.data));

            if (patientCondition == null) {
                getActivity().runOnUiThread(() -> patientConditionAdapter.setValid(false));
                valid = false;
            } else getActivity().runOnUiThread(() -> patientConditionAdapter.setValid(true));

            if (!valid)
                return;

            TransportDetails transportDetails = null;

            try {
                DataHandler handler = DataHandler.instance();

                COptional<String> tourNumber = tourNumberStr.isBlank() ? COptional.empty() : COptional.of(tourNumberStr);

                Address startAddress = handler.createAddress(startStreetStr, startHouseNrStr, startZipCodeStr, startCityStr, "de");
                Address endAddress = handler.createAddress(endStreetStr, endHouseNrStr, endZipCodeStr, endCityStr, "de");

                Direction direction = ((ToggleButton) view.findViewById(R.id.tb_direction)).isChecked() ? Direction.Return : Direction.Outward;

                PatientCondition patientCondition = this.patientCondition;

                boolean paymentExemption = ((CheckBox) view.findViewById(R.id.cb_payment_exemption)).isChecked();


                transportDetails = handler.getTransportDetailsById(this.transportID);

                if (transportDetails != null){
                    if(patientValidation) {
                        if (validSigning)
                            transportDetails = handler.updateTransportPatientSignature(transportID, transporterSignaturePad.getSignatureBitmap().toString(), new Date(System.currentTimeMillis()));
                    }else{
                        transportDetails = handler.updateTransport(transportID, tourNumber, Reference.to(startAddress.id().value()), Reference.to(endAddress.id().value()), direction, patientCondition, paymentExemption);
                        if (validSigning) {
                            TransportDetails tempTransportDetails = handler.updateTransportTransporterSignature(transportID, transporterSignaturePad.getSignatureBitmap().toString(), new Date(System.currentTimeMillis()));
                            if (tempTransportDetails != null)
                                transportDetails = tempTransportDetails;
                        }
                    }
                } else
                    ((MainActivity) getActivity()).informationAlert("Transport nicht gefunden!", "Der Transport mit der ID \"" + transportID + "\" konnte nicht gefunden werden!");
            } catch (ProcessingException e) {
                if (getActivity() == null)
                    return;
                ((MainActivity) getActivity()).exceptionAlert("Transport konnte nicht bearbeitet werden!", e);
            }
            if (getActivity() == null
                    || transportDetails == null)
                return;

            TransportDetails finalTransportDetails = transportDetails;
            NavController navController = NavHostFragment.findNavController(EditorTransportUpdateFragment.this);

            if(finalTransportDetails.transporterSignature().isPresent() && finalTransportDetails.transporterSignatureDate().isPresent()) {
                if (finalTransportDetails.patientSignature().isPresent() && finalTransportDetails.patientSignatureDate().isPresent())
                    ((MainActivity) getActivity()).informationAlert("Transport wurde bearbeitet!",
                            "Transport mit ID " + finalTransportDetails.id().value() + " wurde erfolgreich bearbeitet und validiert!", ((dialog, which) -> {
                                if (navController.getCurrentDestination() == null
                                        || navController.getCurrentDestination().getId() != R.id.editorTransportUpdateFragment)
                                    return;
                                navController.navigateUp();
                            }));
                else
                    ((MainActivity) getActivity()).choiceAlert("Transport wurde bearbeitet!",
                            "Transport mit ID " + finalTransportDetails.id().value() + " wurde erfolgreich bearbeitet!\n\r" +
                                    "Soll der Transport durch den Patienten validiert werden?", "Nein",
                            (dialog, which) -> getActivity().runOnUiThread(() -> {
                                if (navController.getCurrentDestination() == null
                                        || navController.getCurrentDestination().getId() != R.id.editorTransportUpdateFragment)
                                    return;
                                navController.navigateUp();

                            }),
                            "Ja,validieren",
                            (dialog, which) -> {
                                patientValidation = true;
                                setEditable(false, view);
                            }
                    );

            } else
                ((MainActivity) getActivity()).choiceAlert("Transport wurde bearbeitet!", "Transport mit ID " + finalTransportDetails.id().value()
                                + " wurde erfolgreich bearbeitet! \n\r Soll der Transport validiert werden?",
                        "Nein", (dialog, which) -> getActivity().runOnUiThread(() -> {
                            if (navController.getCurrentDestination() == null
                                    || navController.getCurrentDestination().getId() != R.id.editorTransportUpdateFragment)
                                return;
                            navController.navigateUp();
                        }),
                        "Ja, validieren", (dialog, which) -> {
                            transporterValidation = true;
                            setEditable(false, view);
                        }
                );
        });
    }

    @Override
    public <T> void onItemClick(T obj, int position) {
        if (obj == PatientCondition.class)
            patientCondition = patientConditionAdapter.getItem(position);
    }

}