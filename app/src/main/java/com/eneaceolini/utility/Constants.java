package com.eneaceolini.utility;

/**
 * Created by Enea on 04/06/15.
 */
public class Constants {
    public static final int SINGLE_SAMPLE = 512; // this is how much i take from the buffer at each time
    public static final int FRAME_SIZE = SINGLE_SAMPLE * 2; // This / 2 is the max size of the audio buffer
    public static final int LENGTH_SINGLE_CH = SINGLE_SAMPLE / 2;
    public static final int MIN_NUM_SAMPLES = SINGLE_SAMPLE; //This is the size of the fft
}

