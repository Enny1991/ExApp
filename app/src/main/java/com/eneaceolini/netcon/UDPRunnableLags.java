package com.eneaceolini.netcon;

import android.util.Log;

import com.eneaceolini.exapp.MainActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Enea on 20/06/15.
 */
public class UDPRunnableLags implements Runnable {

    private InetAddress Iaddress;
    private InetAddress IaddressDirect;
    private int port;
    private int portDirect;
    private MainActivity activity;
    byte[] packet = new byte[16];
    Thread thread;
    DatagramSocket mSocketInt;
    DatagramSocket mSocketIntDirect;
    GlobalNotifierUDP monitor;
    DatagramPacket sendPacket;

    public UDPRunnableLags(GlobalNotifierUDP monitor,String add,int port, int portDirect, InetAddress addDirect,MainActivity activity) {
        //TODO set up Socket
        Log.d("UDPLags","Created");
        try {
            Iaddress = InetAddress.getByName(add);
        }catch(Exception e){}
        this.IaddressDirect = addDirect;
        this.port = port;
        this.monitor = monitor;
        this.activity = activity;
        this.portDirect = portDirect;
        Log.d("Creating UDP ", "" + this.IaddressDirect);
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
                System.arraycopy(monitor.packetByte, 0, packet, 0, 16); // I free the monitor.packet and I know it's just a double
                try {

                    if (mSocketInt == null) {


                        mSocketInt = new DatagramSocket(null);
                        mSocketInt.setReuseAddress(true);
                        mSocketInt = new DatagramSocket();
                        mSocketInt.connect(Iaddress, port);
                        mSocketInt.setBroadcast(true);
                    }
                } catch (Exception e) {
                    Log.w("UDPCreate 1 start()", e.toString());
                }
                try {


                    sendPacket = new DatagramPacket(packet, 16, Iaddress, port);
                    mSocketInt.send(sendPacket);


                } catch (Exception e) {
                    // Log.w("UDPLAgs",e.toString());
                    //suppress the exception otherwhise the log is full
                    //TODO add an intent filter that tells me when the network is on
                    //Log.w("UDPCreate run()", e.toString());
                }

                try {

                    if (mSocketIntDirect == null) {


                        mSocketIntDirect = new DatagramSocket(null);
                        mSocketIntDirect.setReuseAddress(true);
                        mSocketIntDirect = new DatagramSocket();
                        mSocketIntDirect.connect(IaddressDirect, portDirect);
                        mSocketIntDirect.setBroadcast(true);
                    }
                } catch (Exception e) {
                    Log.w("UDPCreate 2 start()", e.toString());
                }
                try {


                    sendPacket = new DatagramPacket(packet, 16, IaddressDirect, portDirect);
                    mSocketIntDirect.send(sendPacket);


                } catch (Exception e) {
                    // Log.w("UDPLAgs",e.toString());
                    //suppress the exception otherwhise the log is full
                    //TODO add an intent filter that tells me when the network is on
                    //Log.w("UDPCreate run()", e.toString());
                }

            }
    }
}
