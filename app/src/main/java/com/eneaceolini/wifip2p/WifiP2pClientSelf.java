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
                    activity.updateComm("Connecting with Server...");
                    data = ("Connect").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                case Requests.SHOW_DEVICES:
                    activity.updateComm("Asking for devices list...");
                    data = ("Devices").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                case Requests.SEND_MESSAGE:
                    activity.updateComm("Sending Message...");
                    data = ("Message").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                case Requests.DEVICES_FROM_OWNER:
                    activity.updateComm("Asking for devices list...");
                    data = DATA;
                    break;
                case Requests.RELAY_MSG:
                    activity.updateComm("Transmitting message... ");
                    data = DATA;
                    break;
                case Requests.DELAY_CHIRP:
                    activity.updateComm("Sending delay chirp...");
                    data = DATA;
                    break;
                case Requests.ANGLE:
                    activity.updateComm("Sending Orientation...");
                    data = DATA;
                    break;
                case Requests.ACTIVATE_MIC:
                    activity.updateComm("Request of activation...");
                    data = ("Activate%").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                case Requests.CONFIRM_MIC:
                    activity.updateComm("Confirm activation...");
                    data = ("Confirm%").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                case Requests.PLAY_CHIRP:
                    activity.updateComm("Activating chirp in the designated device...");
                    data = ("Chirp%").getBytes(Charset.forName("ISO-8859-1"));
                    break;
                default:
                    data = ("Devices").getBytes(Charset.forName("ISO-8859-1"));
                    break;
            }

            DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, WIFIP2P_PORT);
            mSocket.send(sendPacket);




        }catch(Exception e){
            Log.w("CLIENT",e.toString());
        }
    }
}
