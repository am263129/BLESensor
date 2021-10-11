package com.example.bluetooth.seed;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
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
    public static final int DATA_TYPE_CSV = 3;
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
        } else if(type == DATA_TYPE_CSV){
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + "/DATA_"+ timeStamp +".csv";
        }
        else {
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
                Log.d(TAG,"Create file :"+logFile.createNewFile());
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

    public static String exportData(String csvPath,String time,float[] accel, float[] gyro, float[] compass){
        boolean isnew = false;
        File logFile = new File(csvPath);
        if (!logFile.exists())
        {
            try
            {
                Log.d(TAG,"Create file :"+logFile.createNewFile());
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
//            buf.append(simpleDateFormat.format(calendar.getTime()));
            buf.append(time);
            buf.append(",");
            for(int i =0; i<3; i++){
                buf.append(zeroPrint(accel[i]));
                buf.append(",");
            }
            for(int i =0; i<3; i++){
                buf.append(zeroPrint(gyro[i]));
                buf.append(",");
            }
            for(int i =0; i<3; i++){
                buf.append(zeroPrint(compass[i]));
                buf.append(i<2?",":"");
            }
            buf.newLine();
            buf.close();
            return csvPath;
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return "Failed";
        }
    }

    public static String zeroPrint(float value){
        if(value == 0.0f){
            return "0";
        }
        else{
            return String.valueOf(value);
        }
    }

    public static void exportMp4ToGallery(Context context, String filePath) {
        final ContentValues values = new ContentValues(2);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATA, filePath);
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                values);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + filePath)));
    }

    public static float[] Half(byte[] data, int offset){
        float[] result = new float[3];
        for(int i = 0, j=offset; i< 3; i++, j+=2){
            result[i] = getHalf(new byte[]{data[j],data[j+1]});
        }
        return result;
    }
    public static float getHalf(byte[] buffer){
        byte first = buffer[1];
        byte second = buffer[0];
        byte sign = (byte)(first>>7);
        byte stored = (byte)((first & 0x7C) >>2);
        byte implicit = (byte)((stored==0)?0:1);
        short significand = (short)(((short)(first & (short)0x0003)<<8) | (short)second);
        float result = (float)Math.pow(-1,sign) * (float)Math.pow(2,(stored - 15)) * (implicit + (float)(significand/1024f));
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
