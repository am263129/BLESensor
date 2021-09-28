package com.ble.sensor;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

    private ArrayList<LogItem> mDeviceList;
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView address;
        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            name = (TextView) view.findViewById(R.id.name);
            address = (TextView) view.findViewById(R.id.address);
         }

        public TextView getTextView() {
            return name;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param devicelist Arraylist<Sensor></> containing the data to populate views to be used
     * by RecyclerView.
     */
    public LogAdapter(ArrayList<LogItem> devicelist) {
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
        viewHolder.name.setText(mDeviceList.get(position).getName());
        viewHolder.address.setText(mDeviceList.get(position).getAddress());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }
}