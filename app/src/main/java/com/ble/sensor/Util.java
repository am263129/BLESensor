package com.ble.sensor;

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
import java.util.Date;

public class Util {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final String TAG = "Util";
    /** Create a file Uri for saving an image or video */
    public static String getOutputMediaFileUri(int type){
        return getOutputMediaFile(type).getPath();
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(int type){
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
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
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

    public static void exportData(byte[] data){
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
            for(byte item: data){
                buf.append(String.format("0x%20x",item));
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

    public static String toHexString(byte[] value){
        String result = "";

        return result;
    }

    private static Half[] getHalf(byte[] bytes, int offset)
    {
        Half[] triple = new Half[3];

//        triple[0] = Half.toHalf(bytes, offset);
//        y = Half.ToHalf(bytes, offset + 2);
//        z = Half.ToHalf(bytes, offset + 4);

        return triple;
    }
}
