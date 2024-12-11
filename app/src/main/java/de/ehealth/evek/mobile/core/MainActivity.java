package de.ehealth.evek.mobile.core;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.ehealth.evek.api.entity.TransportDetails;
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
        ClientMain.instance();

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.fabQrScanner.setOnClickListener(view -> scanQRCode());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        setNavigationElementsVisible(false);
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        if(navController.getPreviousBackStackEntry() == navController.getBackStackEntry(R.id.loginUserFragment)
            || navController.getPreviousBackStackEntry() == navController.getBackStackEntry(R.id.loadingConnectionFragment))
            return false;
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public boolean setNavigationElementsVisible(boolean enable){
        boolean ret = true;
        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(enable);
        else ret = false;
        int visibility = View.INVISIBLE;
        if(enable)
            visibility = View.VISIBLE;
        if(findViewById(R.id.fab_qr_scanner) != null)
            findViewById(R.id.fab_qr_scanner).setVisibility(visibility);
        else ret = false;
        return ret;
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
                if(navController.getCurrentDestination() == null
                        || navController.getCurrentDestination().getId() != R.id.mainPageFragment) return;
                navController.navigate(R.id.action_mainPageFragment_to_editorFragment);
                /*Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                intent.putExtra("key", TransportDetails.class); //Optional parameters
                startActivity(intent);*/
                //TODO EDITOR

            }catch(IllegalArgumentException e){
                Log.sendException(e);
                runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Error assigning Transport Provider!");
                    builder.setMessage(e.getLocalizedMessage());
                    builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
                });
            }

        }).start();



    });
}