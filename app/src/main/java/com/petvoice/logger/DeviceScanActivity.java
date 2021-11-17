package com.petvoice.logger;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;


public class DeviceScanActivity extends AppCompatActivity implements DeviceAdapter.ItemClickListener {

    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner mBluetoothLeScanner;
    public BluetoothManager bluetoothManager;
    public ScanSettings scanSettings;
    public BluetoothLeScannerCompat scanner;
    private Handler mHandler;

    private ArrayList<BLEDevice> bluetoothDevices = new ArrayList<>();
    private DeviceAdapter deviceAdapter;
    private RecyclerView devicelist;
    private TextView labelScan, status;
    private ConstraintLayout toggleScan, bgScan;
    private ProgressBar progressBar;
    protected LinearLayoutManager mLayoutManager;

    boolean scanning = false;
    boolean bluetoothready = false;

    private final String TAG = "MainScan";


    private final int SCAN_PERIOD = 5000;
    private final int REPORT_DELAY = 5000;
    private int SCANMODE = 1;
    private final int MODE_CLASSIC = 0;
    private final int MODE_BLE = 1;
    private RadioButton modeBLE, modeClassic;
    private ConstraintLayout noDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUi();
        initScanner();
    }

    @Override
    protected void onResume() {
        updateUi();
        super.onResume();
    }

    public void initUi() {
        setContentView(R.layout.activity_devicescan);
        modeBLE = findViewById(R.id.mode_ble);
        modeClassic = findViewById(R.id.mode_classic);
        devicelist = findViewById(R.id.devicelist);
        progressBar = findViewById(R.id.progressBar);
        labelScan = findViewById(R.id.label_scan);
        bgScan = findViewById(R.id.bg_scan);
        noDevice = findViewById(R.id.no_device);
        status = findViewById(R.id.status);
        deviceAdapter = new DeviceAdapter(bluetoothDevices);
        devicelist.setAdapter(deviceAdapter);
        mLayoutManager = new LinearLayoutManager(this);
        toggleScan = findViewById(R.id.btnscan);
        devicelist.setLayoutManager(mLayoutManager);
        deviceAdapter.setClickListener(this);
        toggleScan.setSelected(true);
    }

    public void initScanner() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        scanner = BluetoothLeScannerCompat.getScanner();
        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();
    }

    public void updateUi() {
        try {
            labelScan.setText(scanning ? "Stop" : "Scan");
            status.setText(bluetoothDevices.size() == 0 ? "No Scanned Device" : "Let's Scan device.");
            bgScan.setBackgroundResource(scanning ? R.drawable.ic_stopscan : R.drawable.ic_scanback);
            toggleScan.setSelected(!scanning);
            noDevice.setVisibility(scanning ? View.GONE : bluetoothDevices.size() == 0 ? View.VISIBLE : View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ToggleScan(View view) {
        if (scanning) {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
            scanning = false;
            progressBar.setVisibility(View.GONE);
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                    scanning = false;
                    updateUi();
                    progressBar.setVisibility(View.GONE);
                }
            }, SCAN_PERIOD);
            progressBar.setVisibility(View.VISIBLE);
            bluetoothDevices.clear();
            deviceAdapter.notifyDataSetChanged();
            mBluetoothLeScanner.startScan(null, scanSettings, mLeScanCallback);
            scanning = true;
        }
        updateUi();
    }

    public void modeSetting(View view) {
        SCANMODE = modeClassic.isChecked() ? MODE_CLASSIC : MODE_BLE;
    }

    public void OpenDevice(int position) {
        if (scanning) {
            if (SCANMODE == MODE_CLASSIC) {
                mBluetoothAdapter.cancelDiscovery();
            } else {
                mBluetoothLeScanner.stopScan(mLeScanCallback);
            }
            scanning = false;
            updateUi();
        }
        Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra("name", bluetoothDevices.get(position).getName());
        intent.putExtra("address", bluetoothDevices.get(position).getAddress());
        startActivity(intent);
    }

    public void OpenDevice(BluetoothDevice WearDev) {
        if (scanning) {
            if (SCANMODE == MODE_CLASSIC) {
                mBluetoothAdapter.cancelDiscovery();
            } else {
                mBluetoothLeScanner.stopScan(mLeScanCallback);
            }
            scanning = false;
            updateUi();
        }
        Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra("name", WearDev.getName());
        intent.putExtra("address", WearDev.getAddress());
        startActivity(intent);
    }

    private void addLog(String msg) {
        Log.e(TAG, msg);
    }

    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            manageScanResult(result);
        }

        @Override
        public void onBatchScanResults(@NonNull List<ScanResult> results) {
            addLog("Batch mode" + results.size());
            for (ScanResult result : results) {
                manageScanResult(result);
            }
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            addLog("Scan Failed. error code:" + errorCode);
            super.onScanFailed(errorCode);
        }
    };

    private void manageScanResult(ScanResult result) {
        addLog("Found device in onScanResult");
        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();
        String devicename = result.getDevice().getName();
        addLog("name from result " + devicename);
        if (devicename == null) {
            devicename = result.getScanRecord().getDeviceName();
            addLog("name from record " + devicename);
        }
        addLog("Bluetooth Device Found. Name:" + devicename + " Address:" + address);
        boolean isnew = true;
        for (BLEDevice item : bluetoothDevices) {
            if (item.getAddress().equals(device.getAddress())) {
                isnew = false;
                break;
            }
        }
        if (isnew) {
            status.setText("Device Found");
            bluetoothDevices.add(new BLEDevice(devicename, address));
            DeviceScanActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    deviceAdapter.notifyDataSetChanged();
                }
            });
        }
        if (devicename != null && devicename.equals("WearDev")) {
            addLog("WearDev Sensor Device Found." + " Address:" + address);
            Toast.makeText(this, "Sensor Found, auto connecting...", Toast.LENGTH_SHORT).show();
            OpenDevice(device);
        }
    }


    @Override
    public void onItemClick(View view, int position) {
        OpenDevice(position);
    }
}