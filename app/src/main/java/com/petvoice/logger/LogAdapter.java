package com.petvoice.logger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

    private ArrayList<LogItem> mlogList;
    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView logView;
        private final TextView dateTime;
        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View
            logView = (TextView) view.findViewById(R.id.name);
            dateTime = (TextView) view.findViewById(R.id.address);
         }

        public TextView getTextView() {
            return logView;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param devicelist Arraylist<Sensor></> containing the data to populate views to be used
     * by RecyclerView.
     */
    public LogAdapter(ArrayList<LogItem> devicelist) {
        mlogList = devicelist;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.log_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.logView.setText(mlogList.get(position).getLog());
        viewHolder.dateTime.setText(mlogList.get(position).getDateTime());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mlogList.size();
    }
}