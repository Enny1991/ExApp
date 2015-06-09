package com.eneaceolini.exapp;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Enea on 02/06/15.
 */
public class WifiP2pServer extends Thread {

    private MainActivity activity;
    public final int WIFIP2P_PORT = 7880;

    public WifiP2pServer(MainActivity activity){
        this.activity = activity;
    }

    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(WIFIP2P_PORT);
            byte[] receiveData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                InetAddress toPass = receivePacket.getAddress();
                Log.d("SERVER","PACKET RECEIVED FROM: "+receivePacket.getAddress());
                serverSocket.disconnect();
                serverSocket.close();
                serverSocket = null;
                activity.setDirectWifiPeerAddress(toPass);

            }
        } catch (Exception e) {
            Log.w("WifiServer", e.toString());
        }
    }
}
