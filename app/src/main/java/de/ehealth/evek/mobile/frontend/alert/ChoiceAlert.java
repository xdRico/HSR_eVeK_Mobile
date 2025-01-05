package de.ehealth.evek.mobile.frontend.alert;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.core.ClientMain;

public class ChoiceAlert extends DialogFragment {

    public ChoiceAlert() {
    }

    private ChoiceAlert(String title, String message,
                        String buttonLeftText, DialogInterface.OnClickListener buttonLeftListener,
                        String buttonRightText, DialogInterface.OnClickListener buttonRightListener){

        if(AlertHandler.getCurrentDialog() != null)
            AlertHandler.dismiss();

        AlertHandler.setCurrent(new AlertDialog.Builder(ClientMain.instance().getContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(buttonLeftText,buttonLeftListener)
                .setPositiveButton(buttonRightText,buttonRightListener)
                .setOnDismissListener((dialog) -> AlertHandler.dismiss()));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        setRetainInstance(true);
        if(!AlertHandler.getCurrentDialog().isShowing())
            AlertHandler.rebuild();
        assert AlertHandler.getCurrentDialog() != null;
        return AlertHandler.getCurrentDialog();
    }
    public static void showDialog(FragmentManager fragmentManager, String title, String message,
                                  String buttonLeftText, DialogInterface.OnClickListener buttonLeftListener,
                                  String buttonRightText, DialogInterface.OnClickListener buttonRightListener) {
        try {
            ChoiceAlert alert = new ChoiceAlert(title, message, buttonLeftText, buttonLeftListener, buttonRightText, buttonRightListener);
            alert.show(fragmentManager, "exception_dialog");
        } catch (NullPointerException ex) {
            Log.sendException(ex);
        }
    }
}
