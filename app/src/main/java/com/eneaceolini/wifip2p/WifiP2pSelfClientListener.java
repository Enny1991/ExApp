package com.eneaceolini.wifip2p;

import android.util.Log;

import com.eneaceolini.exapp.SelfLocalization;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class WifiP2pSelfClientListener extends StopPoolThread {

    private final String TAG = "WifiSELF";
    private SelfLocalization activity;
    public final int WIFIP2P_PORT = 7880;
    public static boolean isRunning = true;
    private DatagramSocket serverSocket;

    public WifiP2pSelfClientListener(SelfLocalization activity){
        this.activity = activity;
    }

    public void run() {
        try {
            serverSocket = new DatagramSocket(WIFIP2P_PORT);
            if(isRunning) {
                while (true) {
                    byte[] receiveData = new byte[1024];

                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    InetAddress toPass = receivePacket.getAddress();
                    //Log.d(TAG, "PACKET RECEIVED FROM: " + receivePacket.getAddress());
                    //Log.d(TAG, "Received " + receivePacket.getLength() + " bytes");
                    receiveData = new byte[receivePacket.getLength()];
                    System.arraycopy(receivePacket.getData(), 0, receiveData, 0, receivePacket.getLength());
                    String msg = new String(receiveData, Charset.forName("ISO-8859-1"));

                    //Log.d(TAG, msg);
                    String MSG[] = msg.split("%");
                    //serverSocket.disconnect();
                    //serverSocket.close();
                    switch (MSG[0]) {
                        case "Connect":
                            activity.addDevice(toPass);
                            break;
                        case "Devices":
                            activity.updateComm("Received List of Devices");
                            Log.d(TAG, "Received back the list");
                            activity.fillDevices(MSG[1]);
                            // send also compass calibration
                            activity.startAngle();
                            break;
                        case "Gotmail":
                            activity.updateComm("Message received");
                            Log.d(TAG, MSG[1] + " - " + MSG[2]);
                            activity.addMessage(MSG[1], MSG[2]);
                            break;
                        case "Activate":
                            activity.updateComm("Activating mic...");
                            activity.activateMic();
                            WifiP2pClientSelf sendReq = new WifiP2pClientSelf(activity, activity.getOwnerAddress());
                            sendReq.setRequest(Requests.CONFIRM_MIC);
                            sendReq.start();
                            break;
                        case "Chirp":
                            Log.d(TAG, "request of playing chirp received");
                            activity.playChirp();
                            break;
                        default:
                            Log.e(TAG, "Message not recognized\n" + msg);
                            break;

                    }

                }
            }

        } catch (Exception e) {
            Log.w("WifiServer", e.toString());
        }
    }

    @Override
    public void destroyMe(){
        isRunning = !isRunning;
        serverSocket.close();
        serverSocket = null;
    }

    @Override
    public String describeMe() {
        return "ClientListener";
    }

    @Override
    public boolean amIRunning() {
        return isRunning;
    }
}
