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

public class TransportDocumentRecyclerAdapter extends RecyclerView.Adapter<TransportDocumentRecyclerAdapter.ViewHolder>{

    private final List<TransportDocument> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    TransportDocumentRecyclerAdapter(Context context, List<TransportDocument> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
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

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView patient;
        TextView serviceProvider;
        TextView transportDocument;
        TransportDocument document;

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

    // convenience method for getting data at click position
    TransportDocument getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        //void onItemClick(View view, int position);
        void onItemClick(TransportDocument obj, int position);
        //<T> void clickedItem(T item);
    }

}
