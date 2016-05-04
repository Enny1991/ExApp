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
    private static native double[] corrJNI(double in1[],double in2[]);
    @SuppressWarnings("JniMissingFunction")
    private static native void initThreadsJNI(int num_of_threads);
    @SuppressWarnings("JniMissingFunction")
    private static native boolean areThreadsEnabled();
    @SuppressWarnings("JniMissingFunction")
    private static native void removeThreadsJNI();
    @SuppressWarnings("JniMissingFunction")
    private static native double[] DelayAndSum(double in1[], double in2[], double theta);
    @SuppressWarnings("JniMissingFunction")
    private static native double[] TEST(double in1[]);

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


    public static double[] test(double in[]) {
        return FFTW.TEST(in);
    }
    public static double[] execute(double in[]) {
        return FFTW.executeJNI(in);
    }
    public static double[] execute_corr(double in1[],double in2[]) {
        return FFTW.corrJNI(in1,in2);
    }
    public static double[] execute_beam(double in1[],double in2[],double theta) {
        return FFTW.DelayAndSum(in1,in2,theta);
    }
}
