package com.eneaceolini.exapp;

import android.app.Dialog;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Environment;
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
import android.widget.TextView;
import android.widget.Toast;

import com.eneaceolini.audio.LocalizeOwnSpk;
import com.eneaceolini.fft.FFTHelper;
import com.eneaceolini.utility.Constants;
import com.eneaceolini.utility.GraphView;
import com.eneaceolini.wifip2p.ContactsAdapter;
import com.eneaceolini.wifip2p.DeviceName;
import com.eneaceolini.wifip2p.DevicesSpecsAdapter;
import com.eneaceolini.wifip2p.Messages;
import com.eneaceolini.wifip2p.Requests;
import com.eneaceolini.wifip2p.StopPoolThreadAdv;
import com.eneaceolini.wifip2p.WiFiPeerListAdapter;
import com.eneaceolini.wifip2p.WifiP2PReceiverSelf;
import com.eneaceolini.wifip2p.WifiP2pClientSelf;
import com.eneaceolini.wifip2p.PeerDevice;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SelfLocalization extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private TextView result, listAdd, comm;
    private ImageView okToGo;
    private Button  discovery, seeList, chirp;
    private ListView listPeers, contacts, messages;
    private EditText editText;
    private WiFiPeerListAdapter la;
    private WiFiPeerListAdapter mWifiPeerListLadapter;
    private WifiP2pManager mWifiManager;
    private WifiP2pManager.Channel mWifiChannel;
    private WifiP2PReceiverSelf receiver;
    private Messages messAdapter;
    private final int PORT_DIRECT = 7880;
    private final int JAMLENGTH = 100;
    private InetAddress ownerAddress;
    private final IntentFilter p2pFilter = new IntentFilter();
    public boolean isConnected;
    private boolean goRecord = false;
    private int curProg = 0;
    private float[] mean = new float[100];
    private static final String TAG = "SelfLocalization";
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private boolean iAmTheOwner;
    private boolean chirpDetected;
    private boolean destroy;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;
    private double[] PHAT;
    public Vector<PeerDevice> Devices;
    private List messToPopulate = new ArrayList();
    //private List checkDevices = new ArrayList();
    private int deviceCounter = 0;
    private DevicesSpecsAdapter specsAdapter;
    public boolean firstConnection = true;
    private double[] recordedChirp = new double[maxSamples];
    private double[] originalChirp = new double[maxSamples];
    private double[] recordedIm = new double[maxSamples];
    private double[] originalIm = new double[maxSamples];
    private double[] convRe = new double[maxSamples];
    private double[] convIm = new double[maxSamples];
    private double[] angles = new double[0];
    private double[][] delays;
    public short[] fromMic = new short[Constants.FRAME_SIZE / 2];
    private short[] buf;
    private byte[] pcmToPlay;
    private GraphView graph;
    private FFTHelper fft = new FFTHelper(65536);
    private LocalizeOwnSpk mLocalizeOwnSpk;
    private int SAMPLE_RATE = 44100;
    private static final int MIC_TYPE = MediaRecorder.AudioSource.MIC;
    //private static final int MIC_TYPE = MediaRecorder.AudioSource.CAMCORDER;
    private int curNumSamples = 0;
    private static final int maxSamples = 65536;
    private Dialog specsDialog;
    private int confirmations = 0;
    private Button play,send;
    private static final String path = "cd /Users/enea/Dropbox/work/COCOHA/locAlg/\n";
    private static final String openMATLAB = "matlab -r \"cd /Users/enea/Dropbox/work/COCOHA/locAlg/; ";
    String fileName;
    private InetAddress chirpPlayer;
    private int chirpPlayerIDX;
    private long[] latencyForHist;
    public Vector<StopPoolThreadAdv> openThreads;
    private boolean isCompassAvailable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_localization);
        openThreads = new Vector<>();
        play = (Button)findViewById(R.id.play);
        send = (Button)findViewById(R.id.send);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PlayAudio().start();
            }
        });
        comm = (TextView) findViewById(R.id.comm);
        discovery = (Button) findViewById(R.id.discovery);
        seeList = (Button) findViewById(R.id.seelist);
        send = (Button) findViewById(R.id.send);
        messages = (ListView) findViewById(R.id.messages);
        messAdapter = new Messages(SelfLocalization.this, messToPopulate);
        messages.setAdapter(messAdapter);
        listPeers = (ListView) findViewById(R.id.listView);
        Devices = new Vector<>();
        specsAdapter = new DevicesSpecsAdapter(SelfLocalization.this, Devices, SelfLocalization.this);
        listPeers.setAdapter(specsAdapter);
        editText = (EditText) findViewById(R.id.editText);
        graph = (GraphView) findViewById(R.id.graph);
        graph.setMaxValue(32768 * 2);


        fileName = Environment.getExternalStorageDirectory().getPath()+"/myrecord.pcm";
        try {
            os = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Log.d(TAG,"Launching task matlab");
                    //new SSHTask(angles, delays).execute();
                    try {
                        File sdCardDir = Environment.getExternalStorageDirectory();
                        File targetFile;
                        targetFile = new File(sdCardDir.getCanonicalPath());
                        File file=new File(targetFile + "/"+"angles"+".txt");
                        RandomAccessFile raf = new RandomAccessFile(file, "rw");
                        raf.seek(file.length());

                        File sdCardDi2r = Environment.getExternalStorageDirectory();
                        File targetFile2;
                        targetFile2 = new File(sdCardDi2r.getCanonicalPath());
                        File file2=new File(targetFile2 + "/"+"delays"+".txt");
                        RandomAccessFile raf2 = new RandomAccessFile(file2, "rw");
                        raf2.seek(file2.length());

                        String PHI = "";
                        String T = "";
                        for(int i = 0;i < angles.length;i++){
                            PHI+=String.format("%.8f ",angles[i]);
                            for(int j = 0;j < delays.length;j++){
                                T+=String.format("%.8f ",delays[i][j]);
                            }
                            T+="\n";
                        }


                        raf.write(PHI.getBytes());
                        raf.close();
                        raf2.write(T.getBytes());
                        raf2.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }catch(Exception e){
                    Log.e(TAG,"error in testshell");
                    e.printStackTrace();
                }
            }
        });

        seeList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (iAmTheOwner) {
                    //listDevices();
                    Log.d(TAG, "Devices are " + Devices.size());
                } else {
                    WifiP2pClientSelf sendReq = new WifiP2pClientSelf(SelfLocalization.this, ownerAddress);
                    sendReq.setRequest(Requests.SHOW_DEVICES);
                    sendReq.start();
                }
            }
        });




        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null &&
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // Do Something
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Log.d(TAG, "no magnet sensor or acc sensor!");
            isCompassAvailable = false;
        }

        //set filters
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Menage WifiP2P connectivity
        mWifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mWifiChannel = mWifiManager.initialize(this, getMainLooper(), null);
        deletePersistentGroups();

        discovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!iAmTheOwner)
                    createConnectionDialog();
                else {
                    //progBarWifi.setVisibility(View.VISIBLE);
                    Log.d(TAG,"Not the owner");
                }
            }
        });

        listPeers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //create context menus
                specsDialog = new Dialog(SelfLocalization.this, 0);
                specsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                specsDialog.setContentView(R.layout.context_peer);
                TextView name = (TextView) specsDialog.findViewById(R.id.context_name);
                TextView delay = (TextView) specsDialog.findViewById(R.id.context_delay);
                TextView angle = (TextView) specsDialog.findViewById(R.id.context_angle);
                PeerDevice current = Devices.get(position);
                if (current.isPingAvailable())
                    name.setText(current.getName() + "  " + current.getAddress().toString());
                if (current.isAngleAvailable() == Requests.COMP_RECEIVED) angle.setText(String.format("%.4f",current.getAngle()));
                if (current.isDelayAvailable()) delay.setText(String.format("%.4f",current.getDelay()));
                specsDialog.show();
            }
        });

        createConnectionDialog();

    } //onCreate



    public void sendActReq(InetAddress add){

        chirpPlayer = add;
        chirpPlayerIDX = findInDevicesIDX(add);
        if(delays == null){
            delays = new double[Devices.size()][Devices.size()];
        }

        //mLocalizeOwnSpk = LocalizeOwnSpk.getInstance(MIC_TYPE, SAMPLE_RATE, SelfLocalization.this);
        //mLocalizeOwnSpk.copyGeneratedSignal(originalChirp);
        //pcmToPlay = mLocalizeOwnSpk.getGeneratedPCM();

        //confirmations++;
        //if(Devices.size() == 0) playSnd(pcmToPlay);
        //send out a signal to tell peers to activate microphone and start recording
        for (int i = 0; i < Devices.size(); i++) {
            WifiP2pClientSelf sendReqMic = new WifiP2pClientSelf(SelfLocalization.this, Devices.get(i).getAddress());
            sendReqMic.setRequest(Requests.ACTIVATE_MIC);
            //sendBack.DATA = listDevicesString(Devices.get(i).getAddress()).getBytes(Charset.forName("ISO-8859-1"));
            sendReqMic.start();
        }
    }
    public double[] resizeAndAdd(double[] array,int ind,double val){
        double[] ret = array;
        if(ind > array.length-1)
            ret = new double[ind+1];
        System.arraycopy(array,0,ret,0,array.length);
        ret[ind] = val;
        return ret;
    }

    public void updateComm(final String update){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                comm.setText(update);
            }
        });

    }

    public void dismissProgBar() {
        //progBarWifi.setVisibility(View.INVISIBLE);
    }


    public InetAddress getOwnerAddress() {
        return ownerAddress;
    }

    public void addMessage(final String add, final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messToPopulate.add(new MessageLayout(add, msg));
                messAdapter.notifyDataSetChanged();
            }
        });

    }

    public void listDevices(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listAdd.setText(msg);
            }
        });

    }

    public void listDevices() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String list = "";
                for (int i = 0; i < Devices.size(); i++)
                    list += Devices.elementAt(i).getName() + " - " + Devices.elementAt(i).getAddress().toString();
                //Log.d(TAG, "Showing Devices #" + Devices.size());
                listAdd.setText(list);
            }
        });

    }

    public void fillDevices(String msg) {
        Log.d(TAG,"calling fill devices");
        Devices = new Vector<>();
        String[] names = msg.split("\\?");
        //Log.d(TAG, "filling devices " + names.length);
        for (String i:names) {
            Devices.add(new PeerDevice(null, i));
        }
        //for (int i = 0; i < names.length; i++)
        //    Devices.add(new PeerDevice(null, names[i]));
    }

    public String listDevicesString() {
        String list = "Devices%";
        for (int i = 0; i < Devices.size(); i++)
            list += Devices.get(i).getName() + "?";

        return list;
    }

    public String listDevicesString(InetAddress avoid) {
        String list = "Devices%";
        for (int i = 0; i < Devices.size(); i++)
            if (!avoid.toString().equals(Devices.get(i).getAddress().toString()))
                list += Devices.get(i).getName() + "?";

        return list;
    }

    public void addInfo(InetAddress to, String info) {
        for (int i = 0; i < Devices.size(); i++) {
            if (Devices.get(i).getAddress().toString().equals(to.toString())) {
                Devices.get(i).setInfo(info);
                break;
            }

        }

    }

    Dialog dialog;

    public void sendMessage(final String msg) {
        dialog = new Dialog(SelfLocalization.this, 0);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.sent_to);
        ListView contacts = (ListView) dialog.findViewById(R.id.listViewContacts);
        List peers = new ArrayList();

        for (int i = 0; i < Devices.size(); i++)
            peers.add(Devices.get(i).getName());

        ContactsAdapter adapter = new ContactsAdapter(SelfLocalization.this, peers);
        contacts.setAdapter(adapter);
        dialog.show();
        contacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String to = ((String) parent.getAdapter().getItem(position));
                WifiP2pClientSelf sendReq = new WifiP2pClientSelf(SelfLocalization.this, ownerAddress);
                sendReq.setRequest(Requests.RELAY_MSG);

                sendReq.DATA = ("Sendtoo%" + (to) + "%" + msg).getBytes(Charset.forName("ISO-8859-1"));
                sendReq.start();
                dialog.dismiss();
            }
        });

    }

    public Dialog connectionDialog;
    ListView choosePeer;

    public void createConnectionDialog() {
        connectionDialog = new Dialog(SelfLocalization.this, 0);
        connectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        connectionDialog.setContentView(R.layout.sent_to);
        choosePeer = (ListView) connectionDialog.findViewById(R.id.listViewContacts);

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

    public InetAddress findInDevices(String to) {
        for (int i = 0; i < Devices.size(); i++)
            if (Devices.get(i).getName().equals(to))
                return Devices.get(i).getAddress();
        return null;
    }

    public int findInDevicesIDX(InetAddress to) {
        for (int i = 0; i < Devices.size(); i++)
            if (Devices.get(i).getAddress().toString().equals(to.toString()))
                return i;
        return -1;
    }

    public String findInDevices(InetAddress to) {
        for (int i = 0; i < Devices.size(); i++)
            if (Devices.get(i).getAddress().toString().equals(to.toString()))
                return Devices.get(i).getName();
        return null;
    }

    public boolean addDevice(final InetAddress toAdd) {

        // add only if the address is not there
        if(!checkNewDevice(toAdd)){

            PeerDevice mDevice = new PeerDevice(toAdd, DeviceName.names[deviceCounter++]);
            mDevice.setOwnerAddress(ownerAddress);
            Devices.add(mDevice);

            //Devices.get(Devices.size()-1).setPingAvailable(true);
            //Log.d(TAG, "device added");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // checkDevices.add(new String(toAdd.toString()));
                    specsAdapter.notifyDataSetChanged();
                }
            });

            // at every new i broadcast the list
            for (int i = 1; i < Devices.size(); i++) {
                WifiP2pClientSelf sendBack = new WifiP2pClientSelf(SelfLocalization.this, Devices.get(i).getAddress());
                sendBack.setRequest(Requests.DEVICES_FROM_OWNER);
                sendBack.DATA = listDevicesString(Devices.get(i).getAddress()).getBytes(Charset.forName("ISO-8859-1"));
                sendBack.start();
            }
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // checkDevices.add(new String(toAdd.toString()));
                    specsAdapter.notifyDataSetChanged();
                }
            });
            for(PeerDevice k:Devices)
                Log.d(TAG,k.getAddress().toString() + " is " + k.getName());
        }


        return true;
    }

    private boolean checkNewDevice(InetAddress toAdd) {
        for(PeerDevice K:Devices)
            if(K.getAddress().toString().equals(toAdd.toString()))
                return true;
        return false;
    }

    // triggered if i am the owner
    public boolean createGroup(InetAddress owner, boolean iAm) {

        try {
            if (Devices.size() == 0) {
                iAmTheOwner = iAm;
                //Devices = new Vector<>();
                ownerAddress = owner;
                if (iAm) {
                    addDevice(owner);
                    Log.d(TAG,"owner: "+owner.toString());
                    startAngle();
                    updateComm("Calculating orientation...");
                }
            }else{
                Log.d(TAG,"Basically Devices is not zero");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.d(TAG, "list of devices not created");
            e.printStackTrace();
            return false;
        }
    }

    public void startAngle() {
        if(isCompassAvailable) {
            goRecord = true;
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
        }else{
            WifiP2pClientSelf sendReq = new WifiP2pClientSelf(SelfLocalization.this, ownerAddress);
            sendReq.setRequest(Requests.ANGLE);
            sendReq.DATA = ("Angle%NO").getBytes();
            sendReq.start();
        }
    }

    private void endAngle() {
        double[] res = meanVariance(mean);

        if (res[1] < 2) {
            //send value to server
            updateComm("Orientation done for server");
            WifiP2pClientSelf sendReq = new WifiP2pClientSelf(SelfLocalization.this, ownerAddress);
            Log.d(TAG,"Owner while angle: "+ownerAddress.toString());
            sendReq.setRequest(Requests.ANGLE);
            sendReq.DATA = ("Angle%" + res[0]).getBytes();
            sendReq.start();
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mMagnetometer);
        } else {
            mean = new float[100];
            updateComm("Restarting orientation");
            curProg = 0;
        }
        //see if it's valid or not;

    }

    public byte[] genSweep(double[] buffer, int totalNumSamples, int sampleRate, float minFreq, float maxFreq)
    {
        int biasInSamples = JAMLENGTH;
        int numSamples = totalNumSamples - biasInSamples;
        int biasInBytes = biasInSamples*8;

        byte[] pcmSignal = new byte[numSamples*8];


        double start = 2.0 * Math.PI * minFreq;
        double stop = 2.0 * Math.PI * maxFreq;
        double tmp1 = Math.log(stop / start);

        int s;
        for (s=0 ; s<numSamples ; s++) {
            double t = (double)s / numSamples;
            double tmp2 = Math.exp(t * tmp1) - 1.0;
            buffer[s + biasInSamples] = Math.sin((start * numSamples * tmp2) / (sampleRate * tmp1));
        }

        //adding JAM
        for(int i = 0;i<biasInSamples;i++)
            buffer[i] = 1;

        int idx = 0;
        for (final double dVal : buffer) {
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            pcmSignal[idx++] = (byte) (val & 0x00ff);
            pcmSignal[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
        //activity.plot(buffer);

        try {
        File sdCardDir = Environment.getExternalStorageDirectory();
        File targetFile;
        targetFile = new File(sdCardDir.getCanonicalPath());
        File file=new File(targetFile + "/"+"chirp"+".txt");

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(file.length());
        String out = "";
            for(double i:buffer)
                out+= i+"\n";

        raf.write(out.getBytes());
        raf.close();
    } catch (IOException e) {
        e.printStackTrace();
    }


        return pcmSignal;

    }


    public void playSnd(byte[] pcmSignal){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, pcmSignal.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(pcmSignal, 0, pcmSignal.length);
        audioTrack.play();
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
            float azimuthInDegress = (float) (Math.toDegrees(azimuthInRadians) + 360) % 360;
            RotateAnimation ra = new RotateAnimation(
                    mCurrentDegree,
                    -azimuthInDegress,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            ra.setDuration(250);

            ra.setFillAfter(true);

            mCurrentDegree = -azimuthInDegress;

            if (goRecord) {
                if (curProg == 100) {
                    endAngle();
                } else {
                    comm.setText(String.format("Calculating orientation %2d %%",curProg++));
                    mean[curProg - 1] = 360 - azimuthInDegress;
                }
            }
        }
        //azimuth.setText(""+event.values[0]);
        //pitch.setText(""+event.values[1]);
        //roll.setText(""+event.values[2]);

    }

    public double[] meanVariance(float[] x) {
        double[] meanStd = new double[2];
        int samples = x.length;
        for(double i : x)
            meanStd[0] += i;
        meanStd[0] = meanStd[0] / samples;
        for(double i : x)
            meanStd[1] += Math.pow(i - meanStd[0], 2);

        meanStd[1] = Math.sqrt(meanStd[1] / samples);

        return meanStd;
    }

    public void setWifiPeerListLadapter(List wifiPeerListLadapter) {
        this.mWifiPeerListLadapter = new WiFiPeerListAdapter(SelfLocalization.this, wifiPeerListLadapter);
        //progBar.setVisibility(View.INVISIBLE);
    }

    public void showPeersList(List adapter) {


        la = new WiFiPeerListAdapter(SelfLocalization.this, adapter);
        if(la != null && choosePeer != null) {
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
                                                for (int i = 0; i < Devices.size(); i++)
                                                    if (device.deviceAddress.equals(Devices.get(i).getName().toString()))
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

        receiver = new WifiP2PReceiverSelf(mWifiManager, mWifiChannel, this);
        registerReceiver(receiver, p2pFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mSensorManager.unregisterListener(this, mAccelerometer);
        //mSensorManager.unregisterListener(this, mMagnetometer);
        //unregisterReceiver(receiver);
        /*
        for(StopPoolThread k:openThreads){
            k.destroyMe();
            k = null;
        }
        openThreads = null;
        mWifiManager.removeGroup(mWifiChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"Successfully deleted group!");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG,"Failed in deleting group...");
            }
        });
        */
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

    public void stopRecording() {
        //mReadTh.STOP = true;
        //mReadTh.stop();
        if (mLocalizeOwnSpk != null) {
            mLocalizeOwnSpk.stop();
            mLocalizeOwnSpk.destroy();
        }
        //
       // Toast.makeText(SelfLocalization.this, "Chirp Detected!", Toast.LENGTH_SHORT).show();
        updateComm("Chirp detected...");
        Log.d(TAG, "Starting Analysis");
        startAnalysis();
    }

    private void startAnalysis() {
        // cause is actualy launche by LocalizeOwnSpk

        //plot(recordedChirp);
/*
       try {
            File sdCardDir = Environment.getExternalStorageDirectory();
            File targetFile;
            targetFile = new File(sdCardDir.getCanonicalPath());
            File file=new File(targetFile + "/"+"test"+".txt");

            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            //raf.seek(file.length());
            String out = "";
            for(int i = 0;i<recordedChirp.length;i++)
                out+= recordedChirp[i]+"\n";
            raf.write(out.getBytes());
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/

        // PH
        recordedChirp[32769] = 0;
        fft.fft(recordedChirp, recordedIm);
        fft.fft(originalChirp, originalIm);
        PHAT = new double[maxSamples];
        createPHAT(PHAT, recordedChirp, recordedIm, originalChirp, originalIm);

        for (int i = 0; i < maxSamples; i++) {

            convRe[i] = (recordedChirp[i] * originalChirp[i] + recordedIm[i] * originalIm[i]) * PHAT[i];
            convIm[i] = (recordedIm[i] * originalChirp[i] - recordedChirp[i] * originalIm[i]) * PHAT[i]; // the minus is for complex conjugate

        }

        fft.ifft(convRe, convIm);


        double delay = findMaxLag(convRe, 1.0f / SAMPLE_RATE);
        //Toast.makeText(SelfLocalization.this, "Chirp analyzed d = " + delay, Toast.LENGTH_LONG).show();
        updateComm(String.format("Calculated %.4f", delay));
        //sendstuff
        if (ownerAddress != null) {
            Log.d(TAG,"Sending chirp to owner..."+ownerAddress);
            WifiP2pClientSelf sendReq = new WifiP2pClientSelf(SelfLocalization.this, ownerAddress);
            sendReq.setRequest(Requests.DELAY_CHIRP);
            sendReq.DATA = ("Delay%" + delay).getBytes();
            sendReq.start();
        }else{
            Log.d(TAG,"Connect before sending results!");
            //Toast.makeText(SelfLocalization.this,"Connect before sending results!",Toast.LENGTH_SHORT).show();
        }
        recordedIm = new double[maxSamples];
        recordedChirp = new double[maxSamples];
        originalChirp = new double[maxSamples];
        originalIm = new double[maxSamples];
        convIm = new double[maxSamples];
        convRe = new double[maxSamples];

    }

    private void createPHAT(double[] phat, double[] recR, double[] recI, double[] orR, double[] orI) {
        int samples = recR.length;
        double re,im,tmp;
        for(int i = 0; i < samples; i++){
            re = recR[i] * orR[i] + recI[i] * orI[i];
            im = recI[i] * orR[i] - recR[i] * orI[i];
            tmp = Math.sqrt(Math.pow(re,2) + Math.pow(im,2));
            if(tmp != 0) phat[i] = 1.0f / tmp;
            else phat[i] = 1.0f;
        }
    }

    public void plot(final double[] x) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 3000; i < 3410; i += 1) {
                    graph.addDataPoint((float) x[i] + 32768);
                    //Log.d(TAG,""+convRe[i]);
                }
            }
        });
    }

    public static double findMaxLag(double[] x, double deltaT) {
        final int l = x.length - 10000;

        int index = 0;
        for (int i = 1; i < l; i++)
            if (Math.abs(x[i]) > Math.abs(x[index]))
                index = i;

        return index * deltaT;
    }

    private byte[] act ;
    private FileOutputStream os;

    public void record(int n){
        act = short2byte(fromMic);
        try {
            os.write(act, 0, act.length);
        }catch(Exception e){
            Log.e(TAG,e.toString());
        }

    }

    private byte[] short2byte(short[] sData) {

        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }
        return bytes;

    }

    public void addSamples(final int n) {

                int m = n;
                //if ((isStrong(fromMic) || chirpDetected ) && !destroy) {
                    record(n);
                    //chirpDetected = true;
                    if (curNumSamples + n >= maxSamples)
                        m = maxSamples - curNumSamples;
                    for (int i = 0; i < m; i++)
                        recordedChirp[curNumSamples + i] = fromMic[i];
                    curNumSamples += m;
                    if (curNumSamples == maxSamples) {
                        stopRecording();
                        curNumSamples = 0;
                        //destroy = true;
                    }

               // }

        /*
        if(curNumSamples + n >= maxSamples)
            n = maxSamples - curNumSamples;
        for(int i = 0;i<n;i++)
            recordedChirp[curNumSamples+i] = tempBuf[i];
        curNumSamples += n;
        if(curNumSamples == maxSamples) stopRecording();
        */

    }

    public boolean isStrong(short[] tempBuf) {

        for(short i : tempBuf)
            if (Math.abs(tempBuf[i]) > 32768 / 8)
                return true;
        return false;
    }

    public void addDelay(final String lookFor, final String s) {
        delays[chirpPlayerIDX][findInDevicesIDX(findInDevices(lookFor))] = Double.parseDouble(s);
        Log.d(TAG,"added delays in ["+chirpPlayerIDX+"]"+"["+findInDevicesIDX(findInDevices(lookFor))+"]");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < Devices.size(); i++)
                    if (lookFor.equals(Devices.get(i).getName())) {
                        Devices.get(i).setDelay(Double.parseDouble(s));
                        specsAdapter.notifyDataSetChanged();
                        break;
                    }
            }
        });
    }

    public void addAngle(final InetAddress inDevices, final String s) {
        Log.d(TAG,"Adding angle "+s);
        if(!s.equals("NO")) {
            angles = resizeAndAdd(angles, findInDevicesIDX(inDevices), Double.parseDouble(s));
            Log.d(TAG, "added angle in [" + findInDevicesIDX(inDevices) + "]");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    for (int i = 0; i < Devices.size(); i++)
                        if (inDevices.toString().equals(Devices.get(i).getAddress().toString())) {
                            Devices.get(i).setAngle(Double.parseDouble(s));
                            specsAdapter.notifyDataSetChanged();
                            break;
                        }
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < Devices.size(); i++)
                        if (inDevices.toString().equals(Devices.get(i).getAddress().toString())) {
                            Devices.get(i).setAngleAvailable(Requests.NO_COMP);
                            specsAdapter.notifyDataSetChanged();
                            break;
                        }
                }
            });
        }
    }


    public void addPingDevice(final String inDevices) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < Devices.size(); i++)
                    if (inDevices.equals(Devices.get(i).getName())) {
                        Devices.get(i).setPingAvailable(true);
                        specsAdapter.notifyDataSetChanged();
                        break;
                    }
            }
        });
    }



    public void activateMic() {
        Log.d(TAG,"Activating mic");
        mLocalizeOwnSpk = LocalizeOwnSpk.getInstance(MIC_TYPE, SAMPLE_RATE, SelfLocalization.this);
        mLocalizeOwnSpk.copyGeneratedSignal(originalChirp);
        pcmToPlay = mLocalizeOwnSpk.getGeneratedPCM();
        mLocalizeOwnSpk.start();
    }



    public void checkPlaySnd() {
        if(++confirmations == Devices.size()){
            //mLocalizeOwnSpk.start();
            //got the ack from every devices now i have to tell the correct one to start playing
            //playSnd(pcmToPlay);
            WifiP2pClientSelf playSnd = new WifiP2pClientSelf(SelfLocalization.this, chirpPlayer);
            playSnd.setRequest(Requests.PLAY_CHIRP);
            playSnd.start();
            confirmations = 0;
        }

    }

    public void playChirp() {
        updateComm("Playing chirp...");
        Log.d(TAG,"Playing chirp");
        playSnd(pcmToPlay);
    }

    public class MessageLayout {
        public String message, name;

        public MessageLayout(String message, String name) {
            this.name = name;
            this.message = message;
        }
    }

    // CHECKING
    AudioTrack mAudioTrack;
    int buffersize;
    class PlayAudio extends Thread {

        public void run() {
            buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                    , buffersize, AudioTrack.MODE_STREAM);


            if (mAudioTrack == null) {
                Log.d("TCAudio", "audio track is not initialised ");
                return;
            }

            int count = 512 * 1024; // 512 kb
            //Reading the file..
            byte[] byteData;
            File file;
            file = new File(Environment.getExternalStorageDirectory().getPath()+"/myrecord.pcm");

            byteData = new byte[count];
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            int bytesread = 0, ret ;
            int size = (int) file.length();
            mAudioTrack.play();
            try {
                while (bytesread < size) {
                    ret = in.read(byteData, 0, count);
                    if (ret != -1) { // Write the byte array to the track
                        mAudioTrack.write(byteData, 0, ret);
                        bytesread += ret;
                    } else break;
                }
                in.close();
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    //

    // command SHH
    public class SSHTask extends AsyncTask<Void, Void, String>
    {

        protected double[][] delays;
        protected double[] angles;
        String T="[",PHI="[";
        String command;

        public SSHTask(double[] angles,double[][] delays){
            //I also create the matlab matrix in form of string to send
            this.angles = angles;
            this.delays = delays;
            for(int i = 0;i < angles.length;i++){
                for(int j = i;j < delays.length;j++){
                    delays[j][i] = delays[i][j];
                }
            }
            for(int i = 0;i < angles.length;i++){
                PHI+=String.format("%.8f ",angles[i]);
                for(int j = 0;j < delays.length;j++){
                    T+=String.format("%.8f ",delays[i][j]);
                }
                T+=";";
            }
            PHI+="]";
            T+="]";
            command = openMATLAB + String.format(" locAlg(%s,%s); exit\" \n",T,PHI);
            Log.d(TAG,"create ssh");
        }


        @Override
        protected String doInBackground(Void... arg0) {

            try {

                Log.d(TAG,"starting matlab");
                Log.d(TAG,command);
                JSch jsch = new JSch();
                String host = null;

                final Session session = jsch.getSession("enea", "172.19.12.228", 22);
                session.setPassword("songoldon");

                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(30000);   // making a connection with timeout.

                final Channel channel = session.openChannel("shell");

                OutputStream inputstream_for_the_channel = channel.getOutputStream();
                PrintStream commander = new PrintStream(inputstream_for_the_channel, true);
                channel.connect();
                channel.setOutputStream(System.out);
                commander.println(command);

            } catch (Exception e) {
                System.out.println(e);
                return "error";

            }


            return "done";
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
        }



    }

    private void deletePersistentGroups(){
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(mWifiManager, mWifiChannel, netid, null);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    //end SSH
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Log.d(TAG,"back pressed");
        //mSensorManager.unregisterListener(this, mAccelerometer);
        //mSensorManager.unregisterListener(this, mMagnetometer);
        //unregisterReceiver(receiver);
        for(StopPoolThreadAdv k:openThreads){
            k.destroyMe();
            k = null;
        }
        openThreads = null;
        finish();
    }


}

