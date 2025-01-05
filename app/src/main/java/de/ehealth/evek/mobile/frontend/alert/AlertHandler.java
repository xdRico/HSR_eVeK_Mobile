package de.ehealth.evek.mobile.frontend.alert;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;

class AlertHandler {

    private static AlertDialog.Builder builder = null;
    private static Dialog dialog = null;


    static void setCurrent(AlertDialog.Builder builder){
        AlertHandler.builder = builder;
        AlertHandler.dialog = builder.create();
        AlertHandler.dialog.show();
    }

    static Dialog getCurrentDialog(){
        return dialog;
    }

    static void rebuild(){
        if(builder == null)
            return;
        if(dialog != null)
            dialog.dismiss();
        dialog = builder.create();
        dialog.show();
    }
    static void dismiss(){
        if(dialog != null)
            dialog.dismiss();
        dialog = null;
        builder = null;
    }
}
