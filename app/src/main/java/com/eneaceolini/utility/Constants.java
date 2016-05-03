package com.eneaceolini.utility;

/**
 * Created by Enea on 04/06/15.
 */
public class Constants {
    public static final int LENGTH_FFT = 2048;
    public static final int FRAME_SIZE = LENGTH_FFT * 2; // This / 2 is the max size of the audio buffer
    public static final int LENGTH_SINGLE_CH = LENGTH_FFT / 2;
}

