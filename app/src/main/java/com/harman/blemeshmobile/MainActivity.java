package com.harman.blemeshmobile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    RecyclerView mRcvFoundDevicesList = null;

    Button mStartScanBtn = null;

    ScanDevicesItem mAdapter = null;

    private BluetoothAdapter mBleAdapter = null;

    final int BT_ENABLE_REQUEST_CODE = 1234;

    private static final long SCA_PERIOD = 15000;

    private Handler mHandler = null;

    UUID[] leScanUUID = {UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")};

    BluetoothGatt mBluetoothGatt = null;

    BluetoothLeScanner bleScanner = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStartScanBtn = findViewById(R.id.btn_ble_bridge);
        mRcvFoundDevicesList = findViewById(R.id.rcv_scan_found_devices);

        LinearLayoutManager ltyManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRcvFoundDevicesList.setLayoutManager(ltyManager);
        mRcvFoundDevicesList.setHasFixedSize(true);
        mAdapter = new ScanDevicesItem(this, mDeviceClickHandler);
        mRcvFoundDevicesList.setAdapter(mAdapter);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "BLE is not supported by this device", Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = bluetoothManager.getAdapter();

        if (mBleAdapter == null || !mBleAdapter.isEnabled()){
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, BT_ENABLE_REQUEST_CODE);
        }

        mHandler = new Handler();

        mStartScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapter.clearAll();
               bleScanner = mBleAdapter.getBluetoothLeScanner();
                mStartScanBtn.setEnabled(false);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mStartScanBtn.setEnabled(true);
                            bleScanner.stopScan(mLeScanCallback);
                        }
                    }, SCA_PERIOD);

                ScanFilter filter = new ScanFilter.Builder().
                        setServiceUuid(ParcelUuid.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")).
                        build();
                List<ScanFilter> scanFilter = new ArrayList<>();
                scanFilter.add(filter);
                ScanSettings settings = new ScanSettings.Builder().
                        setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                bleScanner.startScan(scanFilter, settings, mLeScanCallback);
                //ScanFilter filter = new ScanFilter().

            }
        });
    }

    ScanDevicesItem.DeviceClicked mDeviceClickHandler = new ScanDevicesItem.DeviceClicked() {
        @Override
        public void onDeviceClicked(int pos) {
            ScanResult selectedDevice = mAdapter.getSelectedDevice(pos);
            Intent deviceActiivty = new Intent(getApplicationContext(), DeviceActivity.class);
            Bundle data = new Bundle();
            data.putString("DEVICE_MAC", selectedDevice.getDevice().getAddress());
            data.putString("DEVICE_NAME", selectedDevice.getDevice().getName());
            deviceActiivty.putExtras(data);
            startActivity(deviceActiivty);
            bleScanner.stopScan(mLeScanCallback);


        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addDevice(result);
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            /*if(!results.isEmpty()){
                mAdapter.addAllDevices(results);
            }*/
        }
    };
    /*
    {
        @Override
        public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAdapter.addDevice(bluetoothDevice);
                }
            });
        }
    };*/
}
