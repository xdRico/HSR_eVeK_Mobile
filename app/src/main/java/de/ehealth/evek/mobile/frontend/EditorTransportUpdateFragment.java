package de.ehealth.evek.mobile.frontend;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private boolean validSigning = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String transportID;
        if(getArguments() != null
                && (transportID = getArguments().getString("transportID")) != null
                && !transportID.isBlank())
            this.transportID = new Id<>(transportID);
    }

    //TODO implement frontend for patient data
    //TODO implement frontend for validation of data


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

        transporterSignaturePad.setOnSignedListener(new SignedListener() {

            @Override
            public void onSigning() {
            }

            @Override
            public void onStartSigning() {
                //validSigning = true;
            }

            @Override
            public void onClear() {
                validSigning = false;
            }

            @Override
            public void onSigned() {
                validSigning = true;
            }
        });

        view.findViewById(R.id.ib_delete_signature_transporter).setOnClickListener((v) -> transporterSignaturePad.clear());

        //set Active Transport, if given
        if(transportID == null)
            return view;
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            try{
                TransportDetails transport = handler.getTransportDetailsById(transportID.value());

                if(transport == null)
                    throw new IllegalProcessException("Transport with ID " + transportID.value() + " not found!");

                COptional<Address> startAddress = transport.startAddress().isPresent() ? COptional.of(handler.getAddressById(transport.startAddress().get().id())) : COptional.empty();
                COptional<Address> endAddress = transport.endAddress().isPresent() ? COptional.of(handler.getAddressById(transport.endAddress().get().id())) : COptional.empty();

                if(getActivity() == null)
                    return;

                getActivity().runOnUiThread(() -> {

                    ((EditText) view.findViewById(R.id.et_transport_date)).setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(transport.transportDate().getTime()));

                    if(transport.tourNumber().isPresent())
                        ((EditText) view.findViewById(R.id.et_tour_number)).setText(transport.tourNumber().get());

                    if(transport.direction().isPresent())
                        ((ToggleButton) view.findViewById(R.id.tb_direction)).setChecked(transport.direction().get() == Direction.Outward);

                    if(startAddress.isPresent()){
                        ((EditText) view.findViewById(R.id.et_address_start_street)).setText(startAddress.get().streetName());
                        ((EditText) view.findViewById(R.id.et_address_start_housenumber)).setText(startAddress.get().houseNumber());
                        ((EditText) view.findViewById(R.id.et_address_start_zipcode)).setText(startAddress.get().postCode());
                        ((EditText) view.findViewById(R.id.et_address_start_city)).setText(startAddress.get().city());
                        //TODO country
                    }

                    if(endAddress.isPresent()){
                        ((EditText) view.findViewById(R.id.et_address_end_street)).setText(endAddress.get().streetName());
                        ((EditText) view.findViewById(R.id.et_address_end_housenumber)).setText(endAddress.get().houseNumber());
                        ((EditText) view.findViewById(R.id.et_address_end_zipcode)).setText(endAddress.get().postCode());
                        ((EditText) view.findViewById(R.id.et_address_end_city)).setText(endAddress.get().city());
                        //TODO country
                    }

                    if(transport.patientCondition().isPresent())
                        patientConditionAdapter.setActiveItem(transport.patientCondition().get());

                    if(transport.paymentExemption().isPresent())
                        ((CheckBox) view.findViewById(R.id.cb_payment_exemption)).setChecked(transport.paymentExemption().get());

                    if(transport.direction().isPresent()
                            && startAddress.isPresent()
                            && endAddress.isPresent()
                            && transport.patientCondition().isPresent()) {
                        setEditable(false, view);
                        if (transport.transporterSignatureDate().isPresent()
                                && transport.transporterSignature().isPresent()) {
                            view.findViewById(R.id.constraint_edit).setVisibility(View.GONE);
                            view.findViewById(R.id.constraint_edit).setEnabled(false);

                        }
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
        view.findViewById(R.id.et_tour_number).setEnabled(editable);
        view.findViewById(R.id.tb_direction).setEnabled(editable);
        view.findViewById(R.id.et_address_start_street).setEnabled(editable);
        view.findViewById(R.id.et_address_start_housenumber).setEnabled(editable);
        view.findViewById(R.id.et_address_start_zipcode).setEnabled(editable);
        view.findViewById(R.id.et_address_start_city).setEnabled(editable);
        view.findViewById(R.id.et_address_end_street).setEnabled(editable);
        view.findViewById(R.id.et_address_end_housenumber).setEnabled(editable);
        view.findViewById(R.id.et_address_end_zipcode).setEnabled(editable);
        view.findViewById(R.id.et_address_end_city).setEnabled(editable);
        view.findViewById(R.id.cb_payment_exemption).setEnabled(editable);
        view.findViewById(R.id.constraint_save).setVisibility(editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.constraint_save).setEnabled(editable);
        view.findViewById(R.id.constraint_edit).setVisibility(!editable ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.constraint_edit).setEnabled(!editable);
        patientConditionAdapter.setEditable(editable);
        transporterSignaturePad.setEnabled(editable);
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

            if (startStreetStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_street)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_street)).setHintTextColor(Color.argb(255, 0, 0, 0)));

            if (startHouseNrStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_housenumber)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_housenumber)).setHintTextColor(Color.argb(255, 0, 0, 0)));

            if (startZipCodeStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_zipcode)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_zipcode)).setHintTextColor(Color.argb(255, 0, 0, 0)));

            if (startCityStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_city)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_start_city)).setHintTextColor(Color.argb(255, 0, 0, 0)));

            if (endStreetStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_street)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_street)).setHintTextColor(Color.argb(255, 0, 0, 0)));

            if (endHouseNrStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_housenumber)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_housenumber)).setHintTextColor(Color.argb(255, 0, 0, 0)));

            if (endZipCodeStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_zipcode)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_zipcode)).setHintTextColor(Color.argb(255, 0, 0, 0)));

            if (endCityStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_city)).setHintTextColor(Color.argb(255, 255, 100, 100)));
                valid = false;
            } else
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_address_end_city)).setHintTextColor(Color.argb(255, 0, 0, 0)));

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
                    transportDetails = handler.updateTransport(transportID, tourNumber, Reference.to(startAddress.id().value()), Reference.to(endAddress.id().value()), direction, patientCondition, paymentExemption);
                    if(validSigning){
                        TransportDetails tempTransportDetails = handler.updateTransportTransporterSignature(transportID, transporterSignaturePad.getSignatureBitmap().toString(), new Date(System.currentTimeMillis()));
                        if(tempTransportDetails != null)
                            transportDetails = tempTransportDetails;
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

            if(finalTransportDetails.transporterSignature().isPresent() && finalTransportDetails.transporterSignatureDate().isPresent())
                ((MainActivity) getActivity()).informationAlert("Transport wurde bearbeitet!",
                        "Transport mit ID " + finalTransportDetails.id().value() + " wurde erfolgreich bearbeitet!",
                        (dialog, which) -> getActivity().runOnUiThread(() -> {
                            if (navController.getCurrentDestination() == null
                                    || navController.getCurrentDestination().getId() != R.id.editorTransportUpdateFragment)
                                return;
                            navController.navigateUp();
                        })
                );

            else
                ((MainActivity) getActivity()).choiceAlert("Transport wurde bearbeitet!", "Transport mit ID " + finalTransportDetails.id().value()
                                + " wurde erfolgreich bearbeitet! \n\r Soll der Transport vom Patienten validiert werden?",
                        "Nein", (dialog, which) -> getActivity().runOnUiThread(() -> {
                            if (navController.getCurrentDestination() == null
                                    || navController.getCurrentDestination().getId() != R.id.editorTransportUpdateFragment)
                                return;
                            navController.navigateUp();
                        }),
                        "Ja, validieren", (dialog, which) -> getActivity().runOnUiThread(() -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("transportID", finalTransportDetails.id().value());
                            navController.navigate(R.id.action_editorTransportUpdateFragment_to_patientSignatureFragment, bundle);
                        })
                );
        });
    }

    @Override
    public <T> void onItemClick(T obj, int position) {
        if (obj == PatientCondition.class)
            patientCondition = patientConditionAdapter.getItem(position);

            //Debug.sendMessage("Clicked on " + transportReasonAdapter.getItem(position).toString() + " on position " + position);
    }

}