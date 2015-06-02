package com.eneaceolini.exapp;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by Enea on 02/06/15.
 */
public class WiFiPeerListAdapter extends BaseAdapter {
    private Context context;
    private List peers;
    private WifiP2pDevice device;


    public WiFiPeerListAdapter(Context context,List list){
        this.context = context;
        this.peers = list;

    }

    @Override
    public int getCount() {
        return peers.size();
    }

    @Override
    public Object getItem(int position) {
        return peers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.list_item,parent,false);
        TextView mainText = (TextView)row.findViewById(R.id.firstLine);
        TextView seconText = (TextView)row.findViewById(R.id.secondLine);
        device = (WifiP2pDevice)peers.get(position);
        ImageView icon = (ImageView)row.findViewById(R.id.icon);
        mainText.setText(device.deviceName);
        switch(device.status){
            case WifiP2pDevice.AVAILABLE:
                seconText.setText("Available");
                break;
            case WifiP2pDevice.INVITED:
                seconText.setText("Invited");
                break;
            case WifiP2pDevice.CONNECTED:
                seconText.setText("Connected");
                break;
            case WifiP2pDevice.UNAVAILABLE:
                seconText.setText("Unavailable");
                icon.setImageDrawable(context.getResources().getDrawable(R.mipmap.ic_net_off));
                break;
        }

        return row;
    }
}
