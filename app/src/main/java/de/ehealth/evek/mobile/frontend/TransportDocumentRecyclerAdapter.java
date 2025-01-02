package de.ehealth.evek.mobile.frontend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.ehealth.evek.api.entity.TransportDocument;
import de.ehealth.evek.mobile.R;

/**
 * Class belonging to the TransportDocument Recycler Adapter
 *
 * @extends RecyclerView.Adapter<TransportDocumentRecyclerAdapter.ViewHolder>
 */
public class TransportDocumentRecyclerAdapter extends RecyclerView.Adapter<TransportDocumentRecyclerAdapter.ViewHolder>{

    private final List<TransportDocument> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    /**
     * Constructor of TransportDocumentRecyclerAdapter <br>
     * Used for a RecyclerAdapter configured for displaying Transport Documents
     *
     * @param context - the current context
     * @param data - List containing the TransportDocuments to display
     */
    TransportDocumentRecyclerAdapter(Context context, List<TransportDocument> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_transport_document_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransportDocumentRecyclerAdapter.ViewHolder holder, int position) {
        TransportDocument document = mData.get(position);
        String patient = document.patient().isPresent() ? document.patient().get().id().value() : "No Patient assigned!";
        String serviceProvider = document.healthcareServiceProvider().id().value();
        holder.document = document;
        holder.patient.setText(patient);
        holder.transportDocument.setText(document.id().value());
        holder.serviceProvider.setText(serviceProvider);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * Class used for storing and recycling views as they are scrolled off screen
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView patient;
        TextView serviceProvider;
        TextView transportDocument;
        TransportDocument document;

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
            serviceProvider = itemView.findViewById(R.id.tv_service_provider);
        }

        @Override
        public void onClick(View view) {
            mClickListener.onItemClick(document, getAdapterPosition());
        }
    }

    /**
     * Method for getting the item at the clicked position
     *
     * @param id - the index/id of the Item
     *
     * @return TransportDocument - Item at the given index
     */
    TransportDocument getItem(int id) {
        return mData.get(id);
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
        void onItemClick(TransportDocument obj, int position);
    }
}
