package com.eneaceolini.utility;

/**
 * Created by Enea on 15/06/15.
 */
public class MakeACopy {

    public static double[] makeACopy(double[] array){
        double[] ret = new double[array.length];
        System.arraycopy(array,0,ret,0,array.length);
        return ret;
    }

    public static byte[] makeACopy(byte[] array){
        byte[] ret = new byte[array.length];
        System.arraycopy(array,0,ret,0,array.length);
        return ret;
    }

    public static int[] makeACopy(int[] array){
        int[] ret = new int[array.length];
        System.arraycopy(array,0,ret,0,array.length);
        return ret;
    }

    public static short[] makeACopy(short[] array){
        short[] ret = new short[array.length];
        System.arraycopy(array,0,ret,0,array.length);
        return ret;
    }


}
