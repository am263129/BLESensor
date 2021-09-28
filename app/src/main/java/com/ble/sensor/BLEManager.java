package com.ble.sensor;

import static com.ble.sensor.MainActivity.AddLog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;

public class BLEManager {
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner bluetoothLeScanner;
    public BluetoothManager bluetoothManager;
    public ScanSettings scanSettings;
    public AdvertiseSettings adSettings;
    public ScanCallback scanCallback;
    public BluetoothGattCallback gattCallback;
    public BluetoothGatt bluetoothGatt;
    public BluetoothDevice sensor;
    private boolean isScanning = false;
    private MainActivity context;
    public String TAG = "BLEManager";
    public List<BluetoothGattService> servicelist = new ArrayList<>();
    public List<BluetoothGattCharacteristic> characteristicslist = new ArrayList<>();
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 20000;
    public BLEManager(MainActivity context) {
        this.context = context;
    }

    public void initScanner() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                addLog("Connected Listener, status:" + status + ", new state:" + newState);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        addLog("Device Connected");
                        addLog("Start Discover Service");
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        addLog("Device Disconnected");
                        gatt.close();
                    }
                } else {
                    addLog("Error Bluetooth Gatt status");
                    gatt.close();
                }
                super.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                bluetoothGatt = gatt;
                if (status == BluetoothGatt.GATT_FAILURE) {
                    addLog("Service discovery failed");
                    gatt.close();
                    return;
                } else {
                    try {
                        context.serviceList.clear();
                        servicelist = gatt.getServices();
                        for (BluetoothGattService service : servicelist) {
//                            service.getCharacteristics();
                            context.serviceList.add(service.getUuid().toString());
                            addLog("Service UUID:" + service.getUuid().toString());
                        }

                    } catch (Exception E) {
                        addLog("Exception on getting service uuid. " + E.toString());
                    }
                }
                super.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                String msg = "";
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    msg = "Status success";
                    addLog("______Data Start_______");
                    byte[] result = characteristic.getValue();
                    for (byte b : result) {
                        addLog(String.format("0x%20x", b));
                    }
                    addLog("______Data End_______");
                    Util.toHexString(characteristic.getValue());
                } else if (status == BluetoothGatt.GATT_READ_NOT_PERMITTED) {
                    msg = "Read not permitted for " + characteristic.getUuid().toString();
                } else {
                    msg = "Read failed for " + characteristic.getUuid().toString();
                }
                addLog(msg);
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                super.onCharacteristicWrite(gatt, characteristic, status);
            }

            //when enable notify or indication
            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] result = characteristic.getValue();

                addLog("______Value Changed Start_______");
                for (byte b : result) {
                    addLog(String.format("0x%20x", b));
                }
                addLog("______Value Changed End_______");
                super.onCharacteristicChanged(gatt, characteristic);
            }
        };
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        adSettings = new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                addLog("Found BLE device! Name: " + result.getDevice().getName()
                        + ", address:" + result.getDevice().getAddress()
                );
                if (result.getDevice().getName() != null && result.getDevice().getName().equals("WearDev")) {
                    addLog("Sensor Device Found. Name:" + result.getDevice().getName() + " Address:" + result.getDevice().getAddress());
                    if (isScanning) {
                        isScanning = false;
                        addLog("Stop scanning.");
                        StopScanning();// if device find, stop scanning and connect.
                        context.btnConnect.setEnabled(true);
                    }
                    sensor = result.getDevice();
                    addLog("Connecting to device...");
                    sensor.connectGatt(context, true, gattCallback);
                }
                super.onScanResult(callbackType, result);
            }
        };
    }

    public void StartScanning() {
        if (!isScanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                    context.btnConnect.setEnabled(true);
                }
            }, SCAN_PERIOD);
            isScanning = true;
            context.btnConnect.setEnabled(false);
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    public void StopScanning() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(scanCallback);
            }
        });
        isScanning = false;
    }

    public void getCharacteristic(int selected) {
        BluetoothGattService service = servicelist.get(selected);
        characteristicslist = service.getCharacteristics();
        context.characteristicList.clear();
        for (BluetoothGattCharacteristic characteristic : characteristicslist) {
            context.characteristicList.add(characteristic.getUuid().toString());
        }
        //refresh characteristics spinner only.
        context.refresh(false);
    }

    public void disconnect() {
        if (sensor != null && bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    public void readData(Integer index) {

        bluetoothGatt.readCharacteristic(characteristicslist.get(index));
        BluetoothGattCharacteristic characteristic = characteristicslist.get(index);
        if (characteristic.getUuid().toString().equals(context.getResources().getString(R.string.MEMS_DATA))) {
            addLog("service1_data selected");
            if(!notifyEnable(characteristic)){
                addLog("Failed to enable Notify");
                return;
            }
        } else if (characteristic.getUuid().toString().equals(context.getResources().getString(R.string.MEMS_CONF))) {
            addLog("service1_conf selected");
        } else if (characteristic.getUuid().toString().equals(context.getResources().getString(R.string.MEMS_POW))) {
            addLog("service1_pow selected");
            if(!indicateEnable(characteristic)){
                addLog("Failed to enable indicate");
                return;
            }
        }
    }

    public void writeData(Integer index) {
        BluetoothGattCharacteristic characteristic = characteristicslist.get(index);
        if (characteristic.getUuid().toString().equals(context.getResources().getString(R.string.MEMS_CONF))) {

            if (writeCharacteristic(characteristic, context.createConfig(), null)) {
                addLog("Write Data to characteristics success");
            }
        } else if (characteristic.getUuid().toString().equals(context.getResources().getString(R.string.DFU_NOBOND))) {
            byte[] data = {0x01};
            if(!indicateEnable(characteristic)){
                addLog("Failed to enable indicate");
                return;
            }
            if (writeCharacteristic(characteristic, null, data)) {
                addLog("Write Data to characteristics success");
            }
        }

    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String stringdata, byte[] bytedata) {
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (stringdata == null) {
            characteristic.setValue(bytedata);
        } else {
            characteristic.setValue(stringdata);
        }
        return bluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean notifyEnable(BluetoothGattCharacteristic characteristic) {
        addLog("Characteristics properties:" + characteristic.getProperties());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("CCC_DESCRIPTOR_UUID"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        addLog("Check notify enabled.");
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            addLog("Notify Disabled. enable now");
            if (bluetoothGatt.setCharacteristicNotification(characteristic, true)) {
                addLog("Notify Enabled. write descriptor with enable indication");
                if (bluetoothGatt.writeDescriptor(descriptor)) {
                    addLog("Write descriptor success");
                    return true;
                }
            }
        }
        return false;
    }

    public boolean indicateEnable(BluetoothGattCharacteristic characteristic) {
        addLog("Characteristics properties:" + characteristic.getProperties());
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("CCC_DESCRIPTOR_UUID"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        addLog("Check notify enabled.");
        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            addLog("Notify Disabled. enable now");
            if (bluetoothGatt.setCharacteristicNotification(characteristic, true)) {
                addLog("Notify Enabled. write descriptor with enable indication");
                if (bluetoothGatt.writeDescriptor(descriptor)) {
                    addLog("Write descriptor success");
                    return true;
                }
            }
        }
        return false;
    }

    public void addLog(String msg) {
        Log.e(TAG, msg);
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AddLog(msg);
            }
        });
    }
}
