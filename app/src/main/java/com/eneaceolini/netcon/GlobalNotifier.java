package com.eneaceolini.netcon;

import com.eneaceolini.utility.Constants;

/**
 * Created by Enea on 20/06/15.
 */
public class GlobalNotifier{

    MonitorObject myMonitorObject = new MonitorObject();
    boolean wasSignalled = false;
    short[] packet = new short[Constants.FRAME_SIZE/2]; //Max length of buffer audio

    public int length;

    public void doWait(){
        synchronized(myMonitorObject){
            while(!wasSignalled){
                try{
                    myMonitorObject.wait();
                } catch(InterruptedException e){}
            }
            //clear signal and continue running.
            wasSignalled = false;
        }
    }

    public void doNotify(){
        synchronized(myMonitorObject){
            wasSignalled = true;
            myMonitorObject.notify();
        }
    }
}
