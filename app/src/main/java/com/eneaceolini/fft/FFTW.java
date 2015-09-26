package com.eneaceolini.fft;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class FFTW {

    static {
        System.loadLibrary("fftw_jni");
    }

    @SuppressWarnings("JniMissingFunction")
    private static native double[] executeJNI(double in[]);
    @SuppressWarnings("JniMissingFunction")
    private static native void initThreadsJNI(int num_of_threads);
    @SuppressWarnings("JniMissingFunction")
    private static native boolean areThreadsEnabled();
    @SuppressWarnings("JniMissingFunction")
    private static native void removeThreadsJNI();

    public static void setMultithread(int num_of_threads) {
        if (!FFTW.areThreadsEnabled()) {
            FFTW.initThreadsJNI(num_of_threads);
        }
    }

    public static void setMonothread() {
        if (FFTW.areThreadsEnabled()) {
            FFTW.removeThreadsJNI();
        }
    }


    public static double[] execute(double in[]) {
        return FFTW.executeJNI(in);
    }
}
