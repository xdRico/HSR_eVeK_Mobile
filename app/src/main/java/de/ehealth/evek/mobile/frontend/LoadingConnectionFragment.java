package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import de.ehealth.evek.api.util.Log;

import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.exception.UserLoggedInThrowable;
import de.ehealth.evek.mobile.network.DataHandler;
import de.ehealth.evek.mobile.network.IsInitializedListener;
import de.ehealth.evek.mobile.network.IsLoggedInListener;
import de.ehealth.evek.mobile.R;

public class LoadingConnectionFragment extends Fragment implements IsInitializedListener, IsLoggedInListener {
    private DataHandler handler;
    private TextView connectCounter;
    private NavController navController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() != null && getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setNavigation(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        handler = DataHandler.instance();
        handler.addIsInitializedListener(this);
        handler.initServerConnection();
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
            if(getActivity() == null){
                Log.sendException(new RuntimeException("getActivity() is null!"));
                return;
            }
            TextView state = getActivity().findViewById(R.id.tv_loadConnection);
            getActivity().runOnUiThread(() -> state.setText(getString(R.string.content_fragment_loading_connection_tv_init_connection_auto_login)));
            handler.addIsLoggedInListener(this);
            handler.runOnNetworkThread(() -> {
                if (!handler.tryLoginFromStoredCredentials())
                    getActivity().runOnUiThread(() -> navController.navigate(R.id.action_loadingConnectionFragment_to_loginUserFragment));
            });
        } else {
            setConnectCounter();
        }
    }

    private void setConnectCounter(){
        if(connectCounter == null)
            return;
        DataHandler.ConnectionCounter counter = DataHandler.instance().getConnectionCounter();
        if(getActivity() == null)
            Log.sendException(new RuntimeException("getActivity() is null!"));
        getActivity().runOnUiThread(() -> connectCounter.setText(String.format(Locale.getDefault(),
                "(%d/%d)",
                counter.timesTriedToConnect(),
                counter.timesToTryConnect())));
    }

    @Override
    public void onLoginStateChanged(Throwable loginState) {
        handler.removeIsLoggedInListener(this);
        if(!isAdded())
            return;
        if(!(loginState instanceof UserLoggedInThrowable)) {
            Log.sendMessage("User could not be logged in from remembered data!");
            if(getActivity() == null)
                return;
            getActivity().runOnUiThread(() -> navController.navigate(R.id.action_loadingConnectionFragment_to_loginUserFragment));
            return;
        }

        NavController navController = NavHostFragment.findNavController(LoadingConnectionFragment.this);
        NavGraph newNavGraph = switch(DataHandler.instance().getLoginUser().role()) {
            case HealthcareDoctor, TransportDoctor, SuperUser ->
                    navController.getNavInflater().inflate(R.navigation.nav_graph_doctor);
            case TransportUser ->
                    navController.getNavInflater().inflate(R.navigation.nav_graph_user);
            default -> throw new RuntimeException("Invalid user Role - how did we get here??");
        };
        if(getActivity() == null) return;
        getActivity().runOnUiThread(() -> navController.setGraph(newNavGraph));
    }
}