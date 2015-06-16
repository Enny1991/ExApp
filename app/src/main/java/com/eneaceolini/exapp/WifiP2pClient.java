package com.eneaceolini.exapp;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Enea on 02/06/15.
 */
public class WifiP2pClient extends Thread {

    private final String TAG = "WifiP2pClient";
    private MainActivity activity;
    public final int WIFIP2P_PORT = 7880;
    private InetAddress serverAddress;
    private DatagramSocket mSocket;

    public WifiP2pClient(MainActivity activity,InetAddress ad){
        this.serverAddress = ad;
        this.activity = activity;
    }

    public void run() {
        try {
            byte[] data = ("Blank").getBytes();
            if (mSocket == null) {
                mSocket = new DatagramSocket(null);
                mSocket.setReuseAddress(true);
                mSocket = new DatagramSocket();
                mSocket.connect(serverAddress, WIFIP2P_PORT);
                mSocket.setBroadcast(true);
            }

            //DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            //serverSocket.receive(receivePacket);
            //String sentence = new String(receivePacket.getData());
            //System.out.println("RECEIVED: " + sentence);

            //int port = receivePacket.getPort();
            //String capitalizedSentence = sentence.toUpperCase();
            //sendData = capitalizedSentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, WIFIP2P_PORT);
            mSocket.send(sendPacket);
            Log.d(TAG,"CLIENT PACKET SENT");

            activity.setDirectWifiPeerAddress(serverAddress);



        }catch(Exception e){
            Log.w("CLIENT",e.toString());
        }
    }
}
