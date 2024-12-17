package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;

public class EditorTransportDocFragment extends Fragment {

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
        if(getActivity() != null)
            ((MainActivity) getActivity()).setNavigationElementsVisible(true);
        return inflater.inflate(R.layout.fragment_editor_transport_doc, container, false);
    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private createTransportDoc(){
        new TransportDocument.Create(patient, insuranceData, transportReason, startDate, endDate, weeklyFrequency, serviceProvider, transportType, info, signature)
    }
}
