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
 * Class belonging to the User MainPage Fragment
 *
 * @extends {@link Fragment}
 *
 * @implements {@link TransportRecyclerAdapter.ItemClickListener}
 */
public class MainPageUserFragment extends Fragment implements TransportRecyclerAdapter.ItemClickListener,
        DataHandler.TransportsChangedListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_page_user, container, false);

        updateRecyclerAdapters(view);

        return view;
    }

    private void updateRecyclerAdapters(View view){

        DataHandler handler = DataHandler.instance();

        RecyclerView recyclerViewTransport = view.findViewById(R.id.rv_transports);
        recyclerViewTransport.setLayoutManager(new LinearLayoutManager(getActivity()));

        handler.runOnNetworkThread(() -> {

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
            getActivity().runOnUiThread(() -> recyclerViewTransport.setAdapter(transportAdapter));
        });
    }

    @Override
    public void onItemClick(TransportDetails obj, int position) {
        NavController navController = NavHostFragment.findNavController(MainPageUserFragment.this);
        if(getActivity() == null
                || navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != R.id.mainPageUserFragment) return;
        Bundle bundle = new Bundle();
        bundle.putString("transportID", obj.id().value());
        DataHandler handler = DataHandler.instance();
        handler.runOnNetworkThread(() -> {
        if(obj.transportProvider().get().id().value().equalsIgnoreCase(
                handler.getUser().serviceProvider().id().value())){
            if(obj.transporterSignature().isPresent() && obj.transporterSignatureDate().isPresent()){
                if(obj.patientSignature().isPresent() && obj.patientSignatureDate().isPresent())
                    bundle.putBoolean("finished", true);
                    //((MainActivity) getActivity()).informationAlert(getString(R.string.title_popup_illegal_operation), getString(R.string.content_popup_transport_already_signed));
                else
                    bundle.putBoolean("validation", true);

                getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageUserFragment_to_editorTransportUpdateFragment, bundle));
            }else{
                getActivity().runOnUiThread(() -> navController.navigate(R.id.action_mainPageUserFragment_to_editorTransportUpdateFragment, bundle));
            }
        } else
            ((MainActivity) getActivity()).informationAlert(getString(R.string.title_popup_illegal_operation), getString(R.string.content_popup_transport_already_assigned));
        });
    }

    @Override
    public void onTransportsChanged() {
        if(getView() != null)
            updateRecyclerAdapters(getView());
    }
}