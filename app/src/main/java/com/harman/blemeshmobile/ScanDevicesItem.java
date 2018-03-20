package com.harman.blemeshmobile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mreddy3 on 3/19/2018.
 */

public class ScanDevicesItem extends RecyclerView.Adapter<ScanDevicesItem.ScanHolder>{

    ArrayList<ScanResult> mScannedDevicesList = null;

    public interface DeviceClicked{
        public void onDeviceClicked(int pos);
    }

    DeviceClicked mCallBack = null;

    Context mContext = null;

    public class ScanHolder extends RecyclerView.ViewHolder{

        TextView mName = null;
        TextView mAddr = null;

        public ScanHolder(View view){
            super(view);

            mName = view.findViewById(R.id.txtv_name);
            mAddr = view.findViewById(R.id.txtv_mac_addr);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallBack.onDeviceClicked(getAdapterPosition());
                }
            });
        }
    }

    public ScanDevicesItem(Context context, DeviceClicked CallBack){
        mContext = context;
        mCallBack = CallBack;
        mScannedDevicesList = new ArrayList<>();
    }

    @Override
    public int getItemCount() {
        return mScannedDevicesList.size();
    }

    @Override
    public void onBindViewHolder(ScanHolder holder, int position) {
        BluetoothDevice mac = mScannedDevicesList.get(position).getDevice();
        holder.mName.setText(mac.getName());
        holder.mAddr.setText(mac.getAddress());
    }

    @Override
    public ScanHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        int lyt = R.layout.found_device_list_item;
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(lyt, parent, false);
        return new ScanHolder(view);
    }

    public ScanResult getSelectedDevice(int pos){
        return mScannedDevicesList.get(pos);
    }

    public void addDevice(ScanResult device){
       for (ScanResult added : mScannedDevicesList){
           if (added.getDevice().getAddress().equalsIgnoreCase(device.getDevice().getAddress())){
               return;
           }
       }
        mScannedDevicesList.add(device);
        notifyDataSetChanged();
    }

    public void addAllDevices(List<ScanResult> allFoundDevices){
        mScannedDevicesList.addAll(allFoundDevices);
        notifyDataSetChanged();
    }

    public void clearAll(){
        mScannedDevicesList.clear();
        notifyDataSetChanged();
    }
}
