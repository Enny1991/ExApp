package com.eneaceolini.netcon;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Enea on 20/06/15.
 */
public class UDPRunnableLags implements Runnable {

    private InetAddress Iaddress;
    private int port;
    byte[] packet = new byte[8];
    Thread thread;
    DatagramSocket mSocketInt;
    GlobalNotifierUDP monitor;
    DatagramPacket sendPacket;

    public UDPRunnableLags(GlobalNotifierUDP monitor,String add,int port) {
        //TODO set up Socket
        Log.d("UDPLags","Created");
        try {
            Iaddress = InetAddress.getByName(add);
        }catch(Exception e){}
        this.port = port;
        this.monitor = monitor;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            while(true){

                monitor.doWait();
                //Log.d("UDPLags", "Release");
                System.arraycopy(monitor.packetByte, 0, packet, 0, 8); // I free the monitor.packet and I know it's just a double
                try {

                    if (mSocketInt == null) {


                        mSocketInt = new DatagramSocket(null);
                        mSocketInt.setReuseAddress(true);
                        mSocketInt = new DatagramSocket();
                        mSocketInt.connect(Iaddress, port);
                        mSocketInt.setBroadcast(true);
                    }
                } catch (Exception e) {
                    Log.w("UDPCreate start()", e.toString());
                }
                try {


                    sendPacket = new DatagramPacket(packet, 8, Iaddress, port);
                    mSocketInt.send(sendPacket);


                } catch (Exception e) {
                    // Log.w("UDPLAgs",e.toString());
                    //suppress the exception otherwhise the log is full
                    //TODO add an intent filter that tells me when the network is on
                    //Log.w("UDPCreate run()", e.toString());
                }
            }
    }
}
