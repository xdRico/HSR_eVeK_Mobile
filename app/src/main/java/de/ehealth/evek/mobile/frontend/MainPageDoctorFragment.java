package de.ehealth.evek.mobile.frontend;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.api.exception.ProcessingException;
import de.ehealth.evek.api.util.Log;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.core.MainActivity;
import de.ehealth.evek.mobile.network.DataHandler;

/**
 * Class belonging to the Doctor MainPage Fragment
 *
 * @extends {@link Fragment}
 *
 * @implements {@link TransportDocumentRecyclerAdapter.ItemClickListener}
 * @implements {@link TransportRecyclerAdapter.ItemClickListener}
 */
public class MainPageDoctorFragment extends Fragment implements TransportDocumentRecyclerAdapter.ItemClickListener,
        TransportRecyclerAdapter.ItemClickListener, DataHandler.TransportDocumentsChangedListener, DataHandler.TransportsChangedListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_page_doctor, container, false);

        updateRecyclerAdapters(view);

        view.findViewById(R.id.btn_transport_doc_create).setOnClickListener((l) -> {
            NavController navController = NavHostFragment.findNavController(MainPageDoctorFragment.this);
            if(navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.mainPageDoctorFragment) return;
            if(getActivity() == null) return;
            getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportDocFragment));
        });

        view.findViewById(R.id.btn_transport_create).setOnClickListener((l) -> {
            NavController navController = NavHostFragment.findNavController(MainPageDoctorFragment.this);
            if(navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != R.id.mainPageDoctorFragment) return;
            if(getActivity() == null) return;
            getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportCreateFragment));
        });
        return view;
    }

    private void updateRecyclerAdapters(View view){

        DataHandler handler = DataHandler.instance();

        RecyclerView recyclerViewDocument = view.findViewById(R.id.rv_transport_documents);
        recyclerViewDocument.setLayoutManager(new LinearLayoutManager(getActivity()));

        RecyclerView recyclerViewTransport = view.findViewById(R.id.rv_transports);
        recyclerViewTransport.setLayoutManager(new LinearLayoutManager(getActivity()));

        handler.runOnNetworkThread(() -> {

            List<TransportDocument> docs = handler.getTransportDocuments();
            TransportDocumentRecyclerAdapter transportDocumentAdapter = new TransportDocumentRecyclerAdapter(getActivity(), docs);
            transportDocumentAdapter.setClickListener(this);

            List<TransportRecyclerAdapter.TransportDetailsWithTransportDocument> detailsWithSP = new ArrayList<>();
            Exception invalid = null;
            for(TransportDetails detail : handler.getTransportDetails()){
                try{
                    TransportDocument transportDocument = handler.getTransportDocumentById(detail.transportDocument().id());
                    detailsWithSP.add(new TransportRecyclerAdapter.TransportDetailsWithTransportDocument(detail, transportDocument));
                }catch(ProcessingException e){
                    Log.sendMessage("TransportDocument not found!");
                    invalid = e;
                }
            }
            if(invalid != null && getActivity() != null)
                ((MainActivity) getActivity()).exceptionAlert("Fehler beim Laden von mindestens einem Transport!", invalid);

            TransportRecyclerAdapter transportAdapter = new TransportRecyclerAdapter(getActivity(), detailsWithSP);
            transportAdapter.setClickListener(this);

            if(getActivity() == null)
                return;
            getActivity().runOnUiThread(() -> {

                recyclerViewDocument.setAdapter(transportDocumentAdapter);
                recyclerViewTransport.setAdapter(transportAdapter);
            });
        });
    }

    @Override
    public void onItemClick(TransportDocument obj, int position) {
        NavController navController = NavHostFragment.findNavController(MainPageDoctorFragment.this);
        if(getActivity() == null
                || navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.mainPageDoctorFragment) return;
        MainActivity activity = (MainActivity) getActivity();
        if(obj.patient() == null || obj.patient().isEmpty())
            activity.choiceAlert("Transportschein bearbeiten oder Transport erstellen?",
                    "Soll der Transportschein bearbeitet oder überprüft werden oder soll ein neuer Transport für den Transportschein angelegt werden?",
                    "Patient zuweisen",
                    (dialog, which) -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("transportDocumentID", obj.id().value());
                        bundle.putBoolean("editPatientData", true);
                        getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportDocFragment, bundle));
                        dialog.dismiss();
                    },
                    "Bearbeiten/überprüfen",
                    (dialog, which) -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("transportDocumentID", obj.id().value());
                        getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportDocFragment, bundle));
                        dialog.dismiss();
                    },
                    "Transport erstellen",
                    (dialog, which) -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("transportDocumentID", obj.id().value());
                        navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportCreateFragment, bundle);
                        dialog.dismiss();

                    });
        else
            activity.choiceAlert("Transportschein bearbeiten oder Transport erstellen?",
                "Soll der Transportschein bearbeitet oder überprüft werden oder soll ein neuer Transport für den Transportschein angelegt werden?",
        "Bearbeiten/überprüfen",
                (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("transportDocumentID", obj.id().value());
                    getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportDocFragment, bundle));
                    dialog.dismiss();
                },
                "Transport erstellen",
                (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("transportDocumentID", obj.id().value());
                    navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportCreateFragment, bundle);
                    dialog.dismiss();

                });
    }

    @Override
    public void onItemClick(TransportDetails obj, int position) {
        NavController navController = NavHostFragment.findNavController(MainPageDoctorFragment.this);
        if(getActivity() == null
                || navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.mainPageDoctorFragment) return;
        Bundle bundle = new Bundle();
        bundle.putString("transportID", obj.id().value());
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
            if(obj.transportProvider() == null || obj.transportProvider().isEmpty())
                getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_assignTransportFragment, bundle));
            else if(obj.transportProvider().get().id().value().equalsIgnoreCase(
                    handler.getUser().serviceProvider().id().value())){
                if(obj.transporterSignature().isPresent() && obj.transporterSignatureDate().isPresent()){
                    if(obj.patientSignature().isPresent() && obj.patientSignatureDate().isPresent())
                        bundle.putBoolean("finished", true);
                    else
                        bundle.putBoolean("validation", true);

                    getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportUpdateFragment, bundle));
                }else{
                    getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageDoctorFragment_to_editorTransportUpdateFragment, bundle));
                }
            } else
                ((MainActivity) getActivity()).informationAlert(getString(R.string.title_popup_illegal_operation), getString(R.string.content_popup_transport_already_assigned));
        });
    }

    @Override
    public void onTransportDocumentsChanged() {
        if(getView() != null)
            updateRecyclerAdapters(getView());
    }

    @Override
    public void onTransportsChanged() {
        if(getView() != null)
            updateRecyclerAdapters(getView());
    }
}