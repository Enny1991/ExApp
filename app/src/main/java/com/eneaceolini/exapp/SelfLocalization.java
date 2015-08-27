package com.eneaceolini.exapp;

import android.app.Dialog;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eneaceolini.exapp.R;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SelfLocalization extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor  mAccelerometer;
    private Sensor mMagnetometer;
    private TextView result,listAdd;
    private ImageView mPointer,okToGo;
    private ProgressBar progBar;
    private Button start,discovery,seeList,send;
    private ListView listPeers,contacts,messages;
    private EditText editText;
    private WiFiPeerListAdapter la;
    private WiFiPeerListAdapter mWifiPeerListLadapter;
    private WifiP2pManager mWifiManager;
    private WifiP2pManager.Channel mWifiChannel;
    private WifiP2PReceiverSelfOrg receiver;
    private Messages messAdapter;
    private final int PORT_DIRECT = 7880;
    private InetAddress ownerAddress;
    private final IntentFilter p2pFilter = new IntentFilter();
    public boolean isConnected;
    private boolean goRecord = false;
    private int curProg = 0;
    private float[] mean = new float[100];
    private final String TAG = "SelfLocalization";
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private boolean iAmTheOwner;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;
    public Vector<PeerDevice> Devices;
    private List messToPopulate = new ArrayList();
    private List checkDevices = new ArrayList();
    private int deviceCounter = 0;
    private DevicesSpecsAdapter specsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_localization);
        mPointer = (ImageView)findViewById(R.id.pointer);
        progBar = (ProgressBar)findViewById(R.id.progressBar3);
        result = (TextView)findViewById(R.id.result);
        listAdd = (TextView)findViewById(R.id.listAdd);
        start = (Button)findViewById(R.id.button2);
        okToGo = (ImageView)findViewById(R.id.oktogo);
        discovery = (Button)findViewById(R.id.discovery);
        seeList = (Button)findViewById(R.id.seelist);
        send = (Button)findViewById(R.id.send);
        messages = (ListView)findViewById(R.id.messages);
        messAdapter = new Messages(SelfLocalization.this,messToPopulate);
        messages.setAdapter(messAdapter);
        listPeers = (ListView)findViewById(R.id.listView);
        specsAdapter = new DevicesSpecsAdapter(SelfLocalization.this,checkDevices);
        listPeers.setAdapter(specsAdapter);
        editText = (EditText) findViewById(R.id.editText);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(editText.getText().toString());
                Log.d(TAG,"Click to send");
            }
        });

        seeList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(iAmTheOwner){
                    //listDevices();
                    Log.d(TAG,"Devices are "+Devices.size());
                }else{
                    WifiP2pClientSelf sendReq = new WifiP2pClientSelf(SelfLocalization.this,ownerAddress);
                    sendReq.setRequest(Requests.SHOW_DEVICES);
                    sendReq.start();
                }
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAngle();
            }
        });

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        if(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null &&
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            // Do Something
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }else{
            Log.d(TAG, "no magnet sensor or acc sensor!");
        }

        //set filters
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Menage WifiP2P connectivity
        mWifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiChannel = mWifiManager.initialize(this, getMainLooper(), null);

        discovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createConnectionDialog();
            }
        });
    }

    public String getOwnerAddress(){
        return ownerAddress.toString();
    }

    public void addMessage(final String add,final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messToPopulate.add(new MessageLayout(add,msg));
                messAdapter.notifyDataSetChanged();
            }
        });

    }

    public void listDevices(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                listAdd.setText(msg);
            }
        });

    }

    public void listDevices(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String list = "";
                for (int i = 0; i < Devices.size(); i++)
                    list += Devices.elementAt(i).getName() + " - " + Devices.elementAt(i).getAddress().toString();
                Log.d(TAG, "Showing Devices #" + Devices.size());
                listAdd.setText(list);
            }
        });

    }

    public void fillDevices(String msg){

        Devices = new Vector<>();
        String[] names = msg.split("\\?");
        Log.d(TAG, "filling devices " + names.length);
        for(int i = 0;i<names.length;i++)
            Devices.add(new PeerDevice(null,names[i]));
    }

    public String listDevicesString(){
        String list = "Devices%";
        for(int i = 0;i<Devices.size();i++)
            list += Devices.get(i).getName()+"?";

        return list;
    }

    public String listDevicesString(InetAddress avoid){
        String list = "Devices%";
        for(int i = 0;i<Devices.size();i++)
            if(!avoid.toString().equals(Devices.get(i).getAddress().toString()))list += Devices.get(i).getName()+"?";

        return list;
    }

    public void addInfo(InetAddress to,String info){
        for(int i = 0;i<Devices.size();i++){
            if(Devices.get(i).getAddress().toString().equals(to.toString())) {
                Devices.get(i).setInfo(info);
                break;
            }

        }

    }
    Dialog dialog;
    public void sendMessage(final String msg){
        dialog = new Dialog(SelfLocalization.this, 0);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.sent_to);
        ListView contacts = (ListView)dialog.findViewById(R.id.listViewContacts);
        List peers = new ArrayList();

            for (int i = 0; i < Devices.size(); i++)
                peers.add(Devices.get(i).getName());

        ContactsAdapter adapter = new ContactsAdapter(SelfLocalization.this,peers);
        contacts.setAdapter(adapter);
        dialog.show();
        contacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String to = ((String)parent.getAdapter().getItem(position));
                WifiP2pClientSelf sendReq = new WifiP2pClientSelf(SelfLocalization.this,ownerAddress);
                sendReq.setRequest(Requests.RELAY_MSG);

                sendReq.DATA = ("Sendtoo%"+(to)+"%"+msg).getBytes(Charset.forName("ISO-8859-1"));
                sendReq.start();
                dialog.dismiss();
            }
        });

    }

    public Dialog connectionDialog;
    ListView choosePeer;
    public void createConnectionDialog(){
        connectionDialog = new Dialog(SelfLocalization.this, 0);
        connectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        connectionDialog.setContentView(R.layout.sent_to);
        choosePeer = (ListView)connectionDialog.findViewById(R.id.listViewContacts);

        mWifiManager.discoverPeers(mWifiChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                //Log.d(TAG, "Start discovery Wifi P2P");
            }

            @Override
            public void onFailure(int reasonCode) {

            }


        });
        connectionDialog.show();
    }

    public InetAddress findInDevices(String to){
        for(int i = 0;i<Devices.size();i++)
            if(Devices.get(i).getName().equals(to))
                return Devices.get(i).getAddress();
        return null;
    }

    public String findInDevices(InetAddress to){
        for(int i = 0;i<Devices.size();i++)
            if(Devices.get(i).getAddress().toString().equals(to.toString()))
                return Devices.get(i).getName();
        return null;
    }

    public boolean addDevice(final InetAddress toAdd)
    {
        PeerDevice mDevice = new PeerDevice(toAdd,DeviceName.names[deviceCounter++]);
        mDevice.setOwnerAddress(ownerAddress);
        Devices.add(mDevice);
        Log.d(TAG, "device added");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkDevices.add(new String(toAdd.toString()));
                specsAdapter.notifyDataSetChanged();
            }
        });

        // at every new i broadcast the list
        for(int i = 1;i<Devices.size();i++){
            WifiP2pClientSelf sendBack = new WifiP2pClientSelf(SelfLocalization.this,Devices.get(i).getAddress());
            sendBack.setRequest(Requests.DEVICES_FROM_OWNER);
            sendBack.DATA = listDevicesString(Devices.get(i).getAddress()).getBytes(Charset.forName("ISO-8859-1"));
            sendBack.start();
        }

        return true;
    }
    // triggered if i am the owner
    public boolean createGroup(InetAddress owner,boolean iAm){
        try {
            if(Devices == null) {
                iAmTheOwner = iAm;
                Devices = new Vector<>();
                ownerAddress = owner;
                if(iAm)addDevice(owner);
            }
            return true;
        }catch(Exception e){
            Log.d(TAG,"list of devices not created");
            return false;
        }
    }

    private void startAngle(){
        okToGo.setVisibility(View.GONE);
        goRecord = true;
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    private void endAngle(){
        double[] res = meanVariance(mean);

        result.setText(String.format("Mean = %.2f \n STD = %.2f", res[0], res[1]));
        if(res[1] < 2) {
            okToGo.setImageResource(R.mipmap.ic_right);
            okToGo.setVisibility(View.VISIBLE);
        }else{
            okToGo.setVisibility(View.VISIBLE);
            mean = new float[100];
            curProg = 0;
            progBar.setProgress(0);
        }
        //see if it's valid or not;
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
    }



    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.

    }
    @Override

    public final void onSensorChanged(SensorEvent event) {


        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {


            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            RotateAnimation ra = new RotateAnimation(
                    mCurrentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            ra.setDuration(250);

            ra.setFillAfter(true);

            mPointer.startAnimation(ra);
            mCurrentDegree = -azimuthInDegress;

            if(goRecord) {
                if(curProg==100) {
                    endAngle();
                    return;
                }else {
                    progBar.setProgress(++curProg);
                    mean[curProg - 1] = 360 - azimuthInDegress;
                }
            }
        }
        //azimuth.setText(""+event.values[0]);
        //pitch.setText(""+event.values[1]);
        //roll.setText(""+event.values[2]);

    }

    public double[] meanVariance(float[] x){
        double[] meanStd = new double[2];
        int samples = x.length;
        for(int i = 0;i < samples;i++){
            meanStd[0] += x[i];
        }
        meanStd[0] = meanStd[0]/samples;
        for(int i = 0;i < samples;i++){
            meanStd[1] += Math.pow(x[i] - meanStd[0],2);
        }

        meanStd[1] = Math.sqrt(meanStd[1]/samples);

        return meanStd;
    }

    public void setWifiPeerListLadapter(List wifiPeerListLadapter) {
        this.mWifiPeerListLadapter = new WiFiPeerListAdapter(SelfLocalization.this, wifiPeerListLadapter);
        //progBar.setVisibility(View.INVISIBLE);
    }

    public void showPeersList(List adapter)
    {


        la = new WiFiPeerListAdapter(SelfLocalization.this, adapter);
        choosePeer.setAdapter(la);

        //if(mWifiPeerListLadapter != null)
        //listPeers.setAdapter(mWifiPeerListLadapter);
        choosePeer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final WifiP2pDevice device = (WifiP2pDevice) mWifiPeerListLadapter.getItem(position);
                mWifiPeerListLadapter.removeItem(position);
                mWifiPeerListLadapter.notifyDataSetChanged();

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                if (isConnected) {

                    if (mWifiManager != null && mWifiChannel != null) {


                        mWifiManager.requestGroupInfo(mWifiChannel, new WifiP2pManager.GroupInfoListener() {
                            @Override
                            public void onGroupInfoAvailable(WifiP2pGroup group) {
                                if (group != null && mWifiManager != null && mWifiChannel != null
                                        ) {

                                    mWifiManager.removeGroup(mWifiChannel, new WifiP2pManager.ActionListener() {


                                        @Override
                                        public void onSuccess() {
                                            //I am removing myself or a node from the network
                                            //I need to retrieve its address and take it out from
                                            for(int i = 0;i<Devices.size();i++)
                                                if(device.deviceAddress.equals(Devices.get(i).getName().toString()))
                                                    Devices.remove(i);
                                        }

                                        @Override
                                        public void onFailure(int reason) {
                                            Log.d(TAG, "removeGroup onFailure -" + reason);
                                        }
                                    });
                                }
                            }
                        });
                    }
                    isConnected = false;
                } else {


                    //mWifiManager.createGroup();
                    mWifiManager.connect(mWifiChannel, config, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            connectionDialog.dismiss();
                            //unregisterReceiver(receiver);
                            // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(SelfLocalization.this, "Connect failed. Retry.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
        });


    }

    //provide service via WIfiP2P
/*
    private void startRegistration() {
        //  Create a string map containing information about your service.
        Map record = new HashMap();
        record.put("listenport", String.valueOf(PORT_DIRECT));
        record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
        record.put("available", "visible");

        // Service information.  Pass it an instance name, service type
        // _protocol._transportlayer , and the map containing
        // information other devices will want once they connect to this one.
        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mWifiManager.addLocalService(mWifiChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Command successful! Code isn't necessarily needed here,
                // Unless you want to update the UI or add logging statements.
            }

            @Override
            public void onFailure(int arg0) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
    }
*/
    @Override
    protected void onResume() {
        super.onResume();

        receiver = new WifiP2PReceiverSelfOrg(mWifiManager, mWifiChannel, this);
        registerReceiver(receiver, p2pFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this,mAccelerometer);
        mSensorManager.unregisterListener(this,mMagnetometer);
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_self_localization, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class MessageLayout{
        public String message,name;
        public MessageLayout(String message,String name){
            this.name = name;
            this.message = message;
        }
    }

public class PeerDevice{

    private InetAddress address,ownerAddress;
    private String name,info;

    public PeerDevice(InetAddress  address,String name){
        this.address = address;
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public void setInfo(String info){
        this.info = info;
    }

    public String getInfo(){
        return this.info;
    }

    public InetAddress getAddress(){
        return this.address;
    }

    public void setAddress(InetAddress newAddress){
        this.address = newAddress;
    }

    public void setName(String newName){
        this.name = newName;
    }

    public void setOwnerAddress(InetAddress owner){
        ownerAddress = address;
    }
}


}
