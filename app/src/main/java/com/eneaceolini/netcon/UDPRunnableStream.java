package com.eneaceolini.netcon;

/**
 * Created by Enea on 21/06/15.
 */


import android.os.Environment;
import android.util.Log;

import com.eneaceolini.utility.Constants;
import com.eneaceolini.exapp.MainActivity;

import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Enea on 20/06/15.
 */
public class UDPRunnableStream implements Runnable {

    private InetAddress Iaddress;
    private int port;
    public InetAddress IaddressDirect;
    private int portDirect;
    short[] packet = new short[Constants.FRAME_SIZE/2];
    private byte[] packetByte = new byte[Constants.FRAME_SIZE];
    String fileName;
    FileOutputStream os;
    private boolean go = false;
    private boolean go2 = false;

    Thread thread;
    DatagramSocket mSocketInt;
    DatagramSocket mSocketIntDirect;
    GlobalNotifierUDP monitor;
    GlobalNotifier doubleBackFire;
    DatagramPacket sendPacket;
    DatagramPacket sendPacketDirect;
    MainActivity activity;

    public UDPRunnableStream(GlobalNotifier bf,GlobalNotifierUDP monitor,String add,InetAddress addDirect,int port, int portDir,MainActivity act) {
        //TODO set up Socket
        Log.d("UDPLags", "Created");
        activity = act;
        try {
            Iaddress = InetAddress.getByName(add);
        }catch(Exception e){}
        this.IaddressDirect = addDirect;
        this.portDirect = portDir;
        this.port = port;
        this.monitor = monitor;
        this.doubleBackFire = bf;
    }

    public void start() {
        fileName = Environment.getExternalStorageDirectory().getPath()+"/myrecord.pcm";
        try {
            os = new FileOutputStream(fileName);
        }catch(Exception e){
            Log.w("os","definition");
        }
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while(true){

            monitor.doWait();
            //Log.d("UDPStream","Release");
                System.arraycopy(monitor.packet, 0, packet, 0, monitor.length);
            // I free the monitor.packet


            //short[] a = new short[Constants.FRAME_SIZE/4];
            //for(int i = 0;i<Constants.FRAME_SIZE/4;i++) a[i] = packet[2*i];
            //TODO other impl of ths might be faster but take more RAM

            if(activity.streamToServer1 && activity.streamToServer2) {
                short2byte(packet,packetByte);
                go = true;
            }
            else if(activity.streamToServer1){
                for(int i = 0;i<Constants.FRAME_SIZE/2;i+=2)packet[i+1]=0;
                short2byte(packet,packetByte);
                go = true;
            } else
                if(activity.streamToServer2){
                    for(int i = 0;i<Constants.FRAME_SIZE/2;i+=2)packet[i]=0;
                    short2byte(packet,packetByte);
                    go = true;
                } else go = false;

            if(activity.streamToPeer1 && activity.streamToPeer2) {
                short2byte(packet,packetByte);
                go2 = true;
            }
            else if(activity.streamToPeer1){
                for(int i = 0;i<Constants.FRAME_SIZE/2;i+=2)packet[i+1]=0;
                short2byte(packet,packetByte);
                go2 = true;
            } else
            if(activity.streamToPeer2){
                for(int i = 0;i<Constants.FRAME_SIZE/2;i+=2)packet[i]=0;
                short2byte(packet,packetByte);
                go2 = true;
            } else go2 = false;




            doubleBackFire.doNotify();

            if(go) {
                try {

                    if (mSocketInt == null) {

                        mSocketInt = new DatagramSocket(null);
                        mSocketInt.setReuseAddress(true);
                        mSocketInt = new DatagramSocket();
                        mSocketInt.connect(Iaddress, port);
                        mSocketInt.setBroadcast(true);
                    }

                    sendPacket = new DatagramPacket(packetByte, packetByte.length, Iaddress, port);
                    mSocketInt.send(sendPacket);
                    activity.updateGraphs(packetByte.length/1000);



                } catch (Exception e) {
                    Log.w("UDPCreate start()", e.toString());
                }
            }
            if(go2){
                try {

                    if (mSocketIntDirect == null) {

                        mSocketIntDirect = new DatagramSocket(null);
                        mSocketIntDirect.setReuseAddress(true);
                        mSocketIntDirect = new DatagramSocket();
                        mSocketIntDirect.connect(IaddressDirect, portDirect);
                        mSocketIntDirect.setBroadcast(true);

                    }



                    if (IaddressDirect != null) {
                        sendPacketDirect = new DatagramPacket(packetByte, packetByte.length, IaddressDirect, portDirect);
                        mSocketIntDirect.send(sendPacketDirect);
                        activity.updateGraphs(packetByte.length/1000);
                    }

                    //Update total data sent
                    //KBytesSent += (1.0 * data.length)/1000;
                    //connectionLost = false;

                } catch (Exception e) {
                    //suppress the exception otherwhise the log is full
                    //TODO add an intent filter that tells me when the network is on
                    //Log.w("UDPCreate run()", e.toString());
                }
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

    private void short2byte(short[] sData,byte[] bytes) {

        int shortArrsize = sData.length;
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }


    }
}

