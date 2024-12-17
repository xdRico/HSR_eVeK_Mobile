package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;

public class MainPageFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(getActivity() != null)
            ((MainActivity) getActivity()).setNavigationElementsVisible(true);
        return inflater.inflate(R.layout.fragment_main_page2, container, false);
    }
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(v ->
                NavHostFragment.findNavController(MainPageFragment.this)
                        .navigate(R.id.action_mainPageFragment_to_SecondFragment)
        );
    }
}
