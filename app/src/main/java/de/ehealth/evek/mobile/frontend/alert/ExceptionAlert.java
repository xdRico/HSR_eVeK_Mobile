package de.ehealth.evek.mobile.frontend.alert;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.core.ClientMain;

public class ExceptionAlert extends DialogFragment {

    public ExceptionAlert(){
    }
    private ExceptionAlert(String title, Throwable e) {
        if (AlertHandler.getCurrentDialog() != null)
            AlertHandler.dismiss();
        AlertHandler.setCurrent(new AlertDialog.Builder(ClientMain.instance().getContext())
                .setTitle(title)
                .setMessage(e.getLocalizedMessage())
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss()));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        if(AlertHandler.getCurrentDialog() != null)
            AlertHandler.rebuild();
        assert AlertHandler.getCurrentDialog() != null;
        return AlertHandler.getCurrentDialog();
    }

    public static void showDialog(FragmentManager fragmentManager, String title, Throwable e) {
        try {
            ExceptionAlert alert = new ExceptionAlert(title, e);
            alert.show(fragmentManager, "exception_dialog");
        } catch (NullPointerException ex) {
            Log.sendException(ex);
        }
    }
}
