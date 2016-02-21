package com.eneaceolini.wifip2p;

import android.util.Log;

import com.eneaceolini.exapp.MainActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class WifiP2pServer extends Thread {

    private final String TAG = "WifiP2pServer";
    private MainActivity activity;
    public final int WIFIP2P_PORT = 7880;

    public WifiP2pServer(MainActivity activity){
        this.activity = activity;
    }

    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(WIFIP2P_PORT);
            byte[] receiveData = new byte[1024];

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                InetAddress toPass = receivePacket.getAddress();
                Log.d(TAG,"PACKET RECEIVED FROM: " + receivePacket.getAddress());
                serverSocket.disconnect();
                serverSocket.close();
                activity.setDirectWifiPeerAddress(toPass);
            //Log.e("WRONG SERVER","detected launch of wrong server");


        } catch (Exception e) {
            Log.w("WifiServer", e.toString());
        }
    }
}
