package de.ehealth.evek.mobile.frontend.alert;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.core.ClientMain;

/**
 * Class used to create an ExceptionAlert.
 *
 * @extends DialogFragment
 */
public class ExceptionAlert extends DialogFragment {

    /**
     * Class used to create an ExceptionAlert.<br>
     * Constructor creating a new ExceptionAlert. <br>
     * {@link Deprecated} USE ExceptionAlert.showDialog TO CREATE A NEW ExceptionAlert!
     */
    @Deprecated
    public ExceptionAlert(){
    }

    /**
     * Class used to create an ExceptionAlert.<br>
     * Constructor creating a new ExceptionAlert.
     *
     * @param title title for the {@link Dialog} to show
     * @param e {@link Throwable} for the {@link Dialog} to show
     */
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

    /**
     * Method used to create and show a new {@link Dialog} with the given properties.
     *
     * @param fragmentManager {@link FragmentManager} to call the {@link Dialog} from
     * @param title title for the {@link Dialog} to show
     * @param e {@link Throwable} for the {@link Dialog} to show
     */
    public static void showDialog(FragmentManager fragmentManager, String title, Throwable e) {
        try {
            ExceptionAlert alert = new ExceptionAlert(title, e);
            alert.show(fragmentManager, "exception_dialog");
        } catch (NullPointerException ex) {
            Log.sendException(ex);
        }
    }
}
