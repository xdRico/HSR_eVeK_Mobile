package de.ehealth.evek.mobile.frontend.alert;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.core.ClientMain;

/**
 * Class used to create a ChoiceAlert with two custom {@link Button}'s.
 *
 * @extends DialogFragment
 */
public class ChoiceAlert extends DialogFragment {

    /**
     * Class used to create a ChoiceAlert with two custom {@link Button}'s.<br>
     * Constructor creating a new ChoiceDialog. <br>
     * {@link Deprecated} USE ChoiceAlert.showDialog TO CREATE A NEW ChoiceAlert!
     */
    @Deprecated
    public ChoiceAlert() {
    }

    /**
     * Class used to create a ChoiceAlert with two custom {@link Button}'s.<br>
     * Constructor creating a new ChoiceDialog.
     *
     * @param title title for the {@link Dialog} to show
     * @param message message for the {@link Dialog} to show
     * @param buttonLeftText text for the left {@link Button} to show
     * @param buttonLeftListener listener for the left {@link Button} to call on click
     * @param buttonRightText text for the right {@link Button} to show
     * @param buttonRightListener listener for the right {@link Button} to call on click
     */
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
        //noinspection deprecation
        setRetainInstance(true);
        if(!AlertHandler.getCurrentDialog().isShowing())
            AlertHandler.rebuild();
        assert AlertHandler.getCurrentDialog() != null;
        return AlertHandler.getCurrentDialog();
    }

    /**
     * Method used to create and show a new {@link Dialog} with the given properties.
     *
     * @param fragmentManager {@link FragmentManager} to call the {@link Dialog} from
     * @param title title for the {@link Dialog} to show
     * @param message message for the {@link Dialog} to show
     * @param buttonLeftText text for the left {@link Button} to show
     * @param buttonLeftListener listener for the left {@link Button} to call on click
     * @param buttonRightText text for the right {@link Button} to show
     * @param buttonRightListener listener for the right {@link Button} to call on click
     */
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
