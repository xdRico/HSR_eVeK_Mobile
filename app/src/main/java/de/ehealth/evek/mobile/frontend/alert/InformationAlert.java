package de.ehealth.evek.mobile.frontend.alert;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.core.ClientMain;

public class InformationAlert extends DialogFragment {

    public InformationAlert() {
    }

    private InformationAlert(String title, String message, String confirm) {
        if (AlertHandler.getCurrentDialog() != null)
            AlertHandler.dismiss();

        AlertHandler.setCurrent(new AlertDialog.Builder(ClientMain.instance().getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(confirm, (dialog, which) -> AlertHandler.getCurrentDialog().dismiss()));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        if(AlertHandler.getCurrentDialog() != null)
            AlertHandler.rebuild();
        assert AlertHandler.getCurrentDialog() != null;
        return AlertHandler.getCurrentDialog();
    }

    public static void showDialog(FragmentManager fragmentManager, String title, String message) {
        showDialog(fragmentManager,title,message, "OK");
    }

    public static void showDialog(FragmentManager fragmentManager, String title, String message, String confirm) {
        try {
            InformationAlert alert = new InformationAlert(title, message, confirm);
            alert.show(fragmentManager, "exception_dialog");
        } catch (NullPointerException ex) {
            Log.sendException(ex);
        }
    }
}
