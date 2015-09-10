package com.eneaceolini.audio;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import com.eneaceolini.exapp.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.Vector;

/**
 * Created by Enea on 04/06/15.
 */
public class IAudioReceiver {

    private final String TAG = "IAudioReceiver";
    private final int UPDATE_KBYTES_COUNT = 1;

    private MainActivity activity;
    private String fileName;
    private FileOutputStream os;
    short[] signalA = new short[1024];
    short[] signalB = new short[1024];
    float[] signalAD = new float[512];
    float[] signalBD = new float[512];
    byte[] bDataA = new byte[2048];
    byte[] bDataB = new byte[2048];




    public IAudioReceiver(MainActivity activity, String fileName){
        this.activity = activity;
        this.fileName = fileName;

        try {
            os = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }



    private byte[] short2byte(short[] sData) {

        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }
        return bytes;

    }


    public void capturedAudioReceived(short[] buf,int n, boolean a){


        if(n>0) {



            //System.arraycopy(sac,0,signalA,0,n);
            int k = 0;

            for (int i = 0; i < n - 1; i += 2) {
                signalA[k] = buf[i];
                signalB[k] = buf[i + 1];
                signalAD[k] = (float)buf[i];
                signalBD[k] = (float)buf[i + 1];
                k++;
            }

            bDataA = short2byte(signalA);
            bDataB = short2byte(signalB);
            /*
            try {
                os.write(bDataA, 0, bDataA.length);
            }catch(Exception e){
                Log.e(TAG,e.toString());
            }
            */

            //audioAnalyzer = new AudioAnalyzer(MakeACopy.makeACopy(bData),MakeACopy.makeACopy(bData));
            //audioAnalyzer.start();
/*
            Message msgObj = mHandler.obtainMessage(UPDATE_KBYTES_COUNT);
            Bundle b = new Bundle();
            b.putDoubleArray("signalA", signalAD);
            b.putDoubleArray("signalB", signalBD);
            msgObj.setData(b);
            mHandler.sendMessage(msgObj);
*/

            //activity.updateGraphs(bDataA, bDataB, signalA, signalB, signalAD, signalBD);
        }

    }
}
