package com.eneaceolini;

import android.util.Log;

/**
 * Created by enea on 16/02/16.
 * Project COCOHA
 */
public class EigenHelper {
    private static final String TAG ="EigenHelper";
    static {
        System.loadLibrary("test_eigen");
    }

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
