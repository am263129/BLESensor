package com.example.bluetooth.seed;


import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Half;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Util {

    public static boolean cameraMode = false;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final String TAG = "Util";
    /** Create a file Uri for saving an image or video */
    public static String getOutputMediaFileUri(int type){
        return getOutputMediaFile(type);
    }

    /** Create a File for saving an image or video */
    public static String getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "BLESensor");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("BLESensor", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        String path;
        if (type == MEDIA_TYPE_IMAGE){
            path = mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg";
//            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
//                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            path = mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4";
//            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
//                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        Log.e("FASA","Return file:"+path);

        return path;
    }

    public static void  printLog(String text)
    {
        File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"log.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, e.toString());
            Log.e(TAG, e.toString());
        }
    }

    public static void exportData(float[] accel, float[] gyro, float[] compass){
        boolean isnew = false;
        File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"sensor_data.csv");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
                isnew = true;
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            //write header when new file created.
            if(isnew){
                buf.append("time,gFx,gFy,gFz,wx,wy,wz,Bx,By,Bz");
                buf.newLine();
            }
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE dd-MMM-yyyy hh-mm-ss a,");
            buf.append(simpleDateFormat.format(calendar.getTime()));
            for(int i =0; i<3; i++){
                buf.append(String.valueOf(accel[i]));
                buf.append(",");
            }
            for(int i =0; i<3; i++){
                buf.append(String.valueOf(gyro[i]));
                buf.append(",");
            }
            for(int i =0; i<3; i++){
                buf.append(String.valueOf(compass[i]));
                buf.append(",");
            }
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
    }

//    public static String toHexString(byte[] value){
//        String result = "";
//
//        return result;
//    }

    public static float[] Half(byte[] data, int offset){
        float[] result = new float[3];

        for(int i = 0, j=offset; i< 3; i++, j+=2){
            result[i] = getHalf(new byte[]{data[j],data[j+1]});
        }
        return result;
    }
    public static float getHalf(byte[] buffer){
        int sign = (byte) ((buffer[0] >> 7) & 1);
        int storedExponent = (int)buffer[0]<<1>>2;
        int implicit = (storedExponent == 0)?0:1;
        float significand = (float)(buffer[0]<<6 & 0xCC0 << 4 | buffer[1]) / 1024f;
        float result =  (float)Math.pow(-1,(sign)) *(float)Math.pow(2,(storedExponent - 15)) * (float)(implicit + (significand/1024));
        Log.e("Half data: ",result +"");
        return result;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
