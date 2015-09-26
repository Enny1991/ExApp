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
public class WifiP2pServerSelf extends Thread {

    private final String TAG = "WifiSELF";
    private SelfLocalization activity;
    public final int WIFIP2P_PORT = 7880;
    public static boolean isRunning = true;

    public WifiP2pServerSelf(SelfLocalization activity){
        this.activity = activity;
    }

    public void run() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(WIFIP2P_PORT);
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
                    Log.d(TAG, "Message" + msg);
                    String MSG[] = msg.split("%");
                    //serverSocket.disconnect();
                    //serverSocket.close();
                    switch (MSG[0]) {
                        case "Connect":
                            activity.updateComm("Connecting " + toPass.toString() + "...");
                            activity.addDevice(toPass);
                            break;
                        case "Devices":
                            // the client device is connected and it is requesting the peers list
                            activity.updateComm("Connection to " + toPass.toString() + " confirmed!");
                            activity.addPingDevice(activity.findInDevices(toPass));
                            WifiP2pClientSelf sendBack = new WifiP2pClientSelf(activity, toPass);
                            sendBack.setRequest(Requests.DEVICES_FROM_OWNER);
                            sendBack.DATA = activity.listDevicesString().getBytes(Charset.forName("ISO-8859-1"));
                            sendBack.start();
                            break;
                        case "Sendtoo":
                            activity.updateComm("Relaying message...");
                            if (!MSG[1].equals(activity.Devices.get(0).getName())) {
                                WifiP2pClientSelf send2 = new WifiP2pClientSelf(activity, activity.findInDevices(MSG[1]));
                                send2.setRequest(Requests.RELAY_MSG);
                                send2.DATA = ("Gotmail" + "%" + activity.findInDevices(toPass) + "%" + MSG[2]).getBytes(Charset.forName("ISO-8859-1"));
                                send2.start();
                            } else {
                                activity.addMessage(activity.findInDevices(toPass), MSG[2]);
                            }
                            break;
                        case "Delay":
                            activity.updateComm("Received Delay from " + toPass.toString());
                            activity.addDelay(activity.findInDevices(toPass), MSG[1]);
                            Log.d(TAG, "Delay received from" + toPass);
                            break;
                        case "Angle":
                            activity.updateComm("Received Orientation from " + toPass.toString());
                            activity.addAngle(toPass, MSG[1]);
                            break;
                        case "Activate":
                            activity.updateComm("Activating mic...");
                            activity.activateMic();
                            WifiP2pClientSelf sendReq = new WifiP2pClientSelf(activity, activity.getOwnerAddress());
                            sendReq.setRequest(Requests.CONFIRM_MIC);
                            sendReq.start();
                            break;
                        case "Confirm":
                            activity.updateComm("Activated " + toPass.toString());
                            activity.checkPlaySnd();
                            break;
                        case "Chirp":
                            Log.d(TAG, "Playing chirp");
                            activity.playChirp();
                            break;
                        case "Sendinf":
                            activity.addInfo(toPass, MSG[1]);
                            break;

                    }

                }
            }

        } catch (Exception e) {
            Log.w("WifiServer", e.toString());
            e .printStackTrace();
        }
    }


    public void stopMe(){
        isRunning=!isRunning;
    }
}
