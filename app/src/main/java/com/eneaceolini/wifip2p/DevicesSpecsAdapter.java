package com.eneaceolini.wifip2p;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.eneaceolini.exapp.R;
import com.eneaceolini.exapp.SelfLocalization;

import java.util.List;
import java.util.Vector;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class DevicesSpecsAdapter extends BaseAdapter {
    private final Context context;
    private final Vector<PeerDevice> peers;
    SelfLocalization activity;
    private static final String TAG = "DevicesSpecsAdapter";


    public DevicesSpecsAdapter(Context context, Vector<PeerDevice> peers,SelfLocalization activity){
        this.context = context;
        this.peers = peers;
        this.activity = activity;

    }

    @Override
    public int getCount() {
        return peers.size();
    }

    public void removeItem(int position){
        peers.remove(position);
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
        final int pos = position;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.check_list,parent,false);
        TextView name = (TextView)row.findViewById(R.id.name);
        name.setText(peers.get(position).getName());
        ImageView delay = (ImageView)row.findViewById(R.id.status_delay);
        ImageView angle = (ImageView)row.findViewById(R.id.status_angle);
        ImageView ping = (ImageView)row.findViewById(R.id.status_name); //has it receive a name?
        if(peers.get(position).isDelayAvailable())
            delay.setImageResource(R.mipmap.ic_yep);

        if(peers.get(position).isAngleAvailable() == Requests.COMP_RECEIVED)
            angle.setImageResource(R.mipmap.ic_yep);
        else if (peers.get(position).isAngleAvailable() == Requests.NO_COMP)
            angle.setImageResource(R.mipmap.ic_unav);

        if(peers.get(position).isPingAvailable())
            ping.setImageResource(R.mipmap.ic_yep);
        ImageView reqchirp = (ImageView)row.findViewById(R.id.reqchirp);
        reqchirp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.sendActReq(peers.get(pos).getAddress());
                Log.d(TAG, "req chirp from " + peers.get(pos).getAddress());
            }
        });
        return row;
    }

    public void setName(String s,int i){
        peers.get(i).setName(s);
    }

    public String getName(int i){
        return peers.get(i).getName();
    }
}
