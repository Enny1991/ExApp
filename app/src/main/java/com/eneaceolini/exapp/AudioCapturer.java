package com.eneaceolini.exapp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Created by Enea on 04/06/15.
 */
public class AudioCapturer implements Runnable {

    private final String TAG = "AudioCapturer";
    private AudioRecord audioRecorder = null;
    private int bufferSize;
    private int samplePerSec ;
    private Thread thread = null;
    private int AUDIO_SOURCE;

    private boolean isRecording;
    private static AudioCapturer audioCapturer;

    private IAudioReceiver iAudioReceiver;

    private AudioCapturer(IAudioReceiver audioReceiver, int audioSource, int sampleRate) {
        Log.d(TAG,"New Instance of recorder");
        this.iAudioReceiver = audioReceiver;
        this.AUDIO_SOURCE = audioSource;
        this.samplePerSec = sampleRate;
    }

    public static AudioCapturer getInstance(IAudioReceiver audioReceiver,int audioSource, int sampleRate) {
        if (audioCapturer == null) {
            audioCapturer = new AudioCapturer(audioReceiver,audioSource,sampleRate);
        }
        return audioCapturer;
    }

    public void destroy(){
        this.audioCapturer = null;
    }

    public void start() {

        bufferSize = AudioRecord.getMinBufferSize(samplePerSec, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {

            audioRecorder = new AudioRecord(AUDIO_SOURCE, this.samplePerSec, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, this.bufferSize * 20); // bufferSize
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
        return (audioRecorder != null) ? (audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) : false;
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        while (isRecording && audioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            short[] tempBuf = new short[Constants.FRAME_SIZE / 2];
            int n = audioRecorder.read(tempBuf, 0, tempBuf.length);
            iAudioReceiver.capturedAudioReceived(tempBuf,n, false);
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

}
