package de.ehealth.evek.mobile.frontend;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.type.UserRole;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.network.DataHandler;

/**
 * Class belonging to the AssignTransport Fragment
 *
 * @extends Fragment
 */
public class AssignTransportFragment extends Fragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_assign_transport, container, false);
        Bitmap qrCode;
        String transportID;
        if(getArguments() == null
                || (transportID = getArguments().getString("transportID")) == null
                || transportID.isBlank()
                || (qrCode = generateQRCode(transportID)) == null) {
            NavController navController = NavHostFragment.findNavController(AssignTransportFragment.this);
            if(navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.assignTransportFragment) return view;
            if(getActivity() == null)
                return view;
            getActivity().runOnUiThread(navController::navigateUp);
            ((MainActivity) getActivity()).informationAlert("QR-Code konnte nicht erstellt werden!",
                    "Ungültiger Vorgang!");
            return view;
        }

        ImageView ivQRCode = view.findViewById(R.id.iv_qr_code);
        ivQRCode.setImageBitmap(qrCode);

        Button btnSelfAssign = view.findViewById(R.id.btn_self_assign);
        DataHandler handler = DataHandler.instance();
        UserRole role = handler.getUser().role();
        if(role == UserRole.TransportDoctor || role == UserRole.SuperUser) {
            btnSelfAssign.setEnabled(true);
            btnSelfAssign.setVisibility(View.VISIBLE);
            btnSelfAssign.setOnClickListener((v) -> handler.runOnNetworkThread(() -> {
                try {
                    handler.tryAssignTransport(transportID);
                    if (getActivity() == null)
                        return;
                    ((MainActivity) getActivity()).choiceAlert("Transport wurde zugewiesen!","Transport wurde erfolgreich zugewiesen!\n\r\n\rSoll der Transport bearbeitet werden?",
                             "Nein",
                            (dialog, which) -> {
                                if (getActivity() != null) {
                                    NavController navController = NavHostFragment.findNavController(AssignTransportFragment.this);
                                    if (navController.getCurrentDestination() == null
                                            || navController.getCurrentDestination().getId() != R.id.assignTransportFragment)
                                        return;
                                    navController.navigateUp();
                                    if(navController.getCurrentDestination().getId() == R.id.editorTransportCreateFragment){
                                        navController.navigateUp();
                                        if(navController.getCurrentDestination().getId() == R.id.editorTransportDocumentFragment)
                                            navController.navigateUp();
                                    }
                                }
                                dialog.dismiss();
                            },
                            "Ja",
                            (dialog, which) -> {
                                if (getActivity() != null) {
                                    NavController navController = NavHostFragment.findNavController(AssignTransportFragment.this);
                                    if (navController.getCurrentDestination() == null
                                            || navController.getCurrentDestination().getId() != R.id.assignTransportFragment)
                                        return;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("transportID", transportID);
                                    navController.navigate(R.id.action_assignTransportFragment_to_editorTransportUpdateFragment, bundle);
                                }
                                dialog.dismiss();
                            });
                } catch (ProcessingException e) {
                    Log.sendException(e);
                    if(getActivity() != null)
                        ((MainActivity) getActivity()).exceptionAlert("Transport konnte nicht zugewiesen werden!", e);
                }
            }));
        }
        return view;
    }

    /**
     * Method to generate the Bitmap of an QR Code for assigning the transport
     *
     * @param text the id of the transport to assign as String
     * @return Bitmap the bitmap to generate the QR Code from
     */
    public Bitmap generateQRCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Log.sendException(e);
            return null;
        }
    }
}