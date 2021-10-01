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

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class DeviceControlActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    BluetoothDevice wearDev;
    private String sensorAddress;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothLeScanner mBluetoothLeScanner;
    public BluetoothManager bluetoothManager;
    private BluetoothGatt mBluetoothGatt;

    private List<BluetoothGattService> bluetoothGattServices = new ArrayList<>();
    List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = new ArrayList<>();
    private final ArrayList<String> services = new ArrayList<>();
    private final ArrayList<String> characteristics = new ArrayList<>();
    private final ArrayList<LogItem> logarray = new ArrayList<>();
    private LogAdapter logAdapter;
    protected LinearLayoutManager mLayoutManager;

    private Queue<Runnable> commandQueue;
    private boolean commandQueueBusy;

    private Spinner serviceSpinner, charSpinnner;
    public ImageView icoConnect, iconRecord;
    private TextView deviceName;
    private Dialog configDlg;
    private RecyclerView logList;
    private CheckBox chk_acell, chk_gyro, chk_compass, chk_temperature, chk_log, chk_csv;
    private FrameLayout cameraView;
    private ConstraintLayout btnRecord, btnLog, btnCamera;
    private MediaRecorder recorder;
    private SurfaceHolder mholder;
    private Camera camera;
    private CameraPreview mPreview;
    private final int selected = 0;
    private final String TAG = "ControlActivity";


    private int CONNECTION_STATUS = -1;
    private final int CONNECTED = 1;
    private final int DISCONNECTED = -1;

    private boolean notifyEnable = true;
    private boolean saveLog = true;
    private boolean saveCsv = true;
    private boolean cameraMode = false;
    private boolean showLog = true;
    private boolean recorderReady = false, recording = false;
    ;

    private int deviceWidth;
    private int deviceHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUi();
        initCamera();
        initRecorder();
        initBluetooth();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        notifyEnable = false;
        if (CONNECTION_STATUS == CONNECTED) {
            mBluetoothGatt.disconnect();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        notifyEnable = true;
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

    public void initCamera() {
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        camera.unlock();
        mPreview = new CameraPreview(this, camera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraView);
        preview.addView(mPreview);
        mholder = mPreview.getHolder();
        mholder.addCallback(this);
        mholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        recorder = new MediaRecorder();
        recorder.setPreviewDisplay(mholder.getSurface());
    }

    /* init video recorder */
    private void initRecorder() {
        recorder.setCamera(camera);
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);
        recorder.setOrientationHint(90);
        // set the size of video.
        // If the size is not applicable then throw the media recorder stop
        // -19 error
        recorder.setOutputFile(Util.getOutputMediaFile(Util.MEDIA_TYPE_VIDEO));
//        recorder.setMaxDuration(1800000); // 30 mins
//        recorder.setMaxFileSize(500000000); // Approximately 500 megabytes
        recorderReady = true;
    }


    /* Prepare video recorder */
    private void prepareRecorder() {
        recorder.setPreviewDisplay(mholder.getSurface());
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

//        try {
//            wearDev = mBluetoothAdapter.getRemoteDevice(sensorAddress);
//            if (wearDev == null) {
//                Toast.makeText(this, "Can't find Sensor device", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
    }

    public void recordVideo(View view) {
        if (!recorderReady) {
            Toast.makeText(this, "Unable to capture video.", Toast.LENGTH_SHORT).show();
        } else {
            if (recording) {
                recorder.stop();
                recording = false;
                camera.lock();
                Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
                // Let's initRecorder so we can record again
                initCamera();
                initRecorder();
                prepareRecorder();
            } else {
                recording = true;
                recorder.start();
            }
            iconRecord.setBackgroundResource(recording ? R.drawable.ic_stop : R.drawable.ic_record);
        }
    }

    public void updateUI() {
        icoConnect.setBackgroundResource(CONNECTION_STATUS == CONNECTED ? R.drawable.ic_disconnect : R.drawable.ic_connect);
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


    private void syncConnectButton() {
        DeviceControlActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                updateUI();
            }
        });
    }

    public void Connect(View view) {
        if (CONNECTION_STATUS == CONNECTED) {
            mBluetoothGatt.disconnect();
        } else {
            mBluetoothGatt = wearDev.connectGatt(this, false, mGattCallback);
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
            boolean read = mBluetoothGatt.readCharacteristic(bluetoothGattCharacteristics.get(selected));
            addLog(read ? "Reading Characteristic" : "Reading Characteristic failed");
            String target = SensorUUID.lookup(bluetoothGattCharacteristics.get(selected).getUuid().toString(), "other");
            switch (target) {
                case "MEMS_DATA":
                    addLog("Read data");
                    break;
                case "MEMS_CONF":
                    addLog("Read config");
                    break;
                case "MEMS_POW":
                    addLog("Enable Indicate");
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
            characteristics.add(SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()));
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
                Util.exportData(accel, gyro, compass);
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

                addLog("Connected to Sensor Device.");
                // Attempts to discover services after successful connection.
                Log.e(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
                CONNECTION_STATUS = CONNECTED;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                CONNECTION_STATUS = DISCONNECTED;
                addLog("Disconnected from Sensor Device");
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
                Log.e(TAG, "UUID" + service.getUuid().toString());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                addLog("Data :" + new String(characteristic.getValue()));
                if (SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()).equals("TX") && notifyEnable) {
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


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (recorderReady)
            prepareRecorder();
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (mholder.getSurface() == null){
            // preview surface does not exist
            return;
        }








        try {
            camera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        try {
            camera.setPreviewDisplay(mholder);
            camera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        if (recorder != null)
            recorder.release();
    }
}