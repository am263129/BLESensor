package com.example.bluetooth.seed;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecorderManager implements SurfaceHolder.Callback, Handler.Callback {
    public DeviceControlActivity context;
    public MediaRecorder recorder;
    public SurfaceHolder mSurfaceHolder;
    public boolean recorderReady = false;
    public boolean recording = false;

    static final String TAG = "CamTest";
    static final int MY_PERMISSIONS_REQUEST_CAMERA = 1242;
    private static final int MSG_CAMERA_OPENED = 1;
    private static final int MSG_SURFACE_READY = 2;
    private final Handler mHandler = new Handler(this);
    SurfaceView mSurfaceView;
    CameraManager mCameraManager;
    String[] mCameraIDsList;
    CameraDevice.StateCallback mCameraStateCB;
    CameraDevice mCameraDevice;
    CameraCaptureSession mCaptureSession;
    GLSurfaceView sampleGLView;
    boolean mSurfaceCreated = true;
    boolean mIsCameraConfigured = false;
    private Surface mCameraSurface = null;

    public RecorderManager(DeviceControlActivity context) {
        this.context = context;
        initCamera();
        initRecorder();
        prepareRecorder();
    }

    private void initCamera() {
        mSurfaceView = (SurfaceView) context.findViewById(R.id.cameraView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            mCameraIDsList = this.mCameraManager.getCameraIdList();
            for (String id : mCameraIDsList) {
                Log.v(TAG, "CameraID: " + id);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mCameraStateCB = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Toast.makeText(context, "onOpened", Toast.LENGTH_SHORT).show();

                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Toast.makeText(context, "onDisconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Toast.makeText(context, "onError", Toast.LENGTH_SHORT).show();
            }
        };

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Please check camera permission.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mCameraManager.openCamera(mCameraIDsList[1], mCameraStateCB, new Handler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /* init video recorder */
    private void initRecorder() {

        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        CamcorderProfile cpHigh = CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH);
        recorder.setProfile(cpHigh);
        recorder.setOutputFile(Util.getOutputMediaFileUri(Util.MEDIA_TYPE_VIDEO));
        recorder.setMaxDuration(1800000); // 30 mins
        recorder.setMaxFileSize(500000000); // Approximately 500 megabytes
        recorderReady = true;
    }

    private void configureCamera() {
        // prepare list of surfaces to be used in capture requests
        if(mCameraSurface!=null) {
            List<Surface> sfl = new ArrayList<Surface>();
            sfl.add(mCameraSurface); // surface for viewfinder preview

            // configure camera with all the surfaces to be ever used

            try {
                if (mCameraDevice != null) mCameraDevice.createCaptureSession(sfl,
                        new CaptureSessionListener(), null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mIsCameraConfigured = true;
        }
    }

    /* Prepare video recorder */
    private void prepareRecorder() {
        recorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start/Stop Record video
     */
    public void recordVideo() {
        if (!recorderReady) {
            Toast.makeText(context, "Unable to capture video.", Toast.LENGTH_SHORT).show();
        } else {
            if (recording) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recorder.stop();
                    }
                });
                recording = false;
                Toast.makeText(context, "Saving...", Toast.LENGTH_SHORT).show();
                // Let's initRecorder so we can record again
                initRecorder();
                prepareRecorder();
            } else {
                recording = true;
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recorder.start();
                    }
                });
            }
            context.iconRecord.setBackgroundResource(recording ? R.drawable.ic_stop : R.drawable.ic_record);
        }
    }
    public void resume(){

    }

    public void Stop(){
        try {
            if (mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
                mCaptureSession = null;
            }
            mIsCameraConfigured = false;
        } catch (final CameraAccessException e) {
            // Doesn't matter, cloising device anyway
            e.printStackTrace();
        } catch (final IllegalStateException e2) {
            // Doesn't matter, cloising device anyway
            e2.printStackTrace();
        } finally {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
                mCaptureSession = null;
            }
        }
    }

    private class CaptureSessionListener extends
            CameraCaptureSession.StateCallback {
        @Override
        public void onConfigureFailed(final CameraCaptureSession session) {
            Log.d(TAG, "CaptureSessionConfigure failed");
        }

        @Override
        public void onConfigured(final CameraCaptureSession session) {
            Log.d(TAG, "CaptureSessionConfigure onConfigured");
            mCaptureSession = session;

            try {
                CaptureRequest.Builder previewRequestBuilder = mCameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(mCameraSurface);
                mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                        null, null);
            } catch (CameraAccessException e) {
                Log.d(TAG, "setting up preview failed");
                e.printStackTrace();
            }
        }
    }


    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        mCameraSurface = holder.getSurface();
        if (recorderReady)
            prepareRecorder();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        mCameraSurface = holder.getSurface();
        mSurfaceCreated = true;
        mHandler.sendEmptyMessage(MSG_SURFACE_READY);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mSurfaceCreated = false;
        if (recording) {
            recorder.stop();
            recording = false;
        }
        if(recorder!=null)
            recorder.release();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_CAMERA_OPENED:
            case MSG_SURFACE_READY:
                // if both surface is created and camera device is opened
                // - ready to set up preview and other things
                if (mSurfaceCreated && (mCameraDevice != null)
                        && !mIsCameraConfigured) {
                    configureCamera();
                }
                break;
        }
        return true;
    }
}
