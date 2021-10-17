package com.petvoice.logger;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private ArrayList<BLEDevice> mDeviceList;
    private ItemClickListener mClickListener;
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private final TextView name;
        private final TextView address;
        private final ConstraintLayout root;
        ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            name = (TextView) view.findViewById(R.id.name);
            address = view.findViewById(R.id.address);
            root = view.findViewById(R.id.root);
            root.setOnClickListener(this);
         }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());

        }
    }
    /**
     * Initialize the dataset of the Adapter.
     *
     * @param devicelist Arraylist<Sensor></> containing the data to populate views to be used
     * by RecyclerView.
     */
    public DeviceAdapter(ArrayList<BLEDevice> devicelist) {
        mDeviceList = devicelist;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.device_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.name.setText(mDeviceList.get(position).getName()==null?"Unknown Device":mDeviceList.get(position).getName());
        viewHolder.address.setText(mDeviceList.get(position).getAddress());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }


    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {

        void onItemClick(View view, int position);
    }
}