package com.eneaceolini.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import com.eneaceolini.exapp.MainActivity;
import com.eneaceolini.exapp.SelfLocalization;
import com.eneaceolini.utility.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class LocalizeOwnSpk implements Runnable {

    private final String TAG = "AudioCapturer";
    private AudioRecord audioRecorder = null;
    private final int samplePerSec ;
    private Thread thread = null;
    private final int AUDIO_SOURCE;
    MainActivity.ReadTh myReadTh;
    SelfLocalization activity;
    String fileName;
    private final int JAMLENGTH = 1000;
    private final int GENSIGLENGTH = 4410 + JAMLENGTH;
    private byte[] generatedPCM;
    private double[] generatedSignal = new double[GENSIGLENGTH];
    //cause I wanted it to be 100ms but i need to add the JAM of about at least 50 ms
    private short[] record = new short[0];
    float lenChirp = 0.1f; // in sec

    boolean first = true;
    private boolean isRecording;
    private static LocalizeOwnSpk audioCapturer;
    short[] tempBuf ;
    short[] tempBuf2 = new short[Constants.FRAME_SIZE / 2];
    boolean change = false;



    private LocalizeOwnSpk( int audioSource, int sampleRate,SelfLocalization activity) {
        this.AUDIO_SOURCE = audioSource;
        this.samplePerSec = sampleRate;
        this.activity = activity;
        fileName = Environment.getExternalStorageDirectory().getPath()+"/mychirp.pcm";
        try {
            os = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        generatedPCM = genSweep(generatedSignal,GENSIGLENGTH,samplePerSec,5000,10000);
    }

    public static LocalizeOwnSpk getInstance(int audioSource, int sampleRate,SelfLocalization activity) {
        if (audioCapturer == null) {
            audioCapturer = new LocalizeOwnSpk(audioSource,sampleRate,activity);
        }
        return audioCapturer;
    }

    public void copyGeneratedSignal(double[] buffer){
        System.arraycopy(generatedSignal,0,buffer,0,generatedSignal.length);
    }

    public void destroy(){
        audioCapturer = null;
    }

    public byte[] getGeneratedPCM(){
        return generatedPCM;
    }

    public void start() {



        int bufferSize = AudioRecord.getMinBufferSize(this.samplePerSec, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        tempBuf = new short[Constants.FRAME_SIZE/2];
        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {

            audioRecorder = new AudioRecord(AUDIO_SOURCE, this.samplePerSec, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferSize * 20); // bufferSize
            // 10x

            if (audioRecorder != null && audioRecorder.getState() == AudioRecord.STATE_INITIALIZED) {



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
        //playSnd(generatedPCM);
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            while (isRecording && audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                //short[] tempBuf = new short[Constants.FRAME_SIZE / 2];
                int n = audioRecorder.read(tempBuf, 0, tempBuf.length);
                //Log.d(TAG,"First sample at : " +System.currentTimeMillis());
                    //iAudioReceiver.capturedAudioReceived(tempBuf, n, false);

                    if (n > 0) {
                            System.arraycopy(tempBuf,0,activity.fromMic,0,n);
                            activity.addSamples(n);
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
        thread = null;
    }




    public byte[] genSweep(double[] buffer, int totalNumSamples, int sampleRate, float minFreq, float maxFreq)
    {
        int biasInSamples = JAMLENGTH;
        int numSamples = totalNumSamples - biasInSamples;
        int biasInBytes = biasInSamples*8;

        byte[] pcmSignal = new byte[totalNumSamples*8];


        double start = 2.0 * Math.PI * minFreq;
        double stop = 2.0 * Math.PI * maxFreq;
        //double stop = 2.0 * Math.PI * minFreq;
        double tmp1 = Math.log(stop / start);

        int s;
        for (s=0 ; s<numSamples ; s++) {
            double t = (double)s / numSamples;
            double tmp2 = Math.exp(t * tmp1) - 1.0;
            buffer[s + biasInSamples] = Math.sin((start * numSamples * tmp2) / (sampleRate * tmp1));
            //buffer[s + biasInSamples] = Math.sin(start*t);
        }

        //buffer = createSin(numSamples);


        //adding JAM
        //for(int i = 0;i<biasInSamples;i++)
        //    buffer[i] = 0;

        int idx = 0;
        for (final double dVal : buffer) {
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            pcmSignal[idx++] = (byte) (val & 0x00ff);
            pcmSignal[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
        //activity.plot(buffer);
/*
        try {
        File sdCardDir = Environment.getExternalStorageDirectory();
        File targetFile;
        targetFile = new File(sdCardDir.getCanonicalPath());
        File file=new File(targetFile + "/"+"chirp"+".txt");

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        //raf.seek(file.length());
        String out = "";
        for(int i = 0;i<buffer.length;i++)
            out+= buffer[i]+"\n";
        raf.write(out.getBytes());
        raf.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
*/
        record(pcmSignal);
        return pcmSignal;

    }

    public double[] createSin(int n)
    {
        double MAX = 5000;

        double[] sin = new double[n];

        for (int i = 0; i < n; i++)
        {
            sin[i] = Math.sin(MAX * Math.PI * (double) i / n);

        }

        return sin;
    }

    private FileOutputStream os;

    public void record(byte[] pcm){

        try {
            os.write(pcm, 0, pcm.length);
        }catch(Exception e){
            Log.e(TAG,e.toString());
        }

    }




    public void playSnd(byte[] pcmSignal){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                samplePerSec, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, pcmSignal.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(pcmSignal, 0, pcmSignal.length);
        audioTrack.play();
    }

}
