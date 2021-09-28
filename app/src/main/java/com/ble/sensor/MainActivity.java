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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.transition.Fade;
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
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, AdapterView.OnItemSelectedListener {
    MediaRecorder recorder;
    SurfaceHolder holder;
    private int RECORD_AUDIO = 9001;
    private final int LOCATION = 9002;
    private final int BLUETOOTH = 9003;

    boolean recording = false;

    private static DeviceAdapter LogAdapter;
    private static ArrayList<LogItem> logList = new ArrayList<>();
    protected LinearLayoutManager mLayoutManager;

    private boolean bluetoothReady = false, recorderReady = false;
    private BLEManager manager;

    private ConstraintLayout root, settingArea, btnSetting;
    private CheckBox filterAccell, filterGyro, filterCompass, filterTemperature;
    private boolean enableAccell = false, enableGyro = false, enableCompass = false, enableTemperature = false;
    private EditText edtHz, edtTimes;
    private Button btnConnect;
    private static RecyclerView LogView;
    private static CheckBox enablesavefile;
    private ImageView iconToogle;
    private String logHistory = "";
    private Spinner services, characteristics;
    public List<String> serviceList = new ArrayList<>();
    public List<String> characteristicList = new ArrayList<>();

    public UUID selectedUuid;
    public int selectedcharacteristic = 0;
    private Handler delayHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            manager.StopScanning();
            btnConnect.setEnabled(true);
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        recorder = new MediaRecorder();
        manager = new BLEManager(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, RECORD_AUDIO);
        } else {
            initRecorder();
        }
        initUi();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            checkPermission();
        else {
            checkBluetooth();
        }
    }

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
        LogAdapter = new DeviceAdapter(logList);
        LogView.setAdapter(LogAdapter);
        mLayoutManager = new LinearLayoutManager(this);
        LogView.setLayoutManager(mLayoutManager);
        refresh(true);//refresh both spinner
    }

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

    /**
     * Video Recording part
     * ------- Start --------
     */

    private void initRecorder() {
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);
        File videofile = new File(Util.getOutputMediaFileUri(Util.MEDIA_TYPE_VIDEO));
        recorder.setOutputFile(videofile.getPath());
        recorder.setMaxDuration(50000); // 50 seconds
        recorder.setMaxFileSize(5000000); // Approximately 5 megabytes
        recorderReady = true;
    }

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

    public void recordVideo(View v) {
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

    public void toggleExpand(View view) {
        Transition transition = new Slide(Gravity.TOP);
        transition.setDuration(300);
        transition.addTarget(R.id.control_area);
        TransitionManager.beginDelayedTransition(root, transition);
        settingArea.setVisibility(settingArea.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    /**
     * Video Recording part
     * ------- End --------
     */


    /**
     * Bluetooth Communication part
     * ------- start --------
     */

    /* Need Fine Location permission from android M or above. */
    public void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION);
        } else {
            checkBluetooth();
        }
    }

    /* Check if bluetooth enabled. if not, request to turn on bluetooth */
    public void checkBluetooth() {
        manager.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (manager.mBluetoothAdapter == null || !manager.mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, (manager.mBluetoothAdapter != null) ? "Multiple advertisement not supported" : "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
//            finish();
        } else if (!manager.mBluetoothAdapter.isEnabled()) {
            //Turn on bluetooth
            AddLog("Bluetooth disabled. please turn on bluetooth");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLUETOOTH);
        } else {
            bluetoothReady = true;
            manager.initScanner();
        }
    }


    public void ConnectBLE(BluetoothDevice device) {

    }


    /**
     * If bluetooth device is ready, start scan device and auto connect.
     *
     * @param View connect button.
     */
    public void Connect(View View) {
        if (bluetoothReady) {
            AddLog("Start Scan Device...");
            manager.StartScanning();
            btnConnect.setEnabled(false);
            delayHandler.sendEmptyMessageDelayed(0, 60000);
        }
        else{
            Toast.makeText(this,"Can't use bluetooth. check bluetooth enabled, or device support bleutooth.",Toast.LENGTH_LONG).show();
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
        if(serviceList.size() == 0)
        {
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
        if(characteristicList.size() == 0)
        {
            AddLog("No characteristics exist.");
            return;
        }
        if (bluetoothReady)
            manager.writeData(selectedcharacteristic);
    }

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

    public static void AddLog(String msg) {
        if (logList.size() > 50)
            logList.remove(0);
        logList.add(new LogItem(msg));
        LogAdapter.notifyDataSetChanged();
        LogView.scrollToPosition(logList.size() - 1);
        if(enablesavefile.isChecked())
            Util.printLog(msg);
    }

    /**
     * Bluetooth Communication part
     * ------- End --------
     */


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
                Toast.makeText(this, "Permission dinied", Toast.LENGTH_LONG).show();
                //User denied Permission.
            }
        }
        if (requestCode == LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetooth();
            } else {
                Toast.makeText(this, "Permission dinied", Toast.LENGTH_LONG).show();
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
                manager.getCharacteristic(position);
                selectedcharacteristic = 0;
                break;
            case R.id.characteristicList:
                selectedcharacteristic = position;
                selectedUuid = UUID.fromString(characteristicList.get(position));
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}