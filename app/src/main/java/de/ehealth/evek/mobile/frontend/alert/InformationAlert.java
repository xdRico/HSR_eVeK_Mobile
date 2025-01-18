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
 * Class used to create an InformationAlert.
 *
 * @extends DialogFragment
 */
public class InformationAlert extends DialogFragment {

    /**
     * Class used to create an InformationAlert.<br>
     * Constructor creating a new InformationAlert. <br>
     * {@link Deprecated} USE InformationAlert.showDialog TO CREATE A NEW InformationAlert!
     */
    @Deprecated
    public InformationAlert() {
    }

    /**
     * Class used to create an ExceptionAlert.<br>
     * Constructor creating a new ExceptionAlert.
     *
     * @param title title for the {@link Dialog} to show
     * @param message message for the {@link Dialog} to show
     * @param confirm text for the confirm {@link Button} to show
     */
    private InformationAlert(String title, String message, String confirm, DialogInterface.OnClickListener buttonConfirmListener) {
        if (AlertHandler.getCurrentDialog() != null)
            AlertHandler.dismiss();

        AlertHandler.setCurrent(new AlertDialog.Builder(ClientMain.instance().getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(confirm, buttonConfirmListener));
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
     * @param message message for the {@link Dialog} to show
     */
    public static void showDialog(FragmentManager fragmentManager, String title, String message) {
        showDialog(fragmentManager,title,message, "OK");
    }

    /**
     * Method used to create and show a new {@link Dialog} with the given properties.
     *
     * @param fragmentManager {@link FragmentManager} to call the {@link Dialog} from
     * @param title title for the {@link Dialog} to show
     * @param message message for the {@link Dialog} to show
     * @param confirm text for the confirm {@link Button} to show
     */
    public static void showDialog(FragmentManager fragmentManager, String title, String message, String confirm) {
        try {
            InformationAlert alert = new InformationAlert(title, message, confirm, (dialog, which) -> AlertHandler.getCurrentDialog().dismiss());
            alert.show(fragmentManager, "information_dialog");
        } catch (NullPointerException ex) {
            Log.sendException(ex);
        }
    }

    /**
     * Method used to create and show a new {@link Dialog} with the given properties.
     *
     * @param fragmentManager {@link FragmentManager} to call the {@link Dialog} from
     * @param title title for the {@link Dialog} to show
     * @param message message for the {@link Dialog} to show
     * @param buttonConfirmListener listener for the confirm {@link Button} to call on click
     */
    public static void showDialog(FragmentManager fragmentManager, String title, String message, DialogInterface.OnClickListener buttonConfirmListener) {
        try {
            InformationAlert alert = new InformationAlert(title, message, "OK", buttonConfirmListener);
            alert.show(fragmentManager, "information_dialog");
        } catch (NullPointerException ex) {
            Log.sendException(ex);
        }
    }

    /**
     * Method used to create and show a new {@link Dialog} with the given properties.
     *
     * @param fragmentManager {@link FragmentManager} to call the {@link Dialog} from
     * @param title title for the {@link Dialog} to show
     * @param message message for the {@link Dialog} to show
     * @param confirm text for the confirm {@link Button} to show
     * @param buttonConfirmListener listener for the confirm {@link Button} to call on click
     */
    public static void showDialog(FragmentManager fragmentManager, String title, String message, String confirm, DialogInterface.OnClickListener buttonConfirmListener) {
        try {
            InformationAlert alert = new InformationAlert(title, message, confirm, buttonConfirmListener);
            alert.show(fragmentManager, "information_dialog");
        } catch (NullPointerException ex) {
            Log.sendException(ex);
        }
    }
}
