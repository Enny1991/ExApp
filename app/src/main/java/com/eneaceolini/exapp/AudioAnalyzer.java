package com.eneaceolini.exapp;

/**
 * Created by Enea on 05/06/15.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Enea on 04/06/15.
 */
public class AudioAnalyzer extends Thread {

    private final String TAG = "AudioCAnalyzer";
    private AudioRecord audioRecorder = null;
    private int bufferSize;
    private int samplePerSec = 16000;
    private Thread thread = null;
    private int AUDIO_SOURCE;
    private String STATIC_IP = "172.19.12.186";
    private int PORT = 6880;
    private DatagramSocket mSocket;

    private boolean isRecording;
    private static AudioAnalyzer audioAnalyzer;
    private byte[] sA=null,sB=null;

    private IAudioReceiver iAudioReceiver;



    public AudioAnalyzer(byte[] sA, byte[] sB){
        this.sA = sA;
        this.sB = sB;
    }






    @Override
    public void run() {
       // android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {

            InetAddress IPAddress = InetAddress.getByName(STATIC_IP);
            if (mSocket == null) {
                mSocket = new DatagramSocket(null);
                mSocket.setReuseAddress(true);
                mSocket = new DatagramSocket();
                mSocket.connect(IPAddress, PORT);
                mSocket.setBroadcast(true);
            }

            //byte[] compressed = Compressor.compress(sA);

            DatagramPacket sendPacket = new DatagramPacket(sA, sA.length, IPAddress, PORT);
            short d = toShort(new byte[]{sA[0],sA[1]});
            Log.d(TAG,"first sent: "+d);
            mSocket.send(sendPacket);



        }catch(Exception e) {
            Log.e("UDP", e.toString());
        }
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("AudioAnalyzer finalizer");
        iAudioReceiver = null;
        thread = null;
    }

    public static short toShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

}

