package de.ehealth.evek.mobile.frontend.alert;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

/**
 * Class used for handling Alerts on frontend.
 */
class AlertHandler {

    private static AlertDialog.Builder builder = null;
    private static Dialog dialog = null;

    /**
     * Method used for setting the currently active {@link Dialog} from an {@link AlertDialog.Builder}.
     *
     * @param builder - the {@link AlertDialog.Builder} to create an Alert from
     */
    static void setCurrent(AlertDialog.Builder builder){
        AlertHandler.builder = builder;
        AlertHandler.dialog = builder.create();
        AlertHandler.dialog.show();
    }

    /**
     * Method used for getting the currently active Alerts instance as Dialog.
     *
     * @return {@link Dialog} - the currently active {@link Dialog}
     */
    static Dialog getCurrentDialog(){
        return dialog;
    }

    /**
     * Method used to rebuild the currently active {@link Dialog} from the set {@link AlertDialog.Builder}.
     */
    static void rebuild(){
        if(builder == null)
            return;
        if(dialog != null)
            dialog.dismiss();
        dialog = builder.create();
        dialog.show();
    }

    /**
     * Method used to dismiss the currently active {@link Dialog} and clear it from {@link AlertHandler}.
     */
    static void dismiss(){
        if(dialog != null)
            dialog.dismiss();
        dialog = null;
        builder = null;
    }
}
