package com.ble.sensor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, AdapterView.OnItemSelectedListener {
    MediaRecorder recorder;
    SurfaceHolder holder;
    CameraModule camera;
    private int RECORD_AUDIO = 9001;
    private final int LOCATION = 9002;
    private final int BLUETOOTH = 9003;
    private final int CAMERA = 9004;
    boolean recording = false;

    private static com.ble.sensor.LogAdapter LogAdapter;
    private static ArrayList<LogItem> logList = new ArrayList<>();
    protected LinearLayoutManager mLayoutManager;

    private boolean bluetoothReady = false, recorderReady = false;
    private BLEManager manager;

    private ConstraintLayout root, settingArea, btnSetting;
    private CheckBox filterAccell, filterGyro, filterCompass, filterTemperature;
    private boolean enableAccell = false, enableGyro = false, enableCompass = false, enableTemperature = false;
    private EditText edtHz, edtTimes;
    public Button btnConnect;
    private static RecyclerView LogView;
    private static CheckBox enablesavefile;
    private ImageView iconToogle;
    private String logHistory = "";
    private Spinner services, characteristics;
    public List<String> serviceList = new ArrayList<>();
    public List<String> characteristicList = new ArrayList<>();

    public UUID selectedUuid;
    public int selectedcharacteristic = 0;


    private final String DEVTAG = "DEV-TAG";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        manager = new BLEManager(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RECORD_AUDIO);
        } else {
            Log.e(DEVTAG, "permission granted for record camera");
            initRecorder();
        }
        initUi();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e(DEVTAG, "Build Version:" + Build.VERSION.SDK_INT);
            checkPermission();
        } else {
            Log.e(DEVTAG, "check bluetooth 1");
            checkBluetooth();
        }

        //register bluetooth receiver
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    /* Initialize layout view */
    private void initUi() {
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root);
        settingArea = findViewById(R.id.control_area);
        btnSetting = findViewById(R.id.btnSetting);
        SurfaceView cameraView = (SurfaceView) findViewById(R.id.CameraView);
        holder = cameraView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        LogView = (RecyclerView) findViewById(R.id.sensorList);
        filterAccell = findViewById(R.id.cbAccell);
        filterGyro = findViewById(R.id.cbGyro);
        filterCompass = findViewById(R.id.cbCompass);
        filterTemperature = findViewById(R.id.cbTemperature);
        enablesavefile = findViewById(R.id.enableSaveFile);
        btnConnect = findViewById(R.id.btnConnect);
        edtHz = findViewById(R.id.edtHz);
        edtTimes = findViewById(R.id.edtTimes);
        iconToogle = findViewById(R.id.iconToogle);
        services = findViewById(R.id.serviceList);
        characteristics = findViewById(R.id.characteristicList);
        LogAdapter = new LogAdapter(logList);
        LogView.setAdapter(LogAdapter);
        mLayoutManager = new LinearLayoutManager(this);
        LogView.setLayoutManager(mLayoutManager);
        refresh(true);//refresh both spinner
    }

    /**
     * Reset spinners item when read service from ble device.
     *
     * @param flag if true, refresh both service and characteristics spinners.
     *             if false, refresh only characteristics spinner
     */
    public void refresh(boolean flag) {
        if (flag) {
            ArrayAdapter<String> serviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, serviceList);
            serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            services.setAdapter(serviceAdapter);
        }
        ArrayAdapter<String> charateristicsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, characteristicList);
        charateristicsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        characteristics.setAdapter(charateristicsAdapter);
    }

    /* ------- Video Recording part  Start -------- */

    /* init video recorder */
    private void initRecorder() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        camera = new CameraModule();
        try {
            if (cameraManager.getCameraIdList().length <= 0) {
                Log.e(DEVTAG, "No camera available");
                return;
            }
            for (String item : cameraManager.getCameraIdList()) {
                Log.e(DEVTAG, "Camera :" + item);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);
        File videofile = new File(Util.getOutputMediaFileUri(Util.MEDIA_TYPE_VIDEO));
        recorder.setOutputFile(videofile.getPath());
        recorder.setMaxDuration(50000); // 50 seconds
        recorder.setMaxFileSize(50000000); // Approximately 50 megabytes
        recorderReady = true;
    }

    public interface CameraSupport {
        CameraSupport open(int cameraId);

        int getOrientation(int cameraId);
    }

    public class CameraModule {

        CameraSupport provideCameraSupport() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new CameraNew(MainActivity.this);
            } else {
                return new CameraOld();
            }
        }
    }

    public class CameraOld implements CameraSupport {

        private Camera camera;

        @Override
        public CameraSupport open(final int cameraId) {
            this.camera = Camera.open(cameraId);
            return this;
        }

        @Override
        public int getOrientation(final int cameraId) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            return info.orientation;
        }

    }

    public class CameraNew implements CameraSupport {

        private CameraDevice camera;
        private CameraManager cmanager;

        public CameraNew(final Context context) {
            this.cmanager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        }

        @Override
        public CameraSupport open(final int cameraId) {
            try {
                String[] cameraIds = cmanager.getCameraIdList();
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA);
                }
                cmanager.openCamera(cameraIds[cameraId], new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        CameraNew.this.camera = camera;
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        CameraNew.this.camera = camera;
                        // TODO handle
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {
                        CameraNew.this.camera = camera;
                        // TODO handle
                    }
                }, null);
            } catch (Exception e) {
                // TODO handle
            }
            return this;
        }

        @Override
        public int getOrientation(final int cameraId) {
            try {
                String[] cameraIds = cmanager.getCameraIdList();
                CameraCharacteristics characteristics = cmanager.getCameraCharacteristics(cameraIds[cameraId]);
                return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            } catch (CameraAccessException e) {
                // TODO handle
                return 0;
            }
        }

    }

    /* Prepare video recorder */
    private void prepareRecorder() {
        recorder.setPreviewDisplay(holder.getSurface());
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    /**
     * Start/Stop Record video
     *
     * @param v record button
     */
    public void recordVideo(View v) {
        if (!recorderReady) {
            Toast.makeText(this, "Unable to capture video.", Toast.LENGTH_SHORT).show();
        } else {
            if (recording) {
                recorder.stop();
                recording = false;
                Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show();
                // Let's initRecorder so we can record again
                initRecorder();
                prepareRecorder();
            } else {
                recording = true;
                recorder.start();
            }
            iconToogle.setBackgroundResource(recording ? R.drawable.ic_stop : R.drawable.ic_record);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (recorderReady)
            prepareRecorder();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        finish();
    }

    /**
     * Expand setting view when click setting button
     *
     * @param view setting button
     */
    public void toggleExpand(View view) {
        Transition transition = new Slide(Gravity.TOP);
        transition.setDuration(300);
        transition.addTarget(R.id.control_area);
        TransitionManager.beginDelayedTransition(root, transition);
        settingArea.setVisibility(settingArea.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    /* ------- Video Recording part  End -------- */


    /* ------- Bluetooth Communication part start -------- */

    /* Need Fine Location permission from android M or above. */
    public void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(DEVTAG, "Require location permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION);
        } else {
            Log.e(DEVTAG, "check bluetooth 2");
            checkBluetooth();
        }
    }

    /* Check if bluetooth enabled. if not, request to turn on bluetooth */
    public void checkBluetooth() {
        manager.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (manager.mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
            Log.e(DEVTAG, "device doesn't support bluetooth");
//            finish();
        } else if (!manager.mBluetoothAdapter.isEnabled()) {
            //Turn on bluetooth
            AddLog("Bluetooth disabled. please turn on bluetooth");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLUETOOTH);
        } else {
            AddLog("bluetooth ready, init scanner");
            Log.e(DEVTAG, "bluetooth ready, init scanner");
            bluetoothReady = true;
            manager.initScanner();
        }
    }

    /**
     * If bluetooth device is ready, start scan device and auto connect.
     *
     * @param View connect button.
     */
    public void Connect(View View) {
        if (bluetoothReady) {
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                bluetoothReady = false;
                AddLog("Bluetooth disabled. please turn on");
            }
            AddLog("Start Scan Device...");
            manager.StartScanning();
        } else {
            Toast.makeText(this, "Can't use bluetooth. check bluetooth enabled, or device support bleutooth.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * disconnect sensor
     *
     * @param view disconnect button
     */
    public void Disconnect(View view) {
        if (!bluetoothReady)
            AddLog("Nothing to disconnect.");
        else
            manager.disconnect();
    }

    /**
     * Read data from sensor
     *
     * @param view read button
     */
    public void Read(View view) {
        if (serviceList.size() == 0) {
            AddLog("No service available");
            return;
        }
        if (bluetoothReady)
            manager.readData(selectedcharacteristic);
    }

    /**
     * Write data to sensor
     *
     * @param view write button
     */
    public void Write(View view) {
        createConfig();
        if (characteristicList.size() == 0) {
            AddLog("No characteristics exist.");
            return;
        }
        if (bluetoothReady)
            manager.writeData(selectedcharacteristic);
    }

    /**
     * Create config file with user setting.
     *
     * @return string config
     */
    public String createConfig() {
        String config = (filterAccell.isChecked() ? "AA" : "55") + " "
                + (filterGyro.isChecked() ? "AA" : "55") + " "
                + (filterCompass.isChecked() ? "AA" : "55") + " "
                + (filterTemperature.isChecked() ? "AA" : "55") + " "
                + Integer.toString(Integer.parseInt(edtHz.getText().toString()), 16) + " "
                + Integer.toString(Integer.parseInt(edtTimes.getText().toString()), 16);
        AddLog("Write : " + config);
        return config;
    }

    /**
     * Show log to display and save to log file if enabled file save.
     *
     * @param msg log data
     */
    public static void AddLog(String msg) {
        if (logList.size() > 50)
            logList.remove(0);
        logList.add(new LogItem(msg));
        LogAdapter.notifyDataSetChanged();
        LogView.scrollToPosition(logList.size() - 1);
        if (enablesavefile.isChecked())
            Util.printLog(msg);
    }

    /* ------- Bluetooth Communication part End -------- */


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                initRecorder();
                prepareRecorder();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                //User denied Permission.
            }
        }
        if (requestCode == LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetooth();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH) {
            if (resultCode != Activity.RESULT_OK) {
                checkBluetooth();
            } else {
                AddLog("Bluetooth Enabled. start scan device");
                bluetoothReady = true;
                manager.initScanner();
            }
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (view.getId()) {
            case R.id.serviceList:
                Log.e(DEVTAG, "Service spinner selected:" + position + ", UUID:" + serviceList.get(position));
                manager.getCharacteristic(position);
                selectedcharacteristic = 0;
                break;
            case R.id.characteristicList:
                Log.e(DEVTAG, "Characteristics spinner selected:" + position + ", UUID:" + characteristicList.get(position));
                selectedcharacteristic = position;
                selectedUuid = UUID.fromString(characteristicList.get(position));
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
    }


    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        AddLog("Bluetooth Turned Off");
                        bluetoothReady = false;
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        AddLog("Bluetooth turning off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        AddLog("Bluetooth Turned On");
                        bluetoothReady = true;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        AddLog("Bluetooth turning on...");
                        break;
                }
            }
        }
    };
}