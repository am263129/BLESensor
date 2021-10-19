package com.petvoice.logger;

import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_SIGNED;


import static com.petvoice.logger.Util.csvWriterClose;
import static com.petvoice.logger.Util.zeroPrint;

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
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.daasuu.camerarecorder.CameraRecordListener;
import com.daasuu.camerarecorder.CameraRecorder;
import com.daasuu.camerarecorder.CameraRecorderBuilder;
import com.daasuu.camerarecorder.LensFacing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeviceControlActivity extends AppCompatActivity {

    private BluetoothDevice wearDev;
    private String sensorAddress;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothManager bluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private Handler connectHandler = new Handler();

    private final ArrayList<LogItem> logarray = new ArrayList<>();
    private LogAdapter logAdapter;
    protected LinearLayoutManager mLayoutManager;

    private BluetoothGattCharacteristic memsData;
    private BluetoothGattCharacteristic memsConf;


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
    private TextView labelRecord;

    private int selected = 0;
    private final String TAG = "ControlActivity";


    private SampleGLView sampleGLView;
    protected CameraRecorder cameraRecorder;
    protected LensFacing lensFacing = LensFacing.BACK;
    protected int cameraWidth = 864;
    protected int cameraHeight = 480;
    protected int videoWidth = 864;
    protected int videoHeight = 480;
    private boolean toggleClick = false;
    private int CONNECTION_STATUS = BluetoothProfile.STATE_CONNECTING;
    private boolean saveLog = false;
    private boolean saveCsv = true;
    private boolean cameraMode = false;
    private boolean showLog = true;
    private boolean recording = false;
    private String recordingTime;
    private long starttime = 0;
    private int deviceWidth;

    Handler testHanler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            CONNECTION_STATUS = BluetoothProfile.STATE_DISCONNECTED;
            btnRecord.performClick();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Util.initLogWriter();
        initUi();
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
        Log.d(TAG, "onStop");
        Util.logWriterClose();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED) {
            mBluetoothGatt.disconnect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setUpCamera();
        initBluetooth();
        Log.d(TAG, "onStart");
    }
    /* remove auto sync screen on Petvoice version 1.2
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initUi();
        setUpCamera();
    }
    */


    public void initUi() {
        Camera mcamera = Camera.open();
        List<Camera.Size> list = getSupportedVideoSizes(mcamera);
        for(Camera.Size item:list){
            Log.e("Support list",item.height +":"+ item.width);
        }
//        setContentView(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? R.layout.activity_device_control_landscape : R.layout.activity_device_control_portrait);
        setContentView(R.layout.activity_device_control_landscape);
        logList = findViewById(R.id.result);
        icoConnect = findViewById(R.id.ico_connect);
        cameraView = findViewById(R.id.cameraView);
        btnRecord = findViewById(R.id.btnRecord);
        deviceName = findViewById(R.id.deviceName);
        btnLog = findViewById(R.id.btn_log);
        btnCamera = findViewById(R.id.btn_showcamera);
        iconRecord = findViewById(R.id.iconToogle);
        btnConnect = findViewById(R.id.btn_connect);
        labelRecord = findViewById(R.id.label_record);
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
        sensorAddress = getIntent().getStringExtra("address") == null ? "" : getIntent().getStringExtra("address");
        deviceName.setText(sensorAddress);
        labelRecord.setText(recording ? getString(R.string.label_stop) : getString(R.string.label_start));
    }

    /**
     * set free camera, when finish app or after recording finished.
     */

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

    /**
     * setup camera view, for preview video recording.
     */

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

    /**
     * setup camera. this called everytime after pause or stop. or record video.
     */
    private void setUpCamera() {
        setUpCameraView();
        cameraRecorder = new CameraRecorderBuilder(this, sampleGLView)
                //.recordNoFilter(true)
                .cameraRecordListener(new CameraRecordListener() {
                    @Override
                    public void onGetFlashSupport(boolean flashSupport) {
                    }

                    @Override
                    public void onRecordComplete() {
                        Util.exportMp4ToGallery(getApplicationContext());
                    }

                    @Override
                    public void onRecordStart() {

                    }

                    @Override
                    public void onError(Exception exception) {
                        exception.printStackTrace();
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

    public List<Camera.Size> getSupportedVideoSizes(Camera camera) {
        if (camera.getParameters().getSupportedVideoSizes() != null) {
            return camera.getParameters().getSupportedVideoSizes();
        } else {
            // Video sizes may be null, which indicates that all the supported
            // preview sizes are supported for video recording.
            return camera.getParameters().getSupportedPreviewSizes();
        }
    }

    /**
     * Start/Stop record video
     *
     * @param view record button
     */
    public void recordVideo(View view) {

        if (recording) {
            recording = false;
            csvWriterClose();
            cameraRecorder.stop();
            stopReadMemsData();
            Toast.makeText(
                    this,
                    "Saving. mp4 file path:" + Util.getMp4FileName() + "\n csv file path:" + Util.getCsvFileName(),
                    Toast.LENGTH_LONG
            ).show();
        } else {
//            //device is connected but memsdata is null.
//            if (memsData == null) {
//                Toast.makeText(
//                        this,
//                        "Unable to read mems data. try reconnect to sensor.",
//                        Toast.LENGTH_SHORT
//                ).show();
//                addLog("MEMS_DATA undefined.");
//                return;
//            }
//            //if device is not connected, unable to record.
//            if (CONNECTION_STATUS != BluetoothProfile.STATE_CONNECTED) {
//                Toast.makeText(
//                        this,
//                        "Connection Lost.",
//                        Toast.LENGTH_SHORT
//                ).show();
//                addLog("Connection lost.");
//                return;
//            }
            starttime = System.currentTimeMillis();
            recording = true;
            Util.initCsvWriter();
            Log.e(TAG, Util.getMp4FilePath());
            cameraRecorder.start(Util.getMp4FilePath());
//            startReadMemsData(memsData);
            testHanler.sendEmptyMessageDelayed(0,5000);
        }
        iconRecord.setBackgroundResource(recording ? R.drawable.ic_stop : R.drawable.ic_record);
        labelRecord.setText(recording ? getString(R.string.label_stop) : getString(R.string.label_start));
    }

    public void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        try {
            wearDev = mBluetoothAdapter.getRemoteDevice(sensorAddress);
            if (wearDev == null) {
                Toast.makeText(this, getString(R.string.msg_no_sensor_device), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                if (CONNECTION_STATUS != BluetoothProfile.STATE_CONNECTED)
                    btnConnect.performClick();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateUI() {
        icoConnect.setBackgroundResource(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED ? R.drawable.ic_disconnect : R.drawable.ic_connect);
        btnConnect.setEnabled(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED || CONNECTION_STATUS == BluetoothProfile.STATE_DISCONNECTED);
        connectingProgress.setVisibility(CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED || CONNECTION_STATUS == BluetoothProfile.STATE_DISCONNECTED ? View.GONE : View.VISIBLE);
    }

    /**
     * show setting dlg.
     *
     * @param view setting button on header bar
     */
    public void showSetting(View view) {
        configDlg.show();
    }

    public void closeDlg(View view) {
        if (configDlg.isShowing()) configDlg.dismiss();
    }

    /**
     * Show/Hide camera view.
     *
     * @param view camera button on header bar
     */
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

    /**
     * Hide/Show log view.
     *
     * @param view log visible button on header bar
     */
    public void toggleLog(View view) {
        showLog = !showLog;
        btnLog.setBackgroundResource(showLog ? R.drawable.back_active : R.drawable.back_purple);
        logList.setVisibility(showLog ? View.VISIBLE : View.GONE);
    }


    /**
     * write setting config to sensor device.
     *
     * @param view send button of config dialog.
     */

    public void confirmSetting(View view) {
        if (configDlg.isShowing()) configDlg.dismiss();
        String config = createConfig();
        byte[] single = {0x01};
        saveLog = chk_log.isChecked();
        saveCsv = chk_csv.isChecked();

        addLog("send setting config to sensor");
        if (memsConf == null) {
            addLog("MEMS_CONF undefined");
            return;
        }
        Log.e(TAG, memsConf.getProperties() + "");
        int writeType = memsConf.getProperties();
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
        if ((memsConf.getProperties() & writeProperty) == 0) {
            addLog(getString(R.string.msg_cha_write_permission_denied));
            addLog("character uuid: " + memsConf + ", prermission: " + memsConf.getProperties());
        } else {
            memsConf.setValue(config);
            memsConf.setWriteType(WRITE_TYPE_NO_RESPONSE);
            boolean write = mBluetoothGatt.writeCharacteristic(memsConf);
            addLog(write ? getString(R.string.msg_writing_config) : getString(R.string.msg_write_failed));
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
        addLog("config read: " + Util.bytesToHex(data));
    }


    /**
     * connect to sensor device
     *
     * @param view connect button on header bar
     */
    public void Connect(View view) {
        if(wearDev==null){
            Toast.makeText(this,"Device not found.",Toast.LENGTH_SHORT).show();
            return;
        }
        if (CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTED) {
            addLog(getString(R.string.msg_disconnecting));
            stopReadMemsData();
            mBluetoothGatt.disconnect();
            memsConf = null;
            memsData = null;
            CONNECTION_STATUS = BluetoothProfile.STATE_DISCONNECTING;
        } else {
            addLog(getString(R.string.msg_connecting));
            CONNECTION_STATUS = BluetoothProfile.STATE_CONNECTING;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothGatt = wearDev.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = wearDev.connectGatt(this, false, mGattCallback);
            }
            connectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //If still connecting after 5 sec, update ui, and show as unable to connect;
                    if (CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTING) {
                        addLog(getString(R.string.msg_no_response));
                        CONNECTION_STATUS = BluetoothProfile.STATE_DISCONNECTED;
                        updateUI();
                    }
                }
            }, 8000);
        }
        updateUI();
    }

    /**
     * read data from sensor with selected characteristic.
     *
     * @param view read button of config dialog
     */
    public void Read(View view) {
        if ((memsData.getProperties()
                & BluetoothGattCharacteristic.PROPERTY_READ) == 0)
            addLog(getString(R.string.msg_unable_read));
        else {
            if (CONNECTION_STATUS != BluetoothProfile.STATE_CONNECTED) {
                Toast.makeText(this, "Sensor disconnected. please connect device", Toast.LENGTH_SHORT).show();
                addLog("Sensor disconnected. please connect device");
                return;
            }
            //read data without recording.
            if (memsData != null && !recording)
                startReadMemsData(memsData);
            else {
                addLog(getString(R.string.msg_undefined_memsdata));
            }
//            else if(target.equals("MEMS_CONF")){
//                addLog(getString(R.string.msg_reading_config));
//                boolean read = mBluetoothGatt.readCharacteristic(bluetoothGattCharacteristics.get(selected));
//                addLog(read ? getString(R.string.msg_reading_config_data) : getString(R.string.msg_reading_config_data_failed));
//            }
        }
        if (configDlg.isShowing()) configDlg.dismiss();
    }


    /**
     * @param bytes reading data
     */
    public void dspReadCharacteristic(byte[] bytes) {
        int len = 1;
        for (int i = 0; i < len; i++) {
            float[] accel = Util.Half(bytes, (i * 20) + (0 * 6 + 1));
            float[] gyro = Util.Half(bytes, (i * 20) + (1 * 6 + 1));
            float[] compass = Util.Half(bytes, (i * 20) + (2 * 6 + 1));

            addLog("Accel -- x:" + zeroPrint(accel[0]) + " y:" + zeroPrint(accel[1]) + " z:" + zeroPrint(accel[2]));
            addLog("Gyro  -- x:" + zeroPrint(gyro[0]) + " y:" + zeroPrint(gyro[1]) + " z:" + zeroPrint(gyro[2]));
            addLog("Compass  -- x:" + zeroPrint(compass[0]) + " y:" + zeroPrint(compass[1]) + " z:" + zeroPrint(compass[2]));
            if (saveCsv) {
                long millis = System.currentTimeMillis() - starttime;
                recordingTime = String.valueOf((float) millis/1000f);
                Util.exportData(recordingTime, accel, gyro, compass);
            }
        }
    }


    public void startReadMemsData(BluetoothGattCharacteristic memsdata) {
        addLog(getString(R.string.msg_set_notify_enable));
        boolean result = mBluetoothGatt.setCharacteristicNotification(memsdata, true);
        addLog("set setCharacteristicNotification result: " + result);
        UUID uuid = UUID.fromString(getString(R.string.config_UUID));
        BluetoothGattDescriptor descriptor = memsdata.getDescriptor(uuid);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean descriptor_result = mBluetoothGatt.writeDescriptor(descriptor);
            addLog("Set writeDescriptor result(enable): " + descriptor_result);
        } else {
            addLog(getString(R.string.msg_undefined_descriptor));
        }
    }

    public void stopReadMemsData() {
        if (memsData == null) {
            addLog(getString(R.string.msg_stop_failed_undefined_memsdata));
            return;
        }
        boolean result = mBluetoothGatt.setCharacteristicNotification(memsData, false);
        addLog("set setCharacteristicNotification result: " + result);
        UUID uuid = UUID.fromString(getString(R.string.config_UUID));
        BluetoothGattDescriptor descriptor = memsData.getDescriptor(uuid);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean descriptor_result = mBluetoothGatt.writeDescriptor(descriptor);
            addLog("Set writeDescriptor result(disable): " + descriptor_result);
        } else {
            addLog("Descriptor of MEMS_DATA null");
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
                addLog(getString(R.string.msg_connected));
                boolean retVal = mBluetoothGatt.discoverServices();
                // Attempts to discover services after successful connection.
                Log.e(TAG, getString(R.string.msg_getting_services) + retVal);
                CONNECTION_STATUS = BluetoothProfile.STATE_CONNECTED;
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
                addLog("Device is unable to communicate due to unpaired");
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    // The broadcast receiver should be called.
                    addLog("Request Bond");
                } else {
                    addLog("already bond");
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (CONNECTION_STATUS == BluetoothProfile.STATE_CONNECTING || CONNECTION_STATUS == BluetoothProfile.STATE_DISCONNECTED) {
                    addLog(getString(R.string.msg_connect_failed));
                } else
                    addLog(getString(R.string.msg_disconnected));
                CONNECTION_STATUS = BluetoothProfile.STATE_DISCONNECTED;
                //auto stop recording if disconnected from sensor.
                if(recording){
                    addLog("Connection lost, Stop recording...");
                    btnRecord.performClick();
                }
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                addLog(getString(R.string.msg_connecting));
            }
            DeviceControlActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
                addLog(bluetoothGattServices.size() + " " + getString(R.string.msg_service_found));
                for (BluetoothGattService service : bluetoothGattServices) {
                    String newService = SensorUUID.lookup(service.getUuid().toString(), service.getUuid().toString());
                    addLog("Service" +
                            " UUID " + newService);
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String name = SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString());
                        if (name.equals("MEMS_DATA")) {
                            memsData = characteristic;
                        } else if (name.equals("MEMS_CONF")) {
                            memsConf = characteristic;
                        }
                    }
                    if (memsData != null && memsConf != null) {
                        addLog("Sensor device is ready");
                    }
                }
            } else {
                addLog(getString(R.string.msg_service_getting_failed));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                addLog("Read data:" + Util.bytesToHex(characteristic.getValue()));
                if (SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()).equals("MEMS_CONF")) {
                    addLog(getString(R.string.msg_read_setting) + Util.bytesToHex(characteristic.getValue()));
                }
            } else {
                addLog(getString(R.string.msg_reading_failed));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            addLog("data received from change listener:" + Util.bytesToHex(characteristic.getValue()));
            if (SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()).equals("MEMS_DATA")) {
                addLog("MEMS_DATA : " + Util.bytesToHex(characteristic.getValue()));
                dspReadCharacteristic(characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // stop/start record comes here. and disconnect will also comes here.
            }
            addLog("");
        }
    };
}