package de.ehealth.evek.mobile.frontend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.mobile.R;

/**
 * Class belonging to the Transport Recycler Adapter
 *
 * @extends RecyclerView.Adapter<TransportRecyclerAdapter.ViewHolder>
 */
public class TransportRecyclerAdapter extends RecyclerView.Adapter<TransportRecyclerAdapter.ViewHolder>{

    private final List<TransportDetailsWithTransportDocument> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    /**
     * Record used for passing a {@link TransportDetails Transport} with the given {@link TransportDocument Transport Document}
     *
     * @param transportDetails - the {@link TransportDetails Transport} to be passed
     * @param transportDocument - the {@link TransportDocument Transport Document} to be passed
     */
    public record TransportDetailsWithTransportDocument(TransportDetails transportDetails,
                                                        TransportDocument transportDocument) {
    }

    /**
     * Constructor of TransportRecyclerAdapter <br>
     * Used for a RecyclerAdapter configured for displaying Transports
     *
     * @param context - the current context
     * @param data - List containing the Transports with Service Provider to display
     */    TransportRecyclerAdapter(Context context, List<TransportDetailsWithTransportDocument> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_transport_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransportRecyclerAdapter.ViewHolder holder, int position) {
        TransportDetailsWithTransportDocument detailsWithDoc = mData.get(position);
        TransportDetails details = detailsWithDoc.transportDetails();
        String transportDocument = details.transportDocument().id().value();
        holder.healthcareServiceProvider.setText(detailsWithDoc.transportDocument.healthcareServiceProvider().id().value());

        holder.transportServiceProvider.setText(detailsWithDoc.transportDetails().transportProvider().isPresent()
                        ? detailsWithDoc.transportDetails().transportProvider().get().id().value()
                        : "Nicht zugewiesen!");

        holder.details = details;
        holder.patient.setText(detailsWithDoc.transportDocument.patient().isPresent() ? detailsWithDoc.transportDocument.patient().get().id().value() : "Kein Patient zugewiesen!");
        holder.transportDocument.setText(transportDocument);
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        holder.date.setText(formatter.format(details.transportDate().getTime()));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * Class used for storing and recycling views as they are scrolled off screen
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView transportDocument;
        TextView patient;
        TextView healthcareServiceProvider;
        TextView transportServiceProvider;
        TextView date;
        TransportDetails details;

        /**
         * Constructor of ViewHolder <br>
         * Used for storing and recycling views as they are scrolled off screen
         *
         * @param itemView - the View to create the ViewHolder for
         */
        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            patient = itemView.findViewById(R.id.tv_patient);
            transportDocument = itemView.findViewById(R.id.tv_transport_document);
            healthcareServiceProvider = itemView.findViewById(R.id.tv_service_provider);
            date = itemView.findViewById(R.id.tv_date);
            transportServiceProvider = itemView.findViewById(R.id.tv_transport_provider);
        }

        @Override
        public void onClick(View view) {
            mClickListener.onItemClick(details, getAdapterPosition());
        }
    }

    /**
     * Method for getting the item at the clicked position
     *
     * @param id - the index/id of the Item
     *
     * @return TransportDetails - Item at the given index
     */
    TransportDetails getItem(int id) {
        return mData.get(id).transportDetails();
    }

    /**
     * Method to set the ItemClickListener
     *
     * @param itemClickListener - the ItemClickListener to be set
     */
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    /**
     * Interface used as Listener for performing click events
     */
    public interface ItemClickListener {
        //void onItemClick(View view, int position);
        //<T> void clickedItem(T item);

        /**
         * Method called on Item Click
         *
         * @param obj - the clicked Object
         * @param position - the position of the clicked Item
         */
        void onItemClick(TransportDetails obj, int position);
    }
}
