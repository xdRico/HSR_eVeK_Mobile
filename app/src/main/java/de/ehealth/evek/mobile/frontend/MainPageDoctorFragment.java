package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.ehealth.evek.api.type.UserRole;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.network.DataHandler;

public class MainPageDoctorFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_page_doctor, container, false);

        UserRole role = DataHandler.instance().getLoginUser().role();
        Button btnEditTransport = view.findViewById(R.id.btn_transport_edit);

        if(role == UserRole.TransportDoctor
                || role == UserRole.SuperUser){
            btnEditTransport.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.btn_transport_doc_create).setOnClickListener((l) -> {
            NavController navController = NavHostFragment.findNavController(MainPageDoctorFragment.this);
            if(navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.mainPageDoctorFragment) return;
            if(getActivity() == null) return;
            getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportDocFragment));
        });

        view.findViewById(R.id.btn_transport_create).setOnClickListener((l) -> {
            NavController navController = NavHostFragment.findNavController(MainPageDoctorFragment.this);
            if(navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.mainPageDoctorFragment) return;
            if(getActivity() == null) return;
            getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportCreateFragment));
        });
        return view;
    }
}