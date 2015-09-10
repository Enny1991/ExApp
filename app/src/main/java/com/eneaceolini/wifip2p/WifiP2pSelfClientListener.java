package com.eneaceolini.wifip2p;

import android.util.Log;

import com.eneaceolini.exapp.SelfLocalization;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;

/**
 * Created by Enea on 02/06/15.
 */
public class WifiP2pSelfClientListener extends Thread {

    private final String TAG = "WifiSELF";
    private SelfLocalization activity;
    public final int WIFIP2P_PORT = 7880;

    public WifiP2pSelfClientListener(SelfLocalization activity){
        this.activity = activity;
    }

    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(WIFIP2P_PORT);
            while(true) {
                byte[] receiveData = new byte[1024];

                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                InetAddress toPass = receivePacket.getAddress();
                Log.d(TAG, "PACKET RECEIVED FROM: " + receivePacket.getAddress());
                Log.d(TAG, "Received " + receivePacket.getLength() + " bytes");
                receiveData = new byte[receivePacket.getLength()];
                System.arraycopy(receivePacket.getData(), 0, receiveData, 0, receivePacket.getLength());
                String msg = new String(receiveData, Charset.forName("ISO-8859-1"));

                Log.d(TAG, msg);
                String MSG[] = msg.split("%");
                //serverSocket.disconnect();
                //serverSocket.close();
                switch (MSG[0]) {
                    case "Connect":
                        activity.addDevice(toPass);
                        break;
                    case "Devices":
                        Log.d(TAG,"Receiced back the list");
                        activity.fillDevices(MSG[1]);
                        break;
                    case "Gotmail":
                            Log.d(TAG,MSG[1]+" - "+MSG[2]);
                        activity.addMessage(MSG[1],MSG[2]);
                        break;
                    default:
                        activity.listDevices(msg);
                        break;

                }

            }

        } catch (Exception e) {
            Log.w("WifiServer", e.toString());
        }
    }
}
