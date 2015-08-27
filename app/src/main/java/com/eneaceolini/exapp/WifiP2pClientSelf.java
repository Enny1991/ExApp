package com.eneaceolini.exapp;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;

/**
 * Created by Enea on 02/06/15.
 */
public class WifiP2pClientSelf extends Thread {

    private final String TAG = "WifiP2pClientSELF";
    private SelfLocalization activity;
    public final int WIFIP2P_PORT = 7880;
    private InetAddress serverAddress;
    private DatagramSocket mSocket;
    private int request = 0;
    public byte[] DATA;

    public WifiP2pClientSelf(SelfLocalization activity, InetAddress ad){
        this.serverAddress = ad;
        this.activity = activity;
    }

    public void setRequest(int req){
        this.request = req;
    }

    public void run() {
        byte data[];
        try {
            if (mSocket == null) {
                mSocket = new DatagramSocket(null);
                mSocket.setReuseAddress(true);
                mSocket = new DatagramSocket();
                mSocket.connect(serverAddress, WIFIP2P_PORT);
                mSocket.setBroadcast(true);
            }


            switch(request){
                case Requests.CONNECT:
                    Log.d(TAG,"Sending Connect from client");
                    data = ("Connect").getBytes(Charset.forName("ISO-8859-1"));
                    Log.d(TAG,"Sent "+data.length+" bytes");
                    break;
                case Requests.SHOW_DEVICES:
                    Log.d(TAG,"Sending see list from client");
                    data = ("Devices").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                case Requests.SEND_MESSAGE:
                    data = ("Message").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                case Requests.DEVICES_FROM_OWNER:
                    data = DATA;
                    break;
                case Requests.RELAY_MSG:
                    data = DATA;
                    break;
                default:
                    data = ("Devices").getBytes(Charset.forName("ISO-8859-1"));
                    break;
            }

            DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, WIFIP2P_PORT);
            mSocket.send(sendPacket);
            Log.d(TAG,"CLIENT PACKET SENT");




        }catch(Exception e){
            Log.w("CLIENT",e.toString());
        }
    }
}
