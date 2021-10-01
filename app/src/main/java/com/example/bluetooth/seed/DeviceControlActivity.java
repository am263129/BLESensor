package com.example.bluetooth.seed;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.daasuu.camerarecorder.CameraRecordListener;
import com.daasuu.camerarecorder.CameraRecorder;
import com.daasuu.camerarecorder.CameraRecorderBuilder;
import com.daasuu.camerarecorder.LensFacing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class DeviceControlActivity extends AppCompatActivity {

    private BluetoothDevice wearDev;
    private String sensorAddress;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothManager bluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private Handler connectHandler = new Handler();

    private List<BluetoothGattService> bluetoothGattServices = new ArrayList<>();
    private List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = new ArrayList<>();
    private final ArrayList<String> services = new ArrayList<>();
    private final ArrayList<String> characteristics = new ArrayList<>();
    private final ArrayList<LogItem> logarray = new ArrayList<>();
    private LogAdapter logAdapter;
    protected LinearLayoutManager mLayoutManager;


    private Spinner serviceSpinner, charSpinnner;
    public ImageView icoConnect, iconRecord;
    private TextView deviceName;
    private Dialog configDlg;
    private RecyclerView logList;
    private CheckBox chk_acell, chk_gyro, chk_compass, chk_temperature, chk_log, chk_csv;
    private FrameLayout cameraView;
    private ConstraintLayout btnRecord, btnLog, btnCamera, btnConnect;
    private ProgressBar connectingProgress;
    private MediaRecorder recorder;
    private SurfaceHolder mholder;
    private Camera camera;

    private final int selected = 0;
    private final String TAG = "ControlActivity";


    private SampleGLView sampleGLView;
    protected CameraRecorder cameraRecorder;
    private String filepath;
    private TextView recordBtn;
    protected LensFacing lensFacing = LensFacing.BACK;
    protected int cameraWidth = 1280;
    protected int cameraHeight = 720;
    protected int videoWidth = 720;
    protected int videoHeight = 720;
    private boolean toggleClick = false;

    private int CONNECTION_STATUS = BluetoothProfile.STATE_CONNECTING;

    private boolean notifyEnable = true;
    private boolean saveLog = true;
    private boolean saveCsv = true;
    private boolean cameraMode = false;
    private boolean showLog = true;
    private boolean recorderReady = false, recording = false;


    private int deviceWidth;
    private int deviceHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUi();
        initBluetooth();
    }

    @Override
    protected void onStop() {
        releaseCamera();
        super.onStop();
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        notifyEnable = false;
        if (CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED) {
            mBluetoothGatt.disconnect();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        notifyEnable = true;
        setUpCamera();
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void initUi() {
        setContentView(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? R.layout.activity_device_control_landscape : R.layout.activity_device_control_portrait);
        logList = findViewById(R.id.result);
        icoConnect = findViewById(R.id.ico_connect);
        cameraView = findViewById(R.id.cameraView);
        btnRecord = findViewById(R.id.btnRecord);
        deviceName = findViewById(R.id.deviceName);
        btnLog = findViewById(R.id.btn_log);
        btnCamera = findViewById(R.id.btn_showcamera);
        iconRecord = findViewById(R.id.iconToogle);
        btnConnect = findViewById(R.id.btn_connect);
        connectingProgress = findViewById(R.id.connecting_progress);
        mLayoutManager = new LinearLayoutManager(this);
        logList.setLayoutManager(mLayoutManager);
        logAdapter = new LogAdapter(logarray);
        logList.setAdapter(logAdapter);
        configDlg = new Dialog(this);
        configDlg.setContentView(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? R.layout.config_dlg_landscape : R.layout.config_dlg_portrait);
        configDlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        deviceWidth = size.x;
        deviceHeight = size.y;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(configDlg.getWindow().getAttributes());
        lp.width = (int) (deviceWidth * 0.9);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        configDlg.getWindow().setAttributes(lp);
        chk_acell = configDlg.findViewById(R.id.cbAccell);
        chk_gyro = configDlg.findViewById(R.id.cbGyro);
        chk_compass = configDlg.findViewById(R.id.cbCompass);
        chk_temperature = configDlg.findViewById(R.id.cbTemperature);
        chk_log = configDlg.findViewById(R.id.save_log);
        chk_csv = configDlg.findViewById(R.id.save_csv);
        serviceSpinner = configDlg.findViewById(R.id.spin_service);
        charSpinnner = configDlg.findViewById(R.id.spin_characteristics);
        serviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                syncCharacteristicsSpinner(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        sensorAddress = getIntent().getStringExtra("address") == null ? "" : getIntent().getStringExtra("address");
        deviceName.setText(getIntent().getStringExtra("name") == null ? "Unknown" : getIntent().getStringExtra("name"));
    }


    private void releaseCamera() {
        if (sampleGLView != null) {
            sampleGLView.onPause();
        }

        if (cameraRecorder != null) {
            cameraRecorder.stop();
            cameraRecorder.release();
            cameraRecorder = null;
        }

        if (sampleGLView != null) {
            ((FrameLayout) findViewById(R.id.cameraView)).removeView(sampleGLView);
            sampleGLView = null;
        }
    }

    private void setUpCameraView() {
        runOnUiThread(() -> {
            FrameLayout frameLayout = findViewById(R.id.cameraView);
            frameLayout.removeAllViews();
            sampleGLView = null;
            sampleGLView = new SampleGLView(getApplicationContext());
            sampleGLView.setTouchListener((event, width, height) -> {
                if (cameraRecorder == null) return;
                cameraRecorder.changeManualFocusPoint(event.getX(), event.getY(), width, height);
            });
            frameLayout.addView(sampleGLView);
        });
    }

    private void setUpCamera() {
        setUpCameraView();

        cameraRecorder = new CameraRecorderBuilder(this, sampleGLView)
                //.recordNoFilter(true)
                .cameraRecordListener(new CameraRecordListener() {
                    @Override
                    public void onGetFlashSupport(boolean flashSupport) {
                        runOnUiThread(() -> {
//                            findViewById(R.id.btn_flash).setEnabled(flashSupport);
                        });
                    }

                    @Override
                    public void onRecordComplete() {
                        Util.exportMp4ToGallery(getApplicationContext(), filepath);
                    }

                    @Override
                    public void onRecordStart() {

                    }

                    @Override
                    public void onError(Exception exception) {
                        Log.e("CameraRecorder", exception.toString());
                    }

                    @Override
                    public void onCameraThreadFinish() {
                        if (toggleClick) {
                            runOnUiThread(() -> {
                                setUpCamera();
                            });
                        }
                        toggleClick = false;
                    }
                })
                .videoSize(videoWidth, videoHeight)
                .cameraSize(cameraWidth, cameraHeight)
                .lensFacing(lensFacing)
                .build();


    }

    public void recordVideo(View view){
        if (!recording) {
            recording = true;
            filepath = Util.getOutputMediaFile(Util.MEDIA_TYPE_VIDEO);
            Log.e("TAG",filepath);
            cameraRecorder.start(filepath);
        } else {
            Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
            recording = false;
            cameraRecorder.stop();
        }
        iconRecord.setBackgroundResource(recording ? R.drawable.ic_stop : R.drawable.ic_record);
    }

    public void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        try {
            wearDev = mBluetoothAdapter.getRemoteDevice(sensorAddress);
            if (wearDev == null) {
                Toast.makeText(this, "Can't find Sensor device", Toast.LENGTH_SHORT).show();
                finish();
            }
            else{
                Connect(btnLog);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    public void updateUI() {
        icoConnect.setBackgroundResource(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED ? R.drawable.ic_disconnect : R.drawable.ic_connect);
        btnConnect.setEnabled(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED || CONNECTION_STATUS == BluetoothProfile.STATE_DISCONNECTED);
        connectingProgress.setVisibility(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED || CONNECTION_STATUS == BluetoothProfile.STATE_DISCONNECTED?View.GONE:View.VISIBLE);
    }

    public void showSetting(View view) {
        configDlg.show();
    }

    public void toggleCamera(View view) {
        cameraMode = !cameraMode;
        btnCamera.setSelected(cameraMode);
        cameraView.setVisibility(cameraMode ? View.VISIBLE : View.GONE);
        btnRecord.setVisibility(cameraMode ? View.VISIBLE : View.GONE);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) logList.getLayoutParams();
            params.topToBottom = cameraMode ? R.id.divider : R.id.header;
            logList.setLayoutParams(params);
        }
    }

    public void toggleLog(View view) {
        showLog = !showLog;
        btnLog.setBackgroundResource(showLog ? R.drawable.back_active : R.drawable.back_purple);
        logList.setVisibility(showLog ? View.VISIBLE : View.GONE);
    }

    public void closeDlg(View view) {
        if (configDlg.isShowing()) configDlg.dismiss();
    }

    public void confirmSetting(View view) {
        if (configDlg.isShowing()) configDlg.dismiss();
        String config = createConfig();
        byte[] single = {0x01};
        saveLog = chk_log.isChecked();
        saveCsv = chk_csv.isChecked();
        if (bluetoothGattCharacteristics.size() == 0) {
            addLog("unable to write setting");
            return;
        }
        addLog("send setting config to sensor");
        String target = SensorUUID.lookup(bluetoothGattCharacteristics.get(selected).getUuid().toString(), "other");
        if (!target.equals(getString(R.string.sensor_data)) || !target.equals(getString(R.string.no_bound))) {
            Toast.makeText(this, "Unable to send setting to sensor, please select correct characteristics.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.e(TAG, bluetoothGattCharacteristics.get(selected).getProperties() + "");
        int writeType = bluetoothGattCharacteristics.get(selected).getProperties();
        int writeProperty;
        switch (writeType) {
            case WRITE_TYPE_DEFAULT:
                writeProperty = PROPERTY_WRITE;
                break;
            case WRITE_TYPE_NO_RESPONSE:
                writeProperty = PROPERTY_WRITE_NO_RESPONSE;
                break;
            case WRITE_TYPE_SIGNED:
                writeProperty = PROPERTY_SIGNED_WRITE;
                break;
            default:
                writeProperty = 0;
                break;
        }
        if ((bluetoothGattCharacteristics.get(selected).getProperties() & writeProperty) == 0) {
            addLog("Unable to send data to sensor. permission error");
            addLog("character uuid: " + bluetoothGattCharacteristics.get(selected) + ", prermission: " + bluetoothGattCharacteristics.get(selected).getProperties());
        } else {
            if (target.equals("MEMS_CONF")) {
                bluetoothGattCharacteristics.get(selected).setValue(config);
            } else {
                bluetoothGattCharacteristics.get(selected).setValue(single);
            }
            bluetoothGattCharacteristics.get(selected).setWriteType(WRITE_TYPE_NO_RESPONSE);
            boolean read = mBluetoothGatt.readCharacteristic(bluetoothGattCharacteristics.get(selected));
            addLog(read ? "Reading Characteristic" : "Reading Characteristic failed");
        }
        if (configDlg.isShowing()) configDlg.dismiss();
    }

    /**
     * Create config file with user setting.
     *
     * @return string config
     */
    public String createConfig() {
        String config = (chk_acell.isChecked() ? "AA" : "55") + " "
                + (chk_gyro.isChecked() ? "AA" : "55") + " "
                + (chk_compass.isChecked() ? "AA" : "55") + " "
                + (chk_temperature.isChecked() ? "AA" : "55") + " "
                + Integer.toString(20, 16) + " "
                + Integer.toString(200, 16);
        addLog("config : " + config);
        return config;
    }

    /**
     * set config from sensor device.
     *
     * @return string config
     */
    public void syncConfig(byte[] data) {
        String config = (chk_acell.isChecked() ? "AA" : "55") + " "
                + (chk_gyro.isChecked() ? "AA" : "55") + " "
                + (chk_compass.isChecked() ? "AA" : "55") + " "
                + (chk_temperature.isChecked() ? "AA" : "55") + " "
                + Integer.toString(20, 16) + " "
                + Integer.toString(200, 16);
        addLog("config : " + config);
    }


    private void syncConnectButton() {
        DeviceControlActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                updateUI();
            }
        });
    }

    public void Connect(View view) {
        if (CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED) {
            addLog("Disconnecting from sensor...");
            mBluetoothGatt.disconnect();
            CONNECTION_STATUS = BluetoothProfile.STATE_DISCONNECTING;
            updateUI();
        } else {
            addLog("Connecting to sensor.");
            CONNECTION_STATUS = BluetoothProfile.STATE_CONNECTING;
            mBluetoothGatt = wearDev.connectGatt(this, false, mGattCallback);
            updateUI();
            connectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //If still connecting after 5 sec, update ui, and show as unable to connect;
                    if(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTING) {
                        addLog("No response, Connection failed");
                        CONNECTION_STATUS = BluetoothProfile.STATE_DISCONNECTED;
                        updateUI();
                    }
                }
            },8000);
        }
    }

    public void Read(View view) {
        if (bluetoothGattCharacteristics.size() == 0) {
            addLog("unable to read data");
            return;
        }
        addLog("reading data...");
        Log.e(TAG, bluetoothGattCharacteristics.get(selected).getProperties() + "");
        if ((bluetoothGattCharacteristics.get(selected).getProperties()
                & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
            addLog("Sensor data isn't readable");
        else {

            String target = SensorUUID.lookup(bluetoothGattCharacteristics.get(selected).getUuid().toString(), "other");
            switch (target) {
                case "TX":
                case "MEMS_DATA":
                    addLog("Set Notify Enabled");
                    mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristics.get(selected), true);
                    break;
                case "MEMS_CONF":
                    addLog("Reading config");
                    boolean read = mBluetoothGatt.readCharacteristic(bluetoothGattCharacteristics.get(selected));
                    addLog(read ? "Reading Characteristic" : "Reading Characteristic failed");

                    break;
                case "MEMS_POW":
                    addLog("Enable Indicate");
                    UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                    BluetoothGattDescriptor descriptor = bluetoothGattCharacteristics.get(selected).getDescriptor(uuid);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                    mBluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristics.get(selected), true);
//                    mBluetoothGatt.ind(bluetoothGattCharacteristics.get(selected), true);
                    break;
            }
        }
        if (configDlg.isShowing()) configDlg.dismiss();
    }


    public void syncServiceSpinner() {
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, services);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serviceSpinner.setAdapter(serviceAdapter);
    }

    public void syncCharacteristicsSpinner(int position) {
        bluetoothGattCharacteristics = bluetoothGattServices.get(position).getCharacteristics();
        characteristics.clear();
        for (BluetoothGattCharacteristic characteristic : bluetoothGattCharacteristics) {
            String name = SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString());
            characteristics.add(name);
            addLog("Enabled characteristics: "+name);
        }
        ArrayAdapter<String> characteristicsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, characteristics);
        charSpinnner.setAdapter(characteristicsAdapter);
    }

    public void dspReadCharacteristic(byte[] bytes) {
        int len = 1;
        for (int i = 0; i < len; i++) {
            float[] accel = Util.Half(bytes, (i * 20) + (0 * 6 + 1));
            float[] gyro = Util.Half(bytes, (i * 20) + (1 * 6 + 1));
            float[] compass = Util.Half(bytes, (i * 20) + (2 * 6 + 1));
            for (int j = 0; j < 3; j++) {
                addLog("Accel -- x:" + accel[0] + " y:" + accel[1] + " z:" + accel[2]);
                addLog("Gyro  -- x:" + gyro[0] + " y:" + gyro[1] + " z:" + gyro[2]);
                addLog("Compass  -- x:" + compass[0] + " y:" + compass[1] + " z:" + compass[2]);
            }
            if (saveCsv) {
                String path = Util.exportData(accel, gyro, compass);
                if(path.equals("Failed")){
                    Log.e(TAG, "Failed");
                }
                else {
                    Log.e(TAG, "Success, path:"+path);
                }
            }
        }
    }

    private void addLog(String msg) {
        DeviceControlActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logarray.add(new LogItem(msg));
                logAdapter.notifyDataSetChanged();
                logList.scrollToPosition(logarray.size() - 1);
            }
        });
        if (saveLog)
            Util.printLog(msg);
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //show setting dialog when connected
                showSetting(btnLog);
                addLog("Connected to Sensor Device.");
                // Attempts to discover services after successful connection.
                Log.e(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
                CONNECTION_STATUS = BluetoothProfile.STATE_CONNECTED;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTING || CONNECTION_STATUS == BluetoothProfile.STATE_DISCONNECTED) {
                    addLog("Connecting Failed");
                }else
                addLog("Disconnected from Sensor Device");
                CONNECTION_STATUS = BluetoothProfile.STATE_DISCONNECTED;

            } else if (newState == BluetoothProfile.STATE_CONNECTING){
                addLog("Connecting to Sensor Device.");
                CONNECTION_STATUS = BluetoothProfile.STATE_CONNECTING;
            }
            syncConnectButton();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Success onServicesDiscovered");
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
            bluetoothGattServices = gatt.getServices();
            services.clear();
            for (BluetoothGattService service : bluetoothGattServices) {
                boolean isnew = true;
                for (String uuid : services) {
                    if (uuid.equals(service.getUuid().toString()) || uuid.equals(SensorUUID.lookup(service.getUuid().toString(), service.getUuid().toString()))) {
                        isnew = false;
                        break;
                    }
                }
                if (isnew)
                    services.add(SensorUUID.lookup(service.getUuid().toString(), service.getUuid().toString()));
            }
            DeviceControlActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    syncServiceSpinner();
                }
            });

            addLog(bluetoothGattServices.size() + " services found.");
            for (BluetoothGattService service : bluetoothGattServices) {
                addLog( "UUID" + SensorUUID.lookup(service.getUuid().toString(), service.getUuid().toString()));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                addLog("Data :" + Util.bytesToHex(characteristic.getValue()));
                if(SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()).equals("MEMS_CONF")){
                    addLog("Read Setting :"+Util.bytesToHex(characteristic.getValue()));
                }
                else if (SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()).equals("TX") && notifyEnable) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    addLog("Register future notification when data change");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            dspReadCharacteristic(characteristic.getValue());
        }


    };
}