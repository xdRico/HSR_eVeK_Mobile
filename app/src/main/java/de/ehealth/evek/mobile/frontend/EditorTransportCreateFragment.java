package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.type.Reference;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.network.DataHandler;

/**
 * Class belonging to the EditorTransportCreate Fragment
 *
 * @extends Fragment
 */
public class EditorTransportCreateFragment extends Fragment {

    String transportDocumentID = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String transportDocID;
        if(getArguments() != null
                && (transportDocID = getArguments().getString("transportDocumentID")) != null
                && !transportDocID.isBlank()){
                this.transportDocumentID = transportDocID;
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_editor_transport_create, container, false);

        EditText etTransportDocumentID = view.findViewById(R.id.et_transport_doc_id);
        if(etTransportDocumentID != null) {
            if(transportDocumentID != null){
                etTransportDocumentID.setText(transportDocumentID);
                etTransportDocumentID.setEnabled(false);
                DataHandler.instance().runOnNetworkThread(() -> {
                        try {
                            TransportDocument doc = DataHandler.instance().getTransportDocumentById(transportDocumentID);
                            if(doc.endDate().isEmpty() && doc.weeklyFrequency().isEmpty()
                                    && getActivity() != null) {
                                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_date)).setText(new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(doc.startDate().getTime())));
                            }
                        } catch (ProcessingException e) {
                            Log.sendException(e);
                        }
                    });
            }
            etTransportDocumentID.addTextChangedListener(new TextWatcher() {
                String old;
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    old = s.toString();
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if(old.equals(s.toString()))
                        return;
                    if(s.length() == 0)
                        return;
                    char[] chars = new char[s.length()];

                    s.getChars(0, s.length(), chars, 0);

                    char[] c = chars;
                    int offset = 0;
                    for(int i = 1; i < s.length(); i++ ){
                        if((i == 8 || i == 13 || i== 18 || i == 23)) {
                            if(chars[i] == '-'){
                                c[i + offset] = chars[i];
                                continue;
                            }
                            char[] cs = new char[c.length + 1];
                            System.arraycopy(c, 0, cs, 0, i);
                            c = cs;
                            c[i + offset] = '-';
                            offset ++;
                            c[i + offset] = chars[i];

                        } else if((chars[i] < 'a' || chars[i] > 'z')
                                && (chars[i] < 'A' || chars[i] > 'Z')
                                && (chars[i] < '0' || chars[i] > '9')){
                            char[] cs = new char[c.length - 1];
                            System.arraycopy(c, 0, cs, 0, i);
                            c = cs;
                            offset--;
                        }else c[i + offset] = chars[i];
                    }
                    if(c != chars)
                        etTransportDocumentID.setText(new String (c));
                }
            });
        }
        view.findViewById(R.id.btn_save_transport).setOnClickListener((v) -> createTransport(view));

        return view;
    }

    /**
     * Method used for creating a Transport with the Information currently inserted
     *
     * @param view The View calling the method
     */
    private void createTransport(View view) {
        DataHandler.instance().runOnNetworkThread(() -> {
            boolean valid = true;

            String transportDocStr = ((EditText) view.findViewById(R.id.et_transport_doc_id)).getText().toString();
            Reference<TransportDocument> transportDoc = Reference.to(transportDocStr);
            Date date = null;

            if (getActivity() == null)
                return;

            TypedValue mistakeColor = new TypedValue();
            getActivity().getTheme().resolveAttribute(android.R.attr.colorError, mistakeColor, true);

            if (transportDocStr.isBlank()) {
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_doc_id)).setHintTextColor(mistakeColor.data));
                valid = false;
            }

            try {
                date = DataHandler.getDate(((EditText) view.findViewById(R.id.et_transport_date)).getText().toString());

            } catch (Exception e) {
                Log.sendException(e);
                valid = false;
                getActivity().runOnUiThread(() -> ((EditText) view.findViewById(R.id.et_transport_date)).setHintTextColor(mistakeColor.data));
            }

            if (!valid)
                return;
            TransportDetails transport = null;
            try {
                transport = DataHandler.instance().createTransport(transportDoc, date);
            } catch (ProcessingException e) {
                if (getActivity() == null)
                    return;
                ((MainActivity) getActivity()).exceptionAlert("Transport konnte nicht erstellt werden!", e);
            }
            if (getActivity() == null
                    || transport == null)
                return;

            TransportDetails finalTransport = transport;
            ((MainActivity) getActivity()).choiceAlert(
                    "Transport wurde erstellt!",
                    "Transport wurde erfolgreich mit ID " + transport.id().value() + " erstellt!\n\r\n\rSoll der Transport einem Transportunternehmen zugewiesen werden?",
                     "Nein",
                    (dialog, which) -> {
                        if (getActivity() != null) {

                            NavController navController = NavHostFragment.findNavController(EditorTransportCreateFragment.this);
                            if (navController.getCurrentDestination() == null
                                    || navController.getCurrentDestination().getId() != R.id.editorTransportCreateFragment){
                                dialog.dismiss();
                                return;
                            }
                            getActivity().runOnUiThread(() -> {
                                navController.navigateUp();
                                if(navController.getCurrentDestination().getId() == R.id.editorTransportDocumentFragment)
                                    navController.navigateUp();
                            });

                        }
                        dialog.dismiss();
                    },
                    "Ja",
                    (dialog, which) -> {
                        if (getActivity() != null) {
                            NavController navController = NavHostFragment.findNavController(EditorTransportCreateFragment.this);
                            if (navController.getCurrentDestination() == null
                                    || navController.getCurrentDestination().getId() != R.id.editorTransportCreateFragment)
                                return;
                            Bundle bundle = new Bundle();
                            bundle.putString("transportID", finalTransport.id().value());
                            navController.navigate(R.id.action_editorTransportCreateFragment_to_assignTransportFragment, bundle);
                        }
                        dialog.dismiss();
                    });
        });
    }
}
