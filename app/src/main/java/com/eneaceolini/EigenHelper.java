package com.eneaceolini;

/**
 * Created by enea on 16/02/16.
 * Project COCOHA
 */
public class EigenHelper {
    private static final String TAG ="EigenHelper";
    static {
        System.loadLibrary("test_eigen");
    }

    public static float[] executeLocalization(float[] tdoas, float t_x, float t_y, float rot, float dist){
        return EigenHelper.localization(tdoas, t_x, t_y, rot, dist);
    }

    private static native float[] localization(float[] tdoas, float t_x, float t_y, float rot, float dist);

    private static native float[] linearRegression(float[] x, float[] y);

    public static float[] executeLinearRegression(float[] x, float[] y) {
        float[] reshapeX = new float[2 * y.length];
        for (int j = 0; j < y.length; j++) {
                reshapeX[j] = x[j];
        }
        for (int j = 0; j < y.length; j++) {
            reshapeX[y.length + j] = 1.0f;
        }

        return EigenHelper.linearRegression(reshapeX, y);
    }
}
