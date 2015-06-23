package com.eneaceolini.exapp;

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

    UDPRunnableLags(GlobalNotifierUDP monitor,String add,int port) {
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



            /*
            try {

                mSocketInt = new DatagramSocket(null);
                mSocketInt.setReuseAddress(true);
                mSocketInt = new DatagramSocket();
                mSocketInt.connect(Iaddress, port);
                mSocketInt.setBroadcast(true);
            } catch (Exception e) {
                Log.w("UDPCreate start()", e.toString());
            }

            try {


                DatagramPacket sendPacket = new DatagramPacket(data, data.length, Iaddress, port);
                mSocketInt.send(sendPacket);
                //Update total data sent
                //KBytesSent += (1.0 * data.length)/1000;
                //connectionLost = false;

            } catch (Exception e) {
                Log.w("UDPCreate run()", e.toString());
            }
*/              //monitor.doWait();

                       // Log.d("UDPRunnableLags", "sending..."+monitor.packet);



    }

    private static byte[] short2byte(short[] sData) {

        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }
        return bytes;

    }
}
