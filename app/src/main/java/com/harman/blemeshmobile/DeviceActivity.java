package com.harman.blemeshmobile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeviceActivity extends AppCompatActivity implements View.OnClickListener {
    Bundle mInComingData = null;

    TextView mDeviceMac;
    TextView mDeviceName;
    TextView mConnStatus;

    Button mScanNodesBtn;
    Button mSwithAllLightsBtn;

    BluetoothGatt mBTGattInst = null;

    BluetoothAdapter mBleAdapter = null;

    BluetoothDevice mBTLeDevice = null;

    String mName = null;
    String mMAC = null;

    final int BLE_CONNECTED = 0;
    final int BLE_DISCONNECTED = -1;

    final UUID CHARACTERISTIC_TX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    final UUID CHARACTERISTIC_RX = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    final UUID UUID_UART = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");


    BluetoothGattService mBleUARTService = null;

    BluetoothGattCharacteristic mWriteCharacteristic = null;
    BluetoothGattCharacteristic mReadCharacteristic = null;

    LinearLayout colorLyt = null;
    Switch mControlAll = null;

    Button btn_red;
    Button btn_blue;
    Button btn_green;
    Button btn_white;

    SeekBar seekBar_Intensity = null;

    final int WRITE_DELAY = 20;

    boolean mIsAll = true;

    final int WRITE = 0x10;
    final int READ  = 0x20;

    HandlerThread mThread;
    Handler mDeviceHandler;


    class DeviceHandler extends Handler{
        public DeviceHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case WRITE:
                    writeToDevice((byte[])msg.obj);
                    break;
                case READ:
                    break;

            }
        }
    }

    Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BLE_CONNECTED:
                    mConnStatus.setText("Connected");
                    mScanNodesBtn.setEnabled(true);
                    mSwithAllLightsBtn.setEnabled(true);
                    colorLyt.setVisibility(View.VISIBLE);
                    mControlAll.setVisibility(View.VISIBLE);
                    seekBar_Intensity.setVisibility(View.VISIBLE);

                    break;

                case BLE_DISCONNECTED:
                    mConnStatus.setText("Disconnected");
                    mScanNodesBtn.setEnabled(false);
                    mSwithAllLightsBtn.setEnabled(false);
                    colorLyt.setVisibility(View.GONE);
                    mControlAll.setVisibility(View.GONE);
                    seekBar_Intensity.setVisibility(View.GONE);
                    break;

            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mDeviceMac = findViewById(R.id.txtv_mac_addr);
        mDeviceName = findViewById(R.id.txtv_name);
        mConnStatus = findViewById(R.id.txtv_conn_status);

        mScanNodesBtn = findViewById(R.id.btn_get_paired_nodes);
        mSwithAllLightsBtn = findViewById(R.id.btn_Turn_on_all_lights);

        mScanNodesBtn.setOnClickListener(this);
        mSwithAllLightsBtn.setOnClickListener(this);

        colorLyt = findViewById(R.id.lyt_color);
        btn_red = findViewById(R.id.btn_red);
        btn_blue = findViewById(R.id.btn_blue);
        btn_green = findViewById(R.id.btn_green);
        btn_white = findViewById(R.id.btn_white);

        btn_red.setOnClickListener(this);
        btn_blue.setOnClickListener(this);
        btn_green.setOnClickListener(this);
        btn_white.setOnClickListener(this);

        mThread = new HandlerThread("DeviceThread");
        mThread.start();
        mDeviceHandler = new DeviceHandler(mThread.getLooper());

        mControlAll = findViewById(R.id.sw_single_or_both);
        mControlAll.setChecked(true);
        mControlAll.setShowText(true);
        mControlAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                mIsAll = b;
            }
        });

        seekBar_Intensity = findViewById(R.id.sbe_intensity);
        seekBar_Intensity.setVisibility(View.GONE);
        seekBar_Intensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                byte[] data = {
                        0x00,
                        0x00,
                        0x03,
                        0x01,
                        0x00,
                        0x00,
                        0x00

                };
                data[5] = (byte) ((i & 0x000000FF) >> 0);

                Message msg = Message.obtain();
                msg.what = WRITE;
                msg.obj = data;

                mDeviceHandler.sendMessage(msg);



            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mConnStatus.setText("Not Connected");
        mInComingData = getIntent().getExtras();
        if (null != mInComingData) {

            mName = mInComingData.getString("DEVICE_NAME");
            mMAC = mInComingData.getString("DEVICE_MAC");


        } else {
            finish();
        }


        mDeviceName.setText(mName);
        mDeviceMac.setText(mMAC);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = bluetoothManager.getAdapter();

        mBTLeDevice = mBleAdapter.getRemoteDevice(mMAC);
        mBTGattInst = mBTLeDevice.connectGatt(getApplicationContext(), false, mBTGattCallBack);


    }

    BluetoothGattCallback mBTGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                try {
                    Message msg = Message.obtain();
                    msg.what = BLE_CONNECTED;
                    mMessenger.send(msg);
                } catch (Exception ee) {
                    Log.e("DeviceActivity", ee.getMessage());
                }
                mBTGattInst.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                try {
                    Message msg = Message.obtain();
                    msg.what = BLE_DISCONNECTED;
                    mMessenger.send(msg);
                } catch (Exception ee) {
                    Log.e("DeviceActivity", ee.getMessage());
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {


            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                Log.d("Data", stringBuilder.toString());

            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            Log.d("onCharacteristicRead", characteristic.toString());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (BluetoothGatt.GATT_FAILURE == status) {
                return;
            }


            for (BluetoothGattService gattService : gatt.getServices()) {
                Log.d("services", gattService.getUuid().toString());
            }


            mBleUARTService = mBTGattInst.getService(UUID_UART);
            if (mBleUARTService == null) {
                Log.e("onServicesDiscovered", CHARACTERISTIC_RX.toString() + " is not supported");
                return;
            } else {
                mReadCharacteristic = mBleUARTService.getCharacteristic(CHARACTERISTIC_RX);
                mWriteCharacteristic = mBleUARTService.getCharacteristic(CHARACTERISTIC_TX);
            }


            mBTGattInst.setCharacteristicNotification(mWriteCharacteristic, true);
            mBTGattInst.setCharacteristicNotification(mReadCharacteristic, true);




        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("onCharacteristicChanged", characteristic.toString());
        }


    };

    @Override
    protected void onDestroy() {
        if (null != mBTGattInst) {
            mBTGattInst.close();
        }
        super.onDestroy();
    }


    public void writeToDevice(byte[] data) {
        mWriteCharacteristic.setValue(data);
        mBTGattInst.writeCharacteristic(mWriteCharacteristic);
    }

    @Override
    public void onClick(View view) {
        Message msg = null;
        switch (view.getId()) {
            case R.id.btn_red:
                if (null != mWriteCharacteristic) {
                    byte[] red = {
                            0x00,
                            0x00,
                            0x03,
                            0x01,
                            0x32,
                            0x00,
                            0x00

                    };
                    msg = Message.obtain();
                    msg.what = WRITE;
                    msg.obj = red;

                    mDeviceHandler.sendMessage(msg);

                    if (mIsAll){
                        try {
                            Thread.sleep(WRITE_DELAY);
                        } catch (Exception ee) {

                        }
                        red[1] = 0x01;
                        msg = Message.obtain();
                        msg.what = WRITE;
                        msg.obj = red;

                        mDeviceHandler.sendMessage(msg);
                    }




                }
                break;
            case R.id.btn_blue:
                if (null != mWriteCharacteristic) {
                    byte[] blue = {
                            0x00,
                            0x00,
                            0x03,
                            0x01,
                            0x00,
                            0x00,
                            0x32

                    };
                    msg = Message.obtain();
                    msg.what = WRITE;
                    msg.obj = blue;

                    mDeviceHandler.sendMessage(msg);

                    if (mIsAll) {
                        try {
                            Thread.sleep(WRITE_DELAY);
                        } catch (Exception ee) {

                        }
                        blue[1] = 0x01;
                        msg = Message.obtain();
                        msg.what = WRITE;
                        msg.obj = blue;

                        mDeviceHandler.sendMessage(msg);
                    }


                }
                break;
            case R.id.btn_green:
                if (null != mWriteCharacteristic) {
                    byte[] green = {
                            0x00,
                            0x00,
                            0x03,
                            0x01,
                            0x00,
                            0x32,
                            0x00

                    };
                    msg = Message.obtain();
                    msg.what = WRITE;
                    msg.obj = green;

                    mDeviceHandler.sendMessage(msg);

                    if (mIsAll) {
                        try {
                            Thread.sleep(WRITE_DELAY);
                        } catch (Exception ee) {

                        }
                        green[1] = 0x01;
                        msg = Message.obtain();
                        msg.what = WRITE;
                        msg.obj = green;

                        mDeviceHandler.sendMessage(msg);
                    }


                }
                break;
            case R.id.btn_white:
                if (null != mWriteCharacteristic) {
                    byte[] white = {
                            0x00,
                            0x00,
                            0x03,
                            0x01,
                            0x32,
                            0x32,
                            0x32

                    };
                    msg = Message.obtain();
                    msg.what = WRITE;
                    msg.obj = white;

                    mDeviceHandler.sendMessage(msg);

                    if (mIsAll) {
                        try {
                            Thread.sleep(WRITE_DELAY);
                        } catch (Exception ee) {

                        }
                        white[1] = 0x01;
                        msg = Message.obtain();
                        msg.what = WRITE;
                        msg.obj = white;

                        mDeviceHandler.sendMessage(msg);
                    }

                }
                break;


            case R.id.btn_get_paired_nodes:
                byte[] sensor_info = {
                        0x00, 0x00, 0x00, 0x02
                };
                msg = Message.obtain();
                msg.what = WRITE;
                msg.obj = sensor_info;

                mDeviceHandler.sendMessage(msg);




                //mReadCharacteristic.setValue(sensor_info);
                //mBTGattInst.readCharacteristic(mReadCharacteristic);

                break;
            case R.id.btn_Turn_on_all_lights:
                if (mSwithAllLightsBtn.getText().toString().equalsIgnoreCase("ON ALL")) {

                        byte[] yellow_all = {
                                0x00,
                                0x00,
                                0x03,
                                0x01,
                                0x72,
                                0x05,
                                0x00

                        };


                    msg = Message.obtain();
                    msg.what = WRITE;
                    msg.obj = yellow_all;

                    mDeviceHandler.sendMessage(msg);

                        try {
                            Thread.sleep(WRITE_DELAY);
                        } catch (Exception ee) {

                        }
                        yellow_all[1] = 0x01;
                        msg = Message.obtain();
                        msg.what = WRITE;
                        msg.obj = yellow_all;

                        mDeviceHandler.sendMessage(msg);
                        mSwithAllLightsBtn.setText("OFF ALL");

                }
                else{

                        byte[] yellow_all = {
                                0x00,
                                0x00,
                                0x03,
                                0x00

                        };

                        msg = Message.obtain();
                        msg.what = WRITE;
                        msg.obj = yellow_all;

                        mDeviceHandler.sendMessage(msg);

                        try {
                            Thread.sleep(WRITE_DELAY);
                        } catch (Exception ee) {

                        }
                        yellow_all[1] = 0x01;
                        msg = Message.obtain();
                        msg.what = WRITE;
                        msg.obj = yellow_all;

                        mDeviceHandler.sendMessage(msg);
                        mSwithAllLightsBtn.setText("ON ALL");

                    }

                break;
        }
    }
}
