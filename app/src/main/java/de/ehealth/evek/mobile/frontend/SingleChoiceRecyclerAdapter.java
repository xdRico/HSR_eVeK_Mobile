package de.ehealth.evek.mobile.frontend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.ehealth.evek.api.type.PatientCondition;
import de.ehealth.evek.api.type.TransportReason;
import de.ehealth.evek.api.type.TransportationType;
import de.ehealth.evek.mobile.R;
import de.ehealth.evek.mobile.network.DataHandler;

/** @noinspection rawtypes*/
public class SingleChoiceRecyclerAdapter<T> extends RecyclerView.Adapter<SingleChoiceRecyclerAdapter.ViewHolder>{

    private boolean setup = false;
    private T toSetActive = null;
    private final Class<T> t;
    private final List<T> mData;
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    SingleChoiceRecyclerAdapter(Context context, List<T> data, Class<T> typeClass) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        t = typeClass;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_single_choice_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SingleChoiceRecyclerAdapter.ViewHolder holder, int position) {
        T item = mData.get(position);
        String s = item.toString();
        if(t == TransportReason.class)
            s = DataHandler.getTransportReasonString((TransportReason) item);
        else if(t == TransportationType.class)
            s = DataHandler.getTransportationTypeString((TransportationType) item);
        else if(t == PatientCondition.class)
            s = DataHandler.getPatientConditionString((PatientCondition) item);
        holder.checkBox.setText(s);
        if(toSetActive != null && item == toSetActive)
            holder.checkBox.setChecked(true);
        checkBoxes.add(holder.checkBox);
        setup = true;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setActiveItem (T item){
        if(!setup) {
            toSetActive = item;
            return;
        }

        CheckBox box = checkBoxes.get(mData.indexOf(item));
            box.setChecked(true);
            box.callOnClick();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.cb_recycler_row_name);
            checkBox.setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {

            for(CheckBox box : checkBoxes)
                if(box != view) box.setChecked(false);
            if (mClickListener != null)
                mClickListener.onItemClick(t, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    T getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        //void onItemClick(View view, int position);
        <T> void onItemClick(T obj, int position);
        //<T> void clickedItem(T item);
    }

}
