package com.eneaceolini.wifip2p;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.eneaceolini.exapp.R;

import java.util.List;

/**
 * Created by Enea on 02/06/15.
 */
public class ContactsAdapter extends BaseAdapter {
    private final Context context;
    private final List peers;


    public ContactsAdapter(Context context, List list){
        this.context = context;
        this.peers = list;

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
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.item_contact,parent,false);
        TextView mainText = (TextView)row.findViewById(R.id.firstLine);
        TextView seconText = (TextView)row.findViewById(R.id.secondLine);
        String device = (String)peers.get(position);
        //ImageView icon = (ImageView)row.findViewById(R.id.icon);
        mainText.setText(device);


        return row;
    }
}
