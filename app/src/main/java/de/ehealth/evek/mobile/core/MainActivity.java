package de.ehealth.evek.mobile.core;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.exception.IllegalProcessException;
import de.ehealth.evek.api.exception.UserNotProvidedException;
import de.ehealth.evek.api.util.Debug;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.databinding.ActivityMainBinding;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.network.DataHandler;
import de.ehealth.evek.mobile.frontend.QRScannerActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;


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
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                setQRScanEnabled(destination.getId() == R.id.mainPageDoctorFragment || destination.getId() == R.id.mainPageUserFragment));
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        setNavigation(false);
        setQRScanEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.btn_logout) {

            choiceAlert("Durch das abmelden werden die aktuellen Transportscheine und Transporte aus der App entfernt und können nur noch über die IDs abgerufen werden!", "Abmelden?",
                    "Nein, angemeldet bleiben!", "Ja, abmelden!", (dialog, var) -> {
                        try{
                            Log.sendMessage("Logging out user...");
                            DataHandler.instance().logout();
                            NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                            runOnUiThread(() -> controller.setGraph(controller.getNavInflater().inflate(R.navigation.nav_graph)));
                            Log.sendMessage("Successfully logged out user!");
                        } catch(IllegalProcessException e){
                            if(!(e.getCause() instanceof UserNotProvidedException))
                                Log.sendException(e);
                        }
                    });

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        try {
            if (navController.getPreviousBackStackEntry() == navController.getBackStackEntry(R.id.loginUserFragment))
                return false;
        }catch(IllegalArgumentException e){
            Debug.sendMessage("loginUserFragment ist nicht im backStack!");
        }
        try {
            if (navController.getPreviousBackStackEntry() == navController.getBackStackEntry(R.id.loadingConnectionFragment))
                return false;
        }catch(IllegalArgumentException e){
            Debug.sendMessage("loadingConnectionFragment ist nicht im backStack!");
        }
        try {
            return NavigationUI.navigateUp(navController, appBarConfiguration)
                    || super.onSupportNavigateUp();
        }catch(Exception e){
            Log.sendException(e);
            return false;
        }
    }

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

    public boolean setQRScanEnabled(boolean elementVisible){
        return setQRScanEnabled(elementVisible, getString(R.string.app_name));
    }

    public boolean setNavigation(boolean elementVisible){
        if(getSupportActionBar() == null)
            return false;
        getSupportActionBar().setDisplayHomeAsUpEnabled(elementVisible);
        return true;
    }

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
        new Thread(() -> {
            try {
                TransportDetails created = DataHandler.instance().tryAssignTransport(result.getContents());
                Log.sendMessage(created.toString());
                NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                if(navController.getCurrentDestination() == null)
                    return;
                if(navController.getCurrentDestination().getId() == R.id.mainPageDoctorFragment)
                    runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportUpdateFragment));
                /*Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                intent.putExtra("key", TransportDetails.class); //Optional parameters
                startActivity(intent);*/
                //TODO EDITOR

            }catch(IllegalProcessException e){
                Log.sendException(e);
                exceptionAlert(e, "Error assigning Transport Provider!");
            }
        }).start();
    });

    public void exceptionAlert(Throwable e, String title){
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(title);
            builder.setMessage(e.getLocalizedMessage());
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
        });
    }

    public void informationAlert(String message, String title) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
        });
    }

    public void choiceAlert(String message, String title, String buttonLeftText, DialogInterface.OnClickListener buttonLeftListener, String buttonRightText, DialogInterface.OnClickListener buttonRightListener) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setNegativeButton(buttonLeftText, buttonLeftListener);
            builder.setPositiveButton(buttonRightText, buttonRightListener);
            builder.show();
        });
    }

    public void choiceAlert(String message, String title, String buttonCancelText, String buttonConfirmText, DialogInterface.OnClickListener buttonConfirmListener) {
        choiceAlert(message, title, buttonCancelText, (dialog, var) -> dialog.dismiss(), buttonConfirmText, buttonConfirmListener);
    }

    public void choiceAlert(String message, String title, String buttonLeftText, DialogInterface.OnClickListener buttonLeftListener,
                            String buttonCenterText, DialogInterface.OnClickListener buttonCenterListener, String buttonRightText, DialogInterface.OnClickListener buttonRightListener) {
        runOnUiThread(() -> {
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