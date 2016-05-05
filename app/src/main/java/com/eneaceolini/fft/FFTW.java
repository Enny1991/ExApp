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
    private static native void corrJNI(double in1[],double in2[],double[] buffer);
    @SuppressWarnings("JniMissingFunction")
    private static native void initThreadsJNI(int num_of_threads);
    @SuppressWarnings("JniMissingFunction")
    private static native boolean areThreadsEnabled();
    @SuppressWarnings("JniMissingFunction")
    private static native void removeThreadsJNI();
    @SuppressWarnings("JniMissingFunction")
    private static native void DelayAndSum(double in1[], double in2[],double buffer[], double theta);
    @SuppressWarnings("JniMissingFunction")
    private static native double[] TEST(double in1[]);
    @SuppressWarnings("JniMissingFunction")
    private static native void setup(int len);

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
    public static void setup_FFTW(int len) {
        FFTW.setup(len);
    }
    public static void execute_corr(double in1[],double in2[],double[] buffer) {
        FFTW.corrJNI(in1,in2,buffer);
    }
    public static void execute_beam(double in1[],double in2[],double buffer[],double theta) {
        FFTW.DelayAndSum(in1,in2,buffer,theta);
    }
}
