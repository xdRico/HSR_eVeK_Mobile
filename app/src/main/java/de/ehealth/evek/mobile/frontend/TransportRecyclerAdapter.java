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

import de.ehealth.evek.api.entity.ServiceProvider;
import de.ehealth.evek.api.entity.TransportDetails;
import de.ehealth.evek.api.type.Id;
import de.ehealth.evek.mobile.R;

public class TransportRecyclerAdapter extends RecyclerView.Adapter<TransportRecyclerAdapter.ViewHolder>{

    private final List<TransportDetailsWithServiceProvider> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    public record TransportDetailsWithServiceProvider(TransportDetails transportDetails,
                                                      Id<ServiceProvider> serviceProviderId) {
    }

    // data is passed into the constructor
    TransportRecyclerAdapter(Context context, List<TransportDetailsWithServiceProvider> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;

    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_transport_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransportRecyclerAdapter.ViewHolder holder, int position) {
        TransportDetailsWithServiceProvider detailsWithSP = mData.get(position);
        TransportDetails details = detailsWithSP.transportDetails();
        String transportDocument = details.transportDocument().id().value();
        holder.healthcareServiceProvider.setText(detailsWithSP.serviceProviderId().value());

        holder.transportServiceProvider.setText(detailsWithSP.transportDetails().transportProvider().isPresent()
                        ? detailsWithSP.transportDetails().transportProvider().get().id().value()
                        : "Nicht zugewiesen!");

        holder.details = details;
        holder.transportDocument.setText(transportDocument);
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        holder.date.setText(formatter.format(details.transportDate().getTime()));

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView transportDocument;
        TextView healthcareServiceProvider;
        TextView transportServiceProvider;
        TextView date;
        TransportDetails details;


        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
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

    // convenience method for getting data at click position
    TransportDetails getItem(int id) {
        return mData.get(id).transportDetails();
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        //void onItemClick(View view, int position);
        void onItemClick(TransportDetails obj, int position);
        //<T> void clickedItem(T item);
    }

}
