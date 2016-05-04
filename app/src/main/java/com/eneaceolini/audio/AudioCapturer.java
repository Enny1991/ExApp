package com.eneaceolini.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;
import android.util.Log;

import com.eneaceolini.exapp.MainActivity;
import com.eneaceolini.netcon.GlobalNotifier;
import com.eneaceolini.utility.Constants;

import java.io.FileOutputStream;

/**
 * Created by Enea on 04/06/15.
 */
public class AudioCapturer implements Runnable {

    private static final String TAG = "AudioCapturer";
    private AudioRecord audioRecorder = null;
    private final int samplePerSec ;
    private Thread thread = null;
    private final int AUDIO_SOURCE;
    MainActivity.ReadTh myReadTh;
    String fileName;
    FileOutputStream os;

boolean first = true;
    private boolean isRecording;
    private static AudioCapturer audioCapturer;
    short[] tempBuf ;
    short[] tempBuf2 = new short[Constants.FRAME_SIZE / 2];
    boolean change = false;


    private IAudioReceiver iAudioReceiver;
    GlobalNotifier monitor,backFire;


    private AudioCapturer(IAudioReceiver audioReceiver, int audioSource, int sampleRate,GlobalNotifier monitor,MainActivity.ReadTh rth,GlobalNotifier bb) {
        Log.d(TAG,"New Instance of recorder");
        this.monitor = monitor;
        this.iAudioReceiver = audioReceiver;
        this.AUDIO_SOURCE = audioSource;
        this.samplePerSec = sampleRate;
        backFire = bb;
        myReadTh = rth;
    }

    public static AudioCapturer getInstance(IAudioReceiver audioReceiver,int audioSource, int sampleRate, GlobalNotifier monitor,MainActivity.ReadTh rth,GlobalNotifier bb) {
        if (audioCapturer == null) {
            audioCapturer = new AudioCapturer(audioReceiver,audioSource,sampleRate,monitor,rth,bb);
            Log.d(TAG,"Got the instance");
        }
        return audioCapturer;
    }

    public void destroy(){
        audioCapturer = null;
    }

    public void start() {
        fileName = Environment.getExternalStorageDirectory().getPath()+"/myrecord.pcm";
        try {
            os = new FileOutputStream(fileName);
        }catch(Exception e){
            Log.w("os","definition");
        }


        int bufferSize = AudioRecord.getMinBufferSize(this.samplePerSec, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        tempBuf = new short[Constants.FRAME_SIZE/2];
        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {

            audioRecorder = new AudioRecord(AUDIO_SOURCE, this.samplePerSec, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize * 20); // bufferSize
            // 10x

            if (audioRecorder != null && audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                Log.i(TAG, "Audio Recorder created");

                audioRecorder.startRecording();
                isRecording = true;
                thread = new Thread(this);
                thread.start();

            } else {
                Log.e(TAG, "Unable to create AudioRecord instance");
            }

        } else {
            Log.e(TAG, "Unable to get minimum buffer size");
        }
    }

    // ciao
    public void stop() {
        isRecording = false;
        if (audioRecorder != null) {
            if (audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                Log.d(TAG,"Stopping Recorder");
                // System.out
                // .println("Stopping the recorder inside AudioRecorder");
                audioRecorder.stop();
            }
            if (audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecorder.release();
                Log.d(TAG, "Releasing Recorder");
            }
        }
    }

    public boolean isRecording() {
        return (audioRecorder != null) && (audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING);
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            while (isRecording && audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {

                //short[] tempBuf = new short[Constants.FRAME_SIZE / 2];
                int n = audioRecorder.read(tempBuf, 0, tempBuf.length);

                    //iAudioReceiver.capturedAudioReceived(tempBuf, n, false);
                    if (n > 0) {
                        if(!first) {
                            backFire.doWait();
                            System.arraycopy(tempBuf,0,myReadTh.globalSignal,0,n);
                            //myReadTh.globalSignal = tempBuf;
                            monitor.length = n;
                            monitor.doNotify();
                        }
                        else
                        {
                            System.arraycopy(tempBuf,0,myReadTh.globalSignal,0,n);
                            //myReadTh.globalSignal = tempBuf;
                            monitor.length = n;
                            monitor.doNotify();
                            first =false;
                        }
                    }

            }


    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("AudioCapturer finalizer");
        if (audioRecorder != null && audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecorder.stop();
            audioRecorder.release();
        }
        audioRecorder = null;
        iAudioReceiver = null;
        thread = null;
    }

    private static byte[] short2byte(short[] sData) {

        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }
        return bytes;

    }

}
