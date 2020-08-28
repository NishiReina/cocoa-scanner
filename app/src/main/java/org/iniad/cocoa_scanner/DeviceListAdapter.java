package org.iniad.cocoa_scanner;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
    private List<NeighbourDevice> devices;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        public TextView tvId;
        public TextView tvRssi;

        public ViewHolder(ViewGroup v) {
            super(v);
            this.layout = (LinearLayout) v;
            this.tvId = (TextView)v.findViewById(R.id.tv_id);
            this.tvRssi = (TextView)v.findViewById(R.id.tv_rssi);
        }
    }

    public DeviceListAdapter(List<NeighbourDevice> devices) {
        this.devices = devices;
    }

    @Override
    public DeviceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup v = (ViewGroup) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.found_device, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int rssi = devices.get(position).getRssi();
        holder.tvId.setText(devices.get(position).getId());
        holder.tvRssi.setText(String.valueOf(rssi));

        int r = 0xff;
        int g = - rssi * 2;
        int b = - rssi * 2;
        int color = 0xff << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
        holder.layout.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}