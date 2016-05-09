package com.eneaceolini.netcon;

import com.eneaceolini.utility.Constants;

/**
 * Created by Enea on 20/06/15.
 */
public class GlobalNotifierUDP{

    MonitorObject myMonitorObject = new MonitorObject();
    public boolean wasSignalled = false;
    public short[] packet = new short[Constants.FRAME_SIZE / 2]; //Max length of bugffer audio
    public byte[] packetByte = new byte[16];
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

