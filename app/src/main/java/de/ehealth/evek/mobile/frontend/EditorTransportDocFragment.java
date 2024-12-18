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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;

import de.ehealth.evek.api.entity.InsuranceData;
import de.ehealth.evek.api.entity.Patient;
import de.ehealth.evek.api.entity.ServiceProvider;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.api.entity.User;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.type.Reference;
import de.ehealth.evek.api.type.TransportReason;
import de.ehealth.evek.api.type.TransportationType;
import de.ehealth.evek.api.util.COptional;
import de.ehealth.evek.api.util.Debug;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.network.DataHandler;

public class EditorTransportDocFragment extends Fragment implements RecyclerAdapter.ItemClickListener {

    RecyclerAdapter<TransportReason> transportReasonAdapter;
    RecyclerAdapter<TransportationType> transportationTypeAdapter;
    TransportReason reason = null;
    TransportationType type = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity() != null && getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setNavigationElementsVisible(true);


    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_editor_transport_doc, container, false);

        if(getActivity() != null)
            ((MainActivity) getActivity()).setNavigationElementsVisible(true);


        // data to populate the RecyclerView with
        ArrayList<TransportReason> transportReasons = new ArrayList<>(Arrays.asList(TransportReason.values()));
        // set up the RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.rv_transport_reason);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        transportReasonAdapter = new RecyclerAdapter<>(getActivity(), transportReasons, TransportReason.class);
        transportReasonAdapter.setClickListener(this);
        recyclerView.setAdapter(transportReasonAdapter);


        ArrayList<TransportationType> transportTypes = new ArrayList<>(Arrays.asList(TransportationType.values()));
        // set up the RecyclerView
        recyclerView = view.findViewById(R.id.rv_transportation_type);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        transportationTypeAdapter = new RecyclerAdapter<>(getActivity(), transportTypes, TransportationType.class);
        transportationTypeAdapter.setClickListener(this);
        recyclerView.setAdapter(transportationTypeAdapter);


        view.findViewById(R.id.btn_save_transport_doc).setOnClickListener((v) -> {
            if(getActivity() != null)
                getActivity().runOnUiThread(() ->createTransportDoc(view));
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
                if(c != chars)
                        insuranceNumber.setText(new String (c));
            }
        });
        return view;
    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void createTransportDoc(View view) {
        new Thread(() -> {
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
            Reference<User> signature = Reference.to(DataHandler.instance().getLoginUser().id().value());

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
                startDate = DataHandler.getDate(((EditText) view.findViewById(R.id.et_start_date)).getText().toString());

                //startDate = Date.valueOf(((EditText) view.findViewById(R.id.et_start_date)).getText().toString());
            }catch(Exception e){
                Log.sendException(e);
                valid = false;
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_start_date)).setHintTextColor(Color.argb(255, 255, 100, 100)));
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
                    //endDate = COptional.of(Date.valueOf(((EditText) view.findViewById(R.id.et_end_date)).getText().toString()));
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
            TransportDocument transportDocument = null;
            try {
                transportDocument = DataHandler.instance().createTransportDocument(
                        new TransportDocument.Create(patient, insuranceData, reason, startDate, endDate, weeklyFrequency, serviceProvider, type, info, signature));
            }catch(ProcessingException e){
                if(getActivity() == null)
                    return;
                ((MainActivity) getActivity()).exceptionAlert(e, "Transportschein konnte nicht erstellt werden!");
            }
            if(getActivity() == null
                    || transportDocument == null)
                return;

            ((MainActivity) getActivity()).informationAlert("Transportschein wurde erfolgreich mit ID " + transportDocument.id().value() + " erstellt!", "Transportschein erstellt");
        }).start();
    }

    @Override
    public <T> void onItemClick(T obj, int position) {
        //if(view.getId() == R.id.rv_transport_reason) {
        if (obj == TransportReason.class){
            Debug.sendMessage("Clicked on " + transportReasonAdapter.getItem(position).toString() + " on position " + position);
            reason = transportReasonAdapter.getItem(position);
        //}else if(view.getId() == R.id.rv_transportation_type) {
        }else if(obj == TransportationType.class){
            Debug.sendMessage("Clicked on " + transportationTypeAdapter.getItem(position).toString() + " on position " + position);
            type = transportationTypeAdapter.getItem(position);
        }
    }
}
