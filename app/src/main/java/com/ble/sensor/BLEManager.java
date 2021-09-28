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
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    public ArrayList<BluetoothDevice> scannedDevices = new ArrayList<>();

    public void initScanner() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
//        scanner = BluetoothLeScannerCompat.getScanner();
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
        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
//        adSettings = new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
//                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
//                .build();
        scanCallback = new ScanCallback() {
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                addLog("onBatchScanResults. size:" + results.size());
                for (ScanResult result : results) {
                    BluetoothDevice device = result.getDevice();
                    if (device != null) {
                        String address = device.getAddress();
                        if (result.getScanRecord() != null) {
                            if (result.getScanRecord().getBytes() != null) {
                                Log.e(TAG, "--------scanned device record data start---------");
                                for (byte item : result.getScanRecord().getBytes()) {
                                    Log.e(TAG, "databatch:" + String.format("0x%20x", item));
                                    addLog("databatch:" + String.format("0x%20x", item));
                                }
                                Log.e(TAG, "--------scanned device record data end---------");
                            }
                            else {
                                addLog("Record.getbyte() null");
                            }
                            return;
                        }
                        else{
                            addLog("Record null");
                        }
                        final BleAdvertisedData badata = BleUtil.parseAdertisedData(result.getScanRecord().getBytes());
                        String devicename = result.getDevice().getName();
                        addLog("name from result " + devicename);
                        if (devicename == null) {
                            devicename = result.getScanRecord().getDeviceName();
                            addLog("name from record " + devicename);
                        }
                        if (devicename == null) {
                            addLog("name from record also null");
                            devicename = badata.getName();
                            addLog("name from byte data:" + devicename);
                        }
//                        String name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
                        addLog("Bluetooth Device Found. Name:" + devicename + " Address:" + address);
                        if (devicename != null && devicename.equals("WearDev")) {
                            addLog("Sensor Device Found. Name:" + devicename + " Address:" + address);
                            if (isScanning) {
                                isScanning = false;
                                addLog("Stop scanning.");
                                StopScanning();// if device find, stop scanning and connect.
                                context.btnConnect.setEnabled(true);
                            }
                            sensor = device;
                            addLog("Connecting to device...");
                            sensor.connectGatt(context, true, gattCallback);
                            break;
                        }
                    }
                }
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                addLog("Found device in onScanResult");
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    String address = device.getAddress();
                    if (result.getScanRecord() != null) {
                        if (result.getScanRecord().getBytes() != null) {
                            Log.e(TAG, "--------scanned device record data start---------");
                            for (byte item : result.getScanRecord().getBytes()) {
                                Log.e(TAG, "data:" + String.format("0x%20x", item));
                                addLog("data:" + String.format("0x%20x", item));
                            }
                            Log.e(TAG, "--------scanned device record data end---------");
                        } else {
                            addLog("Record.getbyte() null");
                        }
                        return;
                    } else {
                        addLog("Record null");
                    }
                    final BleAdvertisedData badata = BleUtil.parseAdertisedData(result.getScanRecord().getBytes());
                    String devicename = result.getDevice().getName();
                    addLog("name from result " + devicename);
                    if (devicename == null) {
                        devicename = result.getScanRecord().getDeviceName();
                        addLog("name from record " + devicename);
                    }
                    if (devicename == null) {
                        addLog("name from record also null");
                        devicename = badata.getName();
                        addLog("name from byte data:" + devicename);
                    }
//                        String name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
                    addLog("Bluetooth Device Found. Name:" + devicename + " Address:" + address);
                    if (devicename != null && devicename.equals("WearDev")) {
                        addLog("Sensor Device Found. Name:" + devicename + " Address:" + address);
                        if (isScanning) {
                            isScanning = false;
                            addLog("Stop scanning.");
                            StopScanning();// if device find, stop scanning and connect.
                            context.btnConnect.setEnabled(true);
                        }
                        sensor = device;
                        addLog("Connecting to device...");
                        sensor.connectGatt(context, true, gattCallback);
                    }
                }
                else addLog("scanned device null");
                super.onScanResult(callbackType, result);
            }
        };

//            @Override
//            public void onBatchScanResults(@NonNull List<ScanResult> results) {
//                addLog("onBatchScanResults. size:" + results.size());
//                for (no.nordicsemi.android.support.v18.scanner.ScanResult result : results) {
//                    BluetoothDevice device = result.getDevice();
//                    if (device != null) {
//                        String address = device.getAddress();
//                        if (result.getScanRecord() != null) {
//                            if (result.getScanRecord().getBytes() != null) {
//                                Log.e(TAG, "--------scanned device record data start---------");
//                                for (byte item : result.getScanRecord().getBytes()) {
//                                    Log.e(TAG, "data:" + String.format("0x%20x", item));
//                                    addLog("data:" + String.format("0x%20x", item));
//                                }
//                                Log.e(TAG, "--------scanned device record data end---------");
//                            }
//                            else {
//                                addLog("Record.getbyte() null");
//                            }
//                            return;
//                        }
//                        else{
//                            addLog("Record null");
//                        }
//                        final BleAdvertisedData badata = BleUtil.parseAdertisedData(result.getScanRecord().getBytes());
//                        String devicename = result.getDevice().getName();
//                        addLog("name from result " + devicename);
//                        if (devicename == null) {
//                            devicename = result.getScanRecord().getDeviceName();
//                            addLog("name from record " + devicename);
//                        }
//                        if (devicename == null) {
//                            addLog("name from record also null");
//                            devicename = badata.getName();
//                            addLog("name from byte data:" + devicename);
//                        }
////                        String name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
//                        addLog("Bluetooth Device Found. Name:" + devicename + " Address:" + address);
//                        if (devicename != null && devicename.equals("WearDev")) {
//                            addLog("Sensor Device Found. Name:" + devicename + " Address:" + address);
//                            if (isScanning) {
//                                isScanning = false;
//                                addLog("Stop scanning.");
//                                StopScanning();// if device find, stop scanning and connect.
//                                context.btnConnect.setEnabled(true);
//                            }
//                            sensor = device;
//                            addLog("Connecting to device...");
//                            sensor.connectGatt(context, true, gattCallback);
//                            break;
//                        }
//                    }
//                }
//                super.onBatchScanResults(results);
//            }
//        };
//    }


//            else {
//                Log.e(TAG, "Name from Scanned record" + result.getScanRecord().getDeviceName());
//                addLog("Name from Scanned record:" + result.getScanRecord().getDeviceName());
//                addLog("Address from Scanned record:" + result.getDevice().getAddress());
//                Log.e(TAG, "--------scanned device record data start---------");
//                for (byte item : result.getScanRecord().getBytes()) {
//                    Log.e(TAG, "data:" + String.format("0x%20x", item));
//                }
//                Log.e(TAG, "--------scanned device record data end---------");
//                boolean isnew =true;
//                for(BluetoothDevice device:scannedDevices){
//                    if(device.getAddress().equals(result.getDevice().getAddress())){
//                        isnew = false;
//                        Log.e(TAG,"scanned douplicated device");
//                        Toast.makeText(context,"scanned douplicated device", Toast.LENGTH_LONG).show();
//                        break;
//                    }
//                }
//                if(isnew) {
//                scannedDevices.add(result.getDevice());
//                final BleAdvertisedData badata = BleUtil.parseAdertisedData(result.getScanRecord().getBytes());
//                String devicename = result.getDevice().getName();
//                if (devicename == null) {
//                    devicename = result.getScanRecord().getDeviceName();
//                }
//                if (devicename == null) {
//                    addLog("name from record also null");
//                    devicename = badata.getName();
//                    addLog("name from byte data:" + devicename);
//                }
//                addLog("Found BLE device! Name: " + devicename
//                        + ", address:" + result.getDevice().getAddress()
//                );
//                if (devicename.equals("WearDev")) {
//                    addLog("Sensor Device Found. Name:" + result.getDevice().getName() + " Address:" + result.getDevice().getAddress());
//                    if (isScanning) {
//                        isScanning = false;
//                        addLog("Stop scanning.");
//                        StopScanning();// if device find, stop scanning and connect.
//                        context.btnConnect.setEnabled(true);
//                    }
//                    sensor = result.getDevice();
//                    addLog("Connecting to device...");
//                    sensor.connectGatt(context, true, gattCallback);
//                }
////                }
//            }
//            super.onScanResult(callbackType, result);
//        }
//    };
    }
    public void StartScanning() {
        if (!isScanning) {
            // Stops scanning after a predefined scan period.
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    isScanning = false;
//                    scanner.stopScan(scanCallback);
//                    context.btnConnect.setEnabled(true);
//                }
//            }, SCAN_PERIOD);
            addLog("Scan start");
            isScanning = true;
            context.btnConnect.setEnabled(false);
            scannedDevices.clear();
            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
            addLog("Scan started");
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
            if (!notifyEnable(characteristic)) {
                addLog("Failed to enable Notify");
                return;
            }
        } else if (characteristic.getUuid().toString().equals(context.getResources().getString(R.string.MEMS_CONF))) {
            addLog("service1_conf selected");
        } else if (characteristic.getUuid().toString().equals(context.getResources().getString(R.string.MEMS_POW))) {
            addLog("service1_pow selected");
            if (!indicateEnable(characteristic)) {
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
            if (!indicateEnable(characteristic)) {
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


    static final public class BleUtil {
        private final String TAG = BleUtil.class.getSimpleName();

        public static BleAdvertisedData parseAdertisedData(byte[] advertisedData) {
            List<UUID> uuids = new ArrayList<UUID>();
            String name = null;
            if (advertisedData == null) {
                return new BleAdvertisedData(uuids, name);
            }
            ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
            while (buffer.remaining() > 2) {
                byte length = buffer.get();
                if (length == 0) break;
                byte type = buffer.get();
                switch (type) {
                    case 0x02: // Partial list of 16-bit UUIDs
                    case 0x03: // Complete list of 16-bit UUIDs
                        while (length >= 2) {
                            uuids.add(UUID.fromString(String.format(
                                    "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                            length -= 2;
                        }
                        break;
                    case 0x06: // Partial list of 128-bit UUIDs
                    case 0x07: // Complete list of 128-bit UUIDs
                        while (length >= 16) {
                            long lsb = buffer.getLong();
                            long msb = buffer.getLong();
                            uuids.add(new UUID(msb, lsb));
                            length -= 16;
                        }
                        break;
                    case 0x09:
                        byte[] nameBytes = new byte[length - 1];
                        buffer.get(nameBytes);
                        try {
                            name = new String(nameBytes, "utf-8");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        buffer.position(buffer.position() + length - 1);
                        break;
                }
            }
            return new BleAdvertisedData(uuids, name);
        }
    }


    public static class BleAdvertisedData {
        private List<UUID> mUuids;
        private String mName;

        public BleAdvertisedData(List<UUID> uuids, String name) {
            mUuids = uuids;
            mName = name;
        }

        public List<UUID> getUuids() {
            return mUuids;
        }

        public String getName() {
            return mName;
        }
    }
}
