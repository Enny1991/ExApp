package com.eneaceolini.wifip2p;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.ListAdapter;

import com.eneaceolini.exapp.MainActivity;
import com.eneaceolini.exapp.SelfLocalization;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


public class WifiP2PReceiverMain extends BroadcastReceiver {

    private final String TAG = "WifiP2PReceiver";
    MainActivity activity;
    SelfLocalization selfLocalization;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    private List peers = new ArrayList();


    public WifiP2PReceiverMain(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, SelfLocalization selfLocalization){
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.selfLocalization = selfLocalization;
        activity.setWifiPeerListLadapter(peers);
    }

    public WifiP2PReceiverMain(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity activity){
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.activity = activity;
        activity.setWifiPeerListLadapter(peers);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (mManager != null) {
                mManager.requestPeers(mChannel, peerListListener);

            }


            // The peer list has changed!  We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (mManager == null) {
                return;
            }
            Log.d(TAG, "Connection changed");
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                Log.d(TAG, "Effectively connected");
                // We are connected with the other device, request connection
                // info to find group owner IP
                mManager.requestConnectionInfo(mChannel, connectionListener);
            }else{
                Log.d(TAG, "Not connected");
            }




            // Connection state changed!  We should probably do something about
            // that.

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG,"Device change action");

        }
    }



    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            // Out with the old, in with the new.
            peers.clear();
            peers.addAll(peerList.getDeviceList());

            activity.showPeersList(peers);

            Log.d(TAG+" # peers",""+peers.size());
            if (peers.size() == 0) {
                Log.d(TAG, "No devices found");

                //activity.resetAdapterPeersList();
            }else{
                activity.setWifiPeerListLadapter(peers);
                Log.d(TAG,"More");
            }

        }
    };

    private ListAdapter getListAdapter(){
        return activity.getWifiPeerListLadapter();
    }


    private WifiP2pManager.ConnectionInfoListener connectionListener = new WifiP2pManager.ConnectionInfoListener() {

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            // InetAddress from WifiP2pInfo struct.
            InetAddress groupOwnerAddress = null;
            try {
                groupOwnerAddress = info.groupOwnerAddress;
                Log.d(TAG, "ADDRESS "+groupOwnerAddress.toString());
            }catch(Exception e){Log.w("owner address",e.toString());}
            // After the group negotiation, we can determine the group owner.
            if (info.groupFormed && info.isGroupOwner) {
                Log.d(TAG, "Connection Established: I am the owner");
                activity.isConnected = true;
                //activity.setDirectWifiPeerAddress(groupOwnerAddress);
                new WifiP2pServer(activity).start();

            } else if (info.groupFormed) {
                activity.isConnected = true;
                //activity.setDirectWifiPeerAddress(groupOwnerAddress);
                //Log.d(TAG, "Connection Established I am a client");
                new WifiP2pClient(activity,groupOwnerAddress).start();
                // Everyone knows the owner address but no one knows the proper address, the packets
                // exchange lead to this.
            }
        }
    };


}
