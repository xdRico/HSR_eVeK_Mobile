package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import de.ehealth.evek.api.util.Log;

import de.ehealth.evek.mobile.core.ClientMain;
import de.ehealth.evek.mobile.network.IsInitializedListener;
import de.ehealth.evek.mobile.network.ServerConnection;
import de.ehealth.evek.mobile.R;

public class LoadingConnectionFragment extends Fragment implements IsInitializedListener {
    private ClientMain main;
    private TextView connectCounter;
    private NavController navController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        main = ClientMain.instance();
        ServerConnection connection = main.getServerConnection();
        connection.addIsInitializedListener(this);
        main.initServerConnection();
        View view = inflater.inflate(R.layout.fragment_loading_connection, container, false);
        connectCounter = view.findViewById(R.id.tv_loadConnection_count);
        setConnectCounter();
        navController = NavHostFragment.findNavController(LoadingConnectionFragment.this);
        return view;
    }

    @Override
    public void onInitializedStateChanged(boolean isInitialized) {
        if(navController == null)
            navController = NavHostFragment.findNavController(LoadingConnectionFragment.this);
        if(navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.loadingConnectionFragment) return;
        if(isInitialized) {
            if(getActivity() == null)
                Log.sendException(new RuntimeException("getActivity() is null!"));
            getActivity().runOnUiThread(() -> navController.navigate(R.id.action_loadingConnectionFragment_to_loginUserFragment));
        } else {
            setConnectCounter();
        }
    }

    private void setConnectCounter(){
        if(connectCounter == null)
            return;
        ServerConnection connection = main.getServerConnection();
        if(getActivity() == null)
            Log.sendException(new RuntimeException("getActivity() is null!"));
        getActivity().runOnUiThread(() -> connectCounter.setText(String.format(Locale.getDefault(),
                "(%d/%d)",
                connection.getTimesTriedToConnect(),
                connection.getTimesToTryConnect())));
    }
}