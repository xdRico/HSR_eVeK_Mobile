package de.ehealth.evek.mobile.frontend;

import android.content.Context;
import android.graphics.Color;
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

/**
 * Class belonging to the SingleChoice Recycler Adapter
 *
 * @extends {@link RecyclerView.Adapter<SingleChoiceRecyclerAdapter.ViewHolder>}
 *
 * @noinspection rawtypes
 */
public class SingleChoiceRecyclerAdapter<T> extends RecyclerView.Adapter<SingleChoiceRecyclerAdapter.ViewHolder>{

    private boolean setup = false;
    private T toSetActive = null;
    private final Class<T> t;
    private final List<T> mData;
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    /**
     * Constructor of {@link SingleChoiceRecyclerAdapter} <br>
     * Used for a RecyclerAdapter configured for Single choice lists
     *
     * @param context the current {@link Context}
     * @param data {@link List} containing the choice instances
     * @param typeClass {@link Class} type of the items in the {@link List}
     */
    SingleChoiceRecyclerAdapter(Context context, List<T> data, Class<T> typeClass) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        t = typeClass;
    }

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

    @Override
    public int getItemCount() {
        return mData.size();
    }

    /**
     * Method used for setting the active Item
     *
     * @param item the Item to set as active
     */
    public void setActiveItem (T item){
        if(!setup) {
            toSetActive = item;
            return;
        }

        CheckBox box = checkBoxes.get(mData.indexOf(item));
            box.setChecked(true);
            box.callOnClick();
    }

    /**
     * Class used for storing and recycling {@link View Views} as they are scrolled off screen
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CheckBox checkBox;

        /**
         * Constructor of {@link ViewHolder} <br>
         * Class used for storing and recycling {@link View Views} as they are scrolled off screen
         *
         * @param itemView the {@link View} to create the {@link ViewHolder} for
         */
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

    /**
     * Method for getting the item at the clicked position
     *
     * @param id the index/id of the Item
     *
     * @return {@link T} - item of the {@link T type} at the given index
     */
    T getItem(int id) {
        return mData.get(id);
    }

    /**
     * Method to set the {@link ItemClickListener}
     *
     * @param itemClickListener the {@link ItemClickListener} to be set
     */
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    /**
     * Method used to set the {@link CheckBox CheckBoxes} editable or uneditable.
     *
     * @param editable if the {@link CheckBox CheckBoxes} should be editable
     */
    void setEditable(boolean editable){
        for(CheckBox box : checkBoxes)
            box.setEnabled(editable);
    }

    /**
     * Method used to set the {@link CheckBox CheckBoxes} valid or invalid.
     *
     * @param valid if the {@link CheckBox CheckBoxes} are valid
     */
    void setValid(boolean valid){
        for(CheckBox box : checkBoxes)
            box.setTextColor(valid ? Color.argb(255, 0, 0, 0) : Color.argb(255, 255, 100, 100));
    }

    /**
     * Interface used as Listener for performing click events
     */
    public interface ItemClickListener {

        /**
         * Method called on Item Click
         *
         * @param obj the clicked {@link T Object}
         * @param position the position of the clicked Item
         * @param <T> the {@link T Type} of the Item
         */
        <T> void onItemClick(T obj, int position);
    }
}
