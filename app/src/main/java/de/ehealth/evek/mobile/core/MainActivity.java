package de.ehealth.evek.mobile.core;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.exception.UserNotProvidedException;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.databinding.ActivityMainBinding;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.frontend.alert.ChoiceAlert;
import de.ehealth.evek.mobile.frontend.alert.ExceptionAlert;
import de.ehealth.evek.mobile.frontend.alert.InformationAlert;
import de.ehealth.evek.mobile.network.DataHandler;
import de.ehealth.evek.mobile.frontend.QRScannerActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/**
 * Class used as main entry point of the application, setting up the basic action
 */
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ClientMain.instance(this);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.fabQrScanner.setOnClickListener(view -> scanQRCode());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        Bundle navState;
        String navGraph;
        if (savedInstanceState != null
        && (navState = savedInstanceState.getBundle("nav_state")) != null
        && (navGraph = savedInstanceState.getString("nav_graph")) != null){
            navController.restoreState(navState);
            //DEBUG Log.sendMessage(String.format("%s ?= %s", navGraph, "nav_graph_doctor"));
            if(navGraph.equalsIgnoreCase("nav_graph_doctor"))
                navController.setGraph(R.navigation.nav_graph_doctor);
            else if(navGraph.equalsIgnoreCase("nav_graph_user"))
                navController.setGraph(R.navigation.nav_graph_user);
            else
                navController.setGraph(R.navigation.nav_graph);

        } else navController.setGraph(R.navigation.nav_graph);


        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            setQRScanEnabled(destination.getId() == R.id.mainPageDoctorFragment || destination.getId() == R.id.mainPageUserFragment);
            setNavigation(destination.getId() == R.id.loginUserFragment || destination.getId() == R.id.loadingConnectionFragment);
        });
        try {
            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        }catch (IllegalStateException e){
            Log.sendException(e);
        }
        setQRScanEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Navigation-Controller abrufen
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Aktuelles Ziel speichern
        Bundle navState = navController.saveState();
        if (navState != null) {
            outState.putBundle("nav_state", navState);
            String navGraphName;
            int graphId = navController.getGraph().getId();
            if (graphId == R.id.nav_graph_doctor) {
                navGraphName = "nav_graph_doctor";
            } else if (graphId == R.id.nav_graph_user) {
                navGraphName = "nav_graph_user";
            } else {
                navGraphName = "nav_graph";
            }
            outState.putString("nav_graph", navGraphName);

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.btn_logout) {

            choiceAlert("Abmelden?","Durch das abmelden werden die aktuellen Transportscheine und Transporte aus der App entfernt und können nur noch über die IDs abgerufen werden!",
                    "Nein, angemeldet bleiben!", "Ja, abmelden!", (dialog, var) -> {
                        DataHandler handler = DataHandler.instance();
                        handler.runOnNetworkThread(() -> {
                            try {
                                Log.sendMessage("Logging out user...");
                                handler.logout();
                                NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                                runOnUiThread(() -> controller.setGraph(controller.getNavInflater().inflate(R.navigation.nav_graph)));
                                Log.sendMessage("Successfully logged out user!");
                            } catch (IllegalProcessException e) {
                                if(!(e.getCause() instanceof UserNotProvidedException))
                                    Log.sendException(e);
                            }
                        });
                    });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        if(navController.getPreviousBackStackEntry() != null
        && navController.getCurrentDestination() != null){

            int previousId = navController.getPreviousBackStackEntry().getDestination().getId();

            if(navController.getGraph().getId() == R.id.nav_graph){
                if (previousId == R.id.loginUserFragment ||
                        previousId == R.id.loadingConnectionFragment)
                    return false;

            }else if(navController.getGraph().getId() == R.id.nav_graph_doctor){
                int currentId = navController.getCurrentDestination().getId();

                while(currentId != R.id.mainPageDoctorFragment)
                    try{
                        navController.navigateUp();
                        currentId = navController.getCurrentDestination().getId();
                    }catch(Exception e){
                        break;
                    }
            }
        }
        try {
            return NavigationUI.navigateUp(navController, appBarConfiguration)
                    || super.onSupportNavigateUp();
        }catch(Exception e){
            Log.sendException(e);
            return false;
        }
    }

    /**
     * Method to set the Button for Scanning QR-Codes enabled or disabled and setting the title for the NavigationBar
     *
     * @param elementVisible boolean if the qr scan is enabled
     * @param title the title to set for the page
     *
     * @return boolean - if the action could be performed
     */
    public boolean setQRScanEnabled(boolean elementVisible, String title){
        boolean ret = true;
        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle(title);
        } else ret = false;
        if(findViewById(R.id.fab_qr_scanner) != null)
            findViewById(R.id.fab_qr_scanner).setVisibility(elementVisible ? View.VISIBLE : View.INVISIBLE);
        else ret = false;
        return ret;
    }

    /**
     * Method to set the Button for Scanning QR-Codes enabled or disabled
     *
     * @param elementVisible boolean if the qr scan is enabled
     *
     * @return boolean if the action could be performed
     */
    public boolean setQRScanEnabled(boolean elementVisible){
        return setQRScanEnabled(elementVisible, getString(R.string.app_name));
    }

    /**
     * Method to set the Button for Navigation enabled or disabled
     *
     * @param elementVisible boolean if the navigation element is enabled
     *
     * @return boolean if the action could be performed
     */
    public boolean setNavigation(boolean elementVisible){
        if(getSupportActionBar() == null)
            return false;
        getSupportActionBar().setDisplayHomeAsUpEnabled(elementVisible);
        return true;
    }

    /**
     * Method to open the qr scanner as new foreground action
     */
    private void scanQRCode(){
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(false);
        options.setOrientationLocked(false);
        options.setCaptureActivity(QRScannerActivity.class);

        barLauncher.launch(options);
    }


    final ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(),  result ->
    {
        if(result.getContents() == null)
            return;
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            try {
                TransportDetails created = handler.tryAssignTransport(result.getContents());
                Log.sendMessage(created.toString());
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                if(navController.getCurrentDestination() == null)
                    return;
                if(navController.getCurrentDestination().getId() == R.id.mainPageDoctorFragment)
                    runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportUpdateFragment));

            }catch(IllegalProcessException e){
                Log.sendException(e);
                exceptionAlert("Error assigning Transport Provider!", e);
            }
        });
    });

    /**
     * Method to create a new Alert displaying an exception
     *
     * @param title the title for the Alert
     * @param e the exception to be displayed
     */
    public void exceptionAlert(String title, Throwable e){
        runOnUiThread(() -> ExceptionAlert.showDialog(getSupportFragmentManager(), title, e));
    }

    /**
     * Method to create a new Alert displaying a message
     *
     * @param title the title for the Alert
     * @param message the String to be displayed
     */
    public void informationAlert(String title, String message) {
        runOnUiThread(() -> InformationAlert.showDialog(getSupportFragmentManager(), title, message));
    }

    /**
     * Method to create a new Alert displaying a message
     *
     * @param title the title for the Alert
     * @param message the String to be displayed
     * @param confirm the Message to show on the confirm button
     */
    public void informationAlert(String title, String message, String confirm) {
        runOnUiThread(() -> InformationAlert.showDialog(getSupportFragmentManager(), title, message, confirm));
    }

    /**
     * Method to create a new Alert displaying a message and containing a cancel button and a custom button
     *
     * @param title the title for the Alert
     * @param message the String to be displayed
     * @param buttonCancelText the text to set for the cancel Button
     * @param buttonConfirmText the text to set for the confirm Button
     * @param buttonConfirmListener the Listener to be performed on click of the right (confirm) Button
     */
    public void choiceAlert(String title, String message, String buttonCancelText, String buttonConfirmText, DialogInterface.OnClickListener buttonConfirmListener) {
        choiceAlert(title, message, buttonCancelText, (dialog, var) -> dialog.dismiss(), buttonConfirmText, buttonConfirmListener);
    }

    /**
     * Method to create a new Alert displaying a message and containing two custom buttons
     *
     * @param title the title for the Alert
     * @param message the String to be displayed
     * @param buttonLeftText the text to set for the left Button
     * @param buttonLeftListener the Listener to be performed on click of the left Button
     * @param buttonRightText the text to set for the right Button
     * @param buttonRightListener the Listener to be performed on click of the right Button
     */
    public void choiceAlert(String title, String message, String buttonLeftText, DialogInterface.OnClickListener buttonLeftListener, String buttonRightText, DialogInterface.OnClickListener buttonRightListener) {
        runOnUiThread(() -> ChoiceAlert.showDialog(getSupportFragmentManager(), title, message, buttonLeftText, buttonLeftListener, buttonRightText, buttonRightListener));
    }

    /**
     * Method to create a new Alert displaying a message and containing three custom buttons
     *
     * @param title the title for the Alert
     * @param message the String to be displayed
     * @param buttonLeftText the text to set for the left Button
     * @param buttonLeftListener the Listener to be performed on click of the left Button
     * @param buttonCenterText the text to set for the center Button
     * @param buttonCenterListener the Listener to be performed on click of the center Button
     * @param buttonRightText the text to set for the right Button
     * @param buttonRightListener the Listener to be performed on click of the right Button
     */
    public void choiceAlert(String title, String message, String buttonLeftText, DialogInterface.OnClickListener buttonLeftListener,
                            String buttonCenterText, DialogInterface.OnClickListener buttonCenterListener, String buttonRightText, DialogInterface.OnClickListener buttonRightListener) {
        runOnUiThread(() -> {
            //TODO
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setNegativeButton(buttonLeftText, buttonLeftListener);
            builder.setNeutralButton(buttonCenterText, buttonCenterListener);
            builder.setPositiveButton(buttonRightText, buttonRightListener);
            builder.show();
        });
    }
}