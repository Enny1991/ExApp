package com.eneaceolini.exapp;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;
import com.eneaceolini.audio.AudioCapturer;
import com.eneaceolini.audio.AudioNotifier;
import com.eneaceolini.audio.IAudioReceiver;
import com.eneaceolini.fft.FFTHelper;
import com.eneaceolini.netcon.GlobalNotifier;
import com.eneaceolini.netcon.GlobalNotifierUDP;
import com.eneaceolini.netcon.UDPCommunicationManager;
import com.eneaceolini.netcon.UDPRunnableLags;
import com.eneaceolini.netcon.UDPRunnableStream;
import com.eneaceolini.utility.Constants;
import com.eneaceolini.wifip2p.WiFiPeerListAdapter;
import com.eneaceolini.wifip2p.WifiP2PReceiverMain;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import devadvance.circularseekbar.CircularSeekBar;

// For commit in the new desktop

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {


    private static final String TAG = "MainActivity";
    private UDPCommunicationManager mUDPCOmmunicationManager;
    private MediaRecorder mRecorder = null;
    private AudioTrack mAudioTrack;
    private int buffersize;
    protected static int SAMPLE_RATE = 48000;
    private SeekBar seekAngle;
    private int ANGLE;
    private Switch server1,peer1;
    //private Switch server1,server2,peer1,peer2;
    public boolean streamToServer1,streamToPeer1;
    //public boolean streamToServer1,streamToServer2,streamToPeer1,streamToPeer2;
    private int minFreq2Detect = 100; //Hz
    private int minNumberSamples;
    private ActionBar actionBar;
    private double freqCall = 0.5;//ms
    private double REFRESH_RATE = 0.05;//ms
    private int countArrivedSamples = 0;
    private int samplesToPrint = 0;
    private int refreshPower = 0;
    private Vector<Double> lagCollector = new Vector<>();
    private Vector<Double> powerCollector = new Vector<>();
    private Vector<Double> slowPowerCollector = new Vector<>();
    private Vector<Double> angleCollector = new Vector<>();
    private Vector<Double> slowAngleCollector = new Vector<>();
    private final IntentFilter p2pFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2PReceiverMain receiver;
    private ListView listPeers;
    private WiFiPeerListAdapter mWifiPeerListLadapter;
    private ProgressBar progBar;
    public boolean isConnected = false;
    private double TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
    private double REFRESH_SAMPLES = SAMPLE_RATE * REFRESH_RATE;
    private int PORT_SERVER = 6880;
    private int PORT_SERVER_LAGS = 6890;
    private int PORT_DIRECT_LAGS = 6890;
    private final int PORT_DIRECT = 7880;
    //private String STATIC_IP = "172.19.11.239";
    private boolean IS_START = true;
    private String STATIC_IP = "172.19.12.113";
    // private String STATIC_IP = "77.109.166.135";
    private InetAddress directWifiPeerAddress;
    private float KBytesSent = 0.0f;
    private UDPRunnableLags mUDPRunnableLags;
    private long lastRec = 0;
    private int nCollect = 5;
    GlobalNotifierUDP monitorUDPPing = new GlobalNotifierUDP();
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    private int INTERP_RATE = 4;
    // Graphics

    EditText refresh;
    Button setRefresh;
    TextView lag;
    private ImageButton start,stop;
    private TextView kbytes;
    private EditText newIP, newPort,newPortLags;
    private Button goChanges, soloIP, soloPort,soloPortLags, ping;
    private ImageButton playBack;
    private ImageView[] positionButtons = new ImageView[9];
    private boolean isWifiP2pEnabled;
    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); //% volume
    private double LAST_LAG, LAST_SIGN;

    private XYPlot dynamicPlot, dynamicPlotLag;
    private MyPlotUpdater plotUpdater, plotUpdaterLag;
    private CircularSeekBar mCircularSeekBar;

    private RadioGroup radioBeam;
    private int radioBeamSelected = R.id.nobeam;


    private DatagramSocket mSocket,mSocketDirect,mSocketLags;


    double[] FT2 = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)]; //TODO check realloc when changing SR
    double[] tmpSwapBuffer = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)];
    double[] tmpPrint1 = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)];
    double[] tmpPrint2 = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)];

    private int COUNTER_A = 0;
    private int COUNTER_B = 0;
    private int MIC_TYPE;
    Monitor monitor;
    GlobalNotifier mGlobalNotifier = new GlobalNotifier();
    GlobalNotifier backFire = new GlobalNotifier();
    GlobalNotifier doubleBackFire = new GlobalNotifier();
    GlobalNotifierUDP mGlobalNotifierUDPStream= new GlobalNotifierUDP();
    GlobalNotifierUDP mGlobalNotifierUDPLags = new GlobalNotifierUDP();
    DataOutputStream _logAngles, _logPower, _logMicA, _logMicB, _logBF;
    double[] LAGS;
    int indexOfZeroLag;
    double deltaT,roofLags;

    /* Activity Methods */

    @Override
    public void onCreate(Bundle icicle) {

        int minimumNumberSamples = getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE);
        LAGS = new double[minimumNumberSamples];
        indexOfZeroLag = (minimumNumberSamples - 1)/2; // I take for granted the signal has an even number of samples
        deltaT = 1f / SAMPLE_RATE;
        roofLags = Math.ceil(0.14f * SAMPLE_RATE / 343);
        for(int i = 0; i <=indexOfZeroLag; i++)
        {
            LAGS[indexOfZeroLag-i] = - deltaT * i;
            LAGS[indexOfZeroLag+i+1] = deltaT * ( i + 1 );
        }

        verifyStoragePermissions(this);
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        monitor = new Monitor();

        // playback
        // prepare audio playback
        buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        Log.d(TAG,""+buffersize);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                , buffersize, AudioTrack.MODE_STREAM);



        if (mAudioTrack == null) {
            Log.d("TCAudio", "audio track is not initialised ");
            return;
        }

        //mAudioTrack.setVolume(1.f);

        //


        mUDPCOmmunicationManager = UDPCommunicationManager.getInstance();
        //set filters
        // change in the Wi-Fi P2P status.
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // change in the list of available peers.
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // state of Wi-Fi P2P connectivity has changed.
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // this device's details have changed.
        p2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        //

        //menage actionBar
        actionBar = getSupportActionBar();

        /* GRAPHICS set up */

        kbytes = (TextView)findViewById(R.id.kbytes);

        lag = (TextView) findViewById(R.id.showLag);


        progBar = (ProgressBar) findViewById(R.id.progressBar);

        radioBeam = (RadioGroup)findViewById(R.id.radio_beam);
        radioBeam.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                radioBeamSelected = checkedId;
            }
        });

        server1 = (Switch) findViewById(R.id.server1);
        server1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                streamToServer1 = isChecked;
            }
        });
//        server2 = (Switch) findViewById(R.id.server2);
//        server2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                streamToServer2 = isChecked;
//            }
//        });
        peer1 = (Switch) findViewById(R.id.peer1);
        peer1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                streamToPeer1 = isChecked;
            }
        });
//        peer2 = (Switch) findViewById(R.id.peer2);
//        peer2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                streamToPeer2 = isChecked;
//            }
//        });







        playBack = (ImageButton) findViewById(R.id.playback);
        playBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isPlayBack){
                    if(mAudioTrack != null)
                    mAudioTrack.play();
                    //Log.d(TAG,"Supposed to start Audiotrack");
                    else{
                        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                                , buffersize, AudioTrack.MODE_STREAM);
                        mAudioTrack.play();
                    }
                    v.setBackgroundResource(R.mipmap.ic_volume_up_black_48dp);
                }else{
                    v.setBackgroundResource(R.mipmap.ic_volume_off_black_48dp);
                    mAudioTrack.stop();
                    mAudioTrack.flush();
                    mAudioTrack.release();
                    mAudioTrack = null;
                }
                isPlayBack = !isPlayBack;
            }
        });


        mCircularSeekBar = (CircularSeekBar) findViewById(R.id.circularSeekBar1);
        mCircularSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                if(progress >= 0 && progress < 180)
                    ANGLE = progress;
                if(progress >= 180 && progress < 360)
                    ANGLE =  progress - 360;
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });





        start = (ImageButton) findViewById(R.id.start);

        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(IS_START){
                    startRecording();
                    //v.setEnabled(false);
                    _logAngles = openDOS("logAngles");
                    _logAngles = openDOS("logAngles");
                    _logPower = openDOS("logPower");
                    _logMicA = openDOS("logMicA");
                    _logMicB = openDOS("logMicB");
                    _logBF = openDOS("logBF");
                    v.setBackgroundResource(R.mipmap.ic_pause_black_48dp);
                    Log.d(TAG, "Pressed START");
                }else{
                    try {
                        stopRecording();

                        //Graphics
                        server1.setChecked(false);
                        peer1.setChecked(false);
                        v.setBackgroundResource(R.mipmap.ic_play_arrow_black_48dp);

                        // cleanup
                        kbytes.setText("0.0");
                        KBytesSent = 0.0f;
                        samplesToPrint = 0;
                        refreshPower = 0;
                        countArrivedSamples = 0;
                        _logMicA.close();
                        _logMicB.close();
                        _logBF.close();
                        _logAngles.close();
                        _logPower.close();

                    } catch (Exception e) {
                        Log.w(TAG,"Error in stopping: "+e.toString());
                    }
                }
                IS_START = !IS_START;

            }
        });



        /* END of Graphics set up */



        // Menage WifiP2P connectivity
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);


        // PLOT
        // handles
        dynamicPlot = (XYPlot) findViewById(R.id.main_graph);
        plotUpdater = new MyPlotUpdater(dynamicPlot);

        mUDPRunnableStream = new UDPRunnableStream(doubleBackFire,mGlobalNotifierUDPStream,STATIC_IP,directWifiPeerAddress,PORT_SERVER,PORT_DIRECT,MainActivity.this);
        //mUDPRunnableLags = new UDPRunnableLags(mGlobalNotifierUDPLags, STATIC_IP, PORT_SERVER_LAGS, PORT_DIRECT_LAGS,directWifiPeerAddress, MainActivity.this);
        mReadTh = new ReadTh(mGlobalNotifier, mGlobalNotifierUDPLags, mGlobalNotifierUDPStream,mUDPRunnableStream);
        SampleDynamicSeries sine1Series = new SampleDynamicSeries(mReadTh, 0, "Sine 1");

        LineAndPointFormatter formatter1 = new LineAndPointFormatter(
                Color.rgb(238, 37, 37), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);
        dynamicPlot.addSeries(sine1Series,
                formatter1);

        dynamicPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlot.setDomainStepValue(5);

        dynamicPlot.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlot.setRangeStepValue(45);

        dynamicPlot.setRangeValueFormat(new DecimalFormat("###.#"));

        // uncomment this line to freeze the range boundaries:
        dynamicPlot.setRangeBoundaries(-180, 180, BoundaryMode.FIXED);

        for(int i = 0;i < 30 ; i++){
            slowPowerCollector.add(0.);
        }

        //

        dynamicPlotLag = (XYPlot) findViewById(R.id.main_graph_2);
        plotUpdaterLag = new MyPlotUpdater(dynamicPlotLag);

        SampleDynamicSeries2 sine2Series = new SampleDynamicSeries2(mReadTh, 0, "Sine 2");

        LineAndPointFormatter formatter2 = new LineAndPointFormatter(
                Color.rgb(238, 37, 37), null, null, null);
        formatter1.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        formatter1.getLinePaint().setStrokeWidth(10);
        dynamicPlotLag.addSeries(sine2Series,
                formatter2);

        dynamicPlotLag.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlotLag.setDomainStepValue(5);

        dynamicPlotLag.setRangeStepMode(XYStepMode.INCREMENT_BY_VAL);
        dynamicPlotLag.setRangeStepValue(100);

        dynamicPlotLag.setRangeValueFormat(new DecimalFormat("###.#"));

        // uncomment this line to freeze the range boundaries:
        dynamicPlotLag.setRangeBoundaries(0, 1000, BoundaryMode.FIXED);

        for(int i = 0;i < 30 ; i++){
            slowAngleCollector.add(0.);
        }
        for(int i = 0;i < nCollect ; i++){
            lagCollector.add(0.);
        }

        positionButtons[0] = (ImageView)findViewById(R.id.pos0);
        positionButtons[1] = (ImageView)findViewById(R.id.pos1);
        positionButtons[2] = (ImageView)findViewById(R.id.pos2);
        positionButtons[3] = (ImageView)findViewById(R.id.pos3);
        positionButtons[4] = (ImageView)findViewById(R.id.pos4);
        positionButtons[5] = (ImageView)findViewById(R.id.pos5);
        positionButtons[6] = (ImageView)findViewById(R.id.pos6);
        positionButtons[7] = (ImageView)findViewById(R.id.pos7);
        positionButtons[8] = (ImageView)findViewById(R.id.pos8);
        refresh = (EditText) findViewById(R.id.ref_pick);
        setRefresh = (Button) findViewById(R.id.refresh);
        setRefresh.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                freqCall = Double.parseDouble(refresh.getText().toString());
                TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();

    }

    protected void onResume() {
        super.onResume();

        receiver = new WifiP2PReceiverMain(mManager, mChannel, this);
        registerReceiver(receiver, p2pFilter);


    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
        }
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }
    /* END Activity Methods */


    /* Menu Handler methods */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void showPopupMenuSamplingRate() {
        View menuItemView = findViewById(R.id.action_rate);
        PopupMenu popup = new PopupMenu(MainActivity.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.context_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    public void showPopupMenuMinimumFrequency() {
        View menuItemView = findViewById(R.id.action_rate);
        PopupMenu popup = new PopupMenu(MainActivity.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.cut_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case android.R.id.home:
                return true;
            case R.id.action_tcp_set:
                showTCPSettings();
                return true;
            case R.id.action_rate:
                showPopupMenuSamplingRate();
                return true;
            case R.id.action_start_ssh:
                unregisterReceiver(receiver);
                    startActivity(new Intent(MainActivity.this, SSHConnector.class));
                return true;
            case R.id.action_start_loc:
                if(receiver != null)
                    unregisterReceiver(receiver);
                startActivity(new Intent(MainActivity.this, SelfLocalization.class));
                return true;
            case R.id.action_mic:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    MIC_TYPE = MediaRecorder.AudioSource.CAMCORDER;
                } else {
                    item.setChecked(false);
                    MIC_TYPE = MediaRecorder.AudioSource.MIC;
                }
                return true;
            case R.id.action_min_freq:
                showPopupMenuMinimumFrequency();
                return true;
            case R.id.action_p2p:
                lookForPeers();
                //progBar.setVisibility(View.VISIBLE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.rate_8:
                SAMPLE_RATE = 8000;
                TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                tmpPrint1 = new double[minNumberSamples];
                tmpPrint2 = new double[minNumberSamples];
                FT2 = new double[minNumberSamples];
                tmpSwapBuffer = new double[minNumberSamples];
                if(mReadTh != null) {
                    mReadTh.STOP = true;
                    mReadTh = null;
                }
                stopRecording();
                Toast.makeText(MainActivity.this, "Changed to 8 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
            case R.id.rate_16:
                SAMPLE_RATE = 16000;
                TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 16 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                tmpPrint1 = new double[minNumberSamples];
                tmpPrint2 = new double[minNumberSamples];
                FT2 = new double[minNumberSamples];
                LAGS = new double[minNumberSamples];
                indexOfZeroLag = (minNumberSamples - 1)/2; // I take for granted the signal has an even number of samples
                deltaT = 1f / SAMPLE_RATE;
                for(int i = 0; i <=indexOfZeroLag; i++)
                {
                    LAGS[indexOfZeroLag-i] = - deltaT * i;
                    LAGS[indexOfZeroLag+i+1] = deltaT * ( i + 1 );
                }
                tmpSwapBuffer = new double[minNumberSamples];
                if(mReadTh != null) {
                    mReadTh.STOP = true;
                    mReadTh = null;
                }
                    stopRecording();
                return true;
            case R.id.rate_32:
                SAMPLE_RATE = 32000;
                TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                tmpPrint1 = new double[minNumberSamples];
                tmpPrint2 = new double[minNumberSamples];
                FT2 = new double[minNumberSamples];
                tmpSwapBuffer = new double[minNumberSamples];
                if(mReadTh != null) {
                    mReadTh.STOP = true;
                    mReadTh = null;
                }
                    stopRecording();
                Toast.makeText(MainActivity.this, "Changed to 32 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
            case R.id.rate_44:
                SAMPLE_RATE = 44100;
                TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 44.1 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                tmpPrint1 = new double[minNumberSamples];
                tmpPrint2 = new double[minNumberSamples];
                FT2 = new double[minNumberSamples];
                tmpSwapBuffer = new double[minNumberSamples];
                if(mReadTh != null) {
                    mReadTh.STOP = true;
                    mReadTh = null;
                }
                stopRecording();
                //RESTART
                return true;
            case R.id.hz_100:
                minFreq2Detect = 100;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 100 Hz", Toast.LENGTH_SHORT).show();
                tmpPrint1 = new double[minNumberSamples];
                tmpPrint2 = new double[minNumberSamples];
                FT2 = new double[minNumberSamples];
                tmpSwapBuffer = new double[minNumberSamples];
                if(mReadTh != null) {
                    mReadTh.STOP = true;
                    mReadTh = null;
                }
                stopRecording();
                //RESTART
                return true;
            case R.id.hz_1000:
                minFreq2Detect = 1000;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 1 kHz", Toast.LENGTH_SHORT).show();
                tmpPrint1 = new double[minNumberSamples];
                tmpPrint2 = new double[minNumberSamples];
                FT2 = new double[minNumberSamples];
                tmpSwapBuffer = new double[minNumberSamples];
                mReadTh.STOP = true;
                mReadTh = null;
                stopRecording();
                //RESTART
                return true;
        }
        return false;
    }
    /* END Methods for Menu */


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    /* Methods for WifiP2P */

    private boolean sendToDirect = false;
    public static long START;

    public void setDirectWifiPeerAddress(InetAddress address) {
        Log.d(TAG, "Setting address");
        directWifiPeerAddress = address;
        if(mUDPRunnableStream != null) mUDPRunnableStream.IaddressDirect = address;
        //sendToDirect = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peer1.setEnabled(true);
                //peer2.setEnabled(true);
            }
        });

        new UDPServer().start(); //start Thread that listen to packets

    }

    byte[] bytesTMP = new byte[2];
    ByteBuffer bb = ByteBuffer.allocate(1024);

    // Create a non-direct ByteBuffer with a 10 byte capacity
    // The underlying storage is a byte array.


    public void toShort(short[] merged, byte[] bytes) {
        //int n = bytes.length;
        /*
        for(int i = 0;i<n/2;i++){
            bytesTMP[0] = bytes[2*i];
            bytesTMP[1] = bytes[2*i +1];
            merged[i] = getShortFromLittleEndianRange(bytesTMP);
        }
*/
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(merged);


    }

    public static short getShortFromLittleEndianRange(byte[] range){
        return (short)((range[1] << 8) + (range[0] & 0xff));
    }

    FileOutputStream os;
    private boolean isFirstTime = true;

    public class UDPServer extends Thread
    {

        byte[] receiveData = new byte[16];
        byte[] lag = new byte[8];
        short[] signalCS = new short[Constants.FRAME_SIZE/4];
        byte[] pow = new byte[8];
        short[] mergedSignal = new short[Constants.FRAME_SIZE/2];
        short[] toPrint = new short[Constants.FRAME_SIZE/4];
        short[] signalAS = new short[Constants.FRAME_SIZE/4];
        short[] signalBS = new short[Constants.FRAME_SIZE/4];
        byte[] tmp = new byte[2];
        DatagramPacket receivePacket;
        int newPos;

        public long time, back, diff;
        int k = 0;


        public void run()
        {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);


            try{
                DatagramSocket serverSocket = new DatagramSocket(PORT_DIRECT_LAGS);


                Log.d(TAG,"UDP Server Started: Waiting for Packets...");


                while(true)
                {
                    receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);

                    System.arraycopy(receivePacket.getData(), 0, receiveData, 0, receivePacket.getLength());


                    for(int i = 0;i<8;i++){
                        lag[i] = receiveData[i];
                        pow[i] = receiveData[i + 8];
                    }



                    //Log.d(TAG,"Received Lags: "+toDouble(lag));
                    //Log.d(TAG,"Received Pow: "+ toDouble(pow));

                    // Calculating position based on
                    newPos = assign(LAST_SIGN, LAST_LAG, toDouble(pow));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0; i< 9;i++)
                                positionButtons[i].setBackgroundColor(Color.GRAY);
                            positionButtons[newPos].setBackgroundColor(Color.RED);
                            //lag.setText(String.format("Mean Theta " + "%.2f" + " degrees" , Math.signum(val) * theta));
                        }
                    });

                    //Log.d("LATENCY", "" + time + " us");


//                    try {
//
//                        RAF2.write((time+"\n").getBytes());
//                        //RAF.close();
//                        Log.d(TAG,"" + time);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
////
//                    try{
//                        receiveData = receivePacket.getData();
//                    }catch(Exception e){
//                        System.out.println(e.toString());
//                    }
//
//                    try {
//                        //back = bytesToLong(receiveData);
//                        //diff = back - START;
//                        START = System.currentTimeMillis();
//                        if (toPingRunnable == null){
//                            toPingRunnable = new UDPRunnable(directWifiPeerAddress, PORT_DIRECT, new byte[Constants.FRAME_SIZE]);
//                            toPingRunnable.start();
//                            Log.d(TAG,"starting ping runnable");
//                        }
//                        monitorUDPPing.doNotify();
////                        Log.d(TAG,"" + diff);
////                        RAF.write((diff+"\n").getBytes());
//                        //RAF.close();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }

                    //just fo the Drift



                    //

//                    k = 0;
//                    mergedSignal = new short[receiveData.length/N];

//                    for(int i = 0;i<Constants.FRAME_SIZE/2;i+=2){
//                        signalA[i] = receiveData[2*i];
//                        signalA[i + 1] = receiveData[2*i + 1];
//                        signalB[i] = receiveData[2*i + 2];
//                        signalB[i + 1] = receiveData[2*i + 3];
//                    }
//                    toShort(signalAS,signalA);
//                    toShort(signalBS,signalB);
//                    for(int i = 0; i < Constants.FRAME_SIZE/4; i++) {
//                        if (i + ANGLE < Constants.FRAME_SIZE / 4)
//                            signalCS[i] = (short) (signalAS[i] + signalBS[i + ANGLE]);
//                        else
//                            signalCS[i] = signalAS[i];
//                    }
//                    try {
//                        os.write(signalA, 0, signalA.length);
//                    } catch (Exception e) {
//                        Log.w("os", e.toString());
//                    }

//                    if(isPlayBack ){
//                        mAudioTrack.write(signalA, 0, Constants.FRAME_SIZE / 2);
//                        Log.d(TAG,"writing on audio track!");
//                    }
//                     toShort(mergedSignal,receiveData);
                    //lastRec = System.currentTimeMillis();
//                    if(radioButtonSelected == R.id.latency) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                    mGraphView3.setMaxValue(6);
//                                    mGraphView3.addDataPoint((float) time + 3);
//                                    //Log.d("lag",""+time);
//
//                            }
//                        });
//                    }


//                    for(int i = 0;i<receiveData.length/2;i++){
//                        for(int j = 0;j<2;j++) tmp[j] = receiveData[i*2 + j];
//                        mergedSignal[i] = toShort(tmp);
//                    }


//
//                    for(int i = 0;i<Constants.FRAME_SIZE/4;i++){
//                        signalA[i] = mergedSignal[2*i];
//                        signalB[i] = mergedSignal[2*i + 1];
//                    }
                    //System.arraycopy(mergedSignal, 0, signalA, 0, mergedSignal.length/2);
                    //System.arraycopy(mergedSignal, signalA.length, signalB, 0, mergedSignal.length/2);




//////
//////
//                    if(null != toPrint) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                int l = toPrint.length;
//                                mGraphView3.setMaxValue(MAXFT);
//                                for (int i = 0; i < l; i += (MAXCOLLECT + 5 + SAMPLE_RATE / 500 )) {
//                                    mGraphView3.addDataPoint((float) toPrint[i] / 10 + MAXFT / 2);
//                                }
//
//                            }
//                        });
//                    }

                    //signalB = extractSignal(mergedSignal,false);
                    //System.out.println(""+mergedSignal[0]);
                    /*
                    try{
                        for(int i=0;i<mergedSignal.length/2;i++)
                        {
                            bufWriter.append(""+signalA[i]+"\n");
                            bufWriter2.append(""+signalB[i]+"\n");
                        }
                    }catch(Exception e){
                        System.out.println(e.toString());
                    }
                    */


                    //bufWriter.

                    //String sentence = new String( receivePacket.getData());
                    //System.out.println("RECEIVED: " + sentence);
                    //InetAddress IPAddress = receivePacket.getAddress();
                    //int port = receivePacket.getPort();
                    //String capitalizedSentence = sentence.toUpperCase();
                    //sendData = capitalizedSentence.getBytes();
                    //DatagramPacket sendPacket =
                    //	new DatagramPacket(sendData, sendData.length, IPAddress, port);
                    //serverSocket.send(sendPacket);
                }

		/*
			int serverPort = 6880;
			ServerSocket listenSocket = new ServerSocket(serverPort);


			while(true) {

				Socket clientSocket = listenSocket.accept();
				Connection c = new Connection(clientSocket,COUNT++,bufWriter);

			}
			*/

            }
            catch(IOException e) {
                Log.e(TAG,"Listen :"+e.getMessage());
            }

        }



    }


    public void resetAdapterPeersList() {
        listPeers.setAdapter(null);
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public WiFiPeerListAdapter getWifiPeerListLadapter(){
        return mWifiPeerListLadapter;
    }


    public void setWifiPeerListLadapter(List wifiPeerListLadapter) {
        this.mWifiPeerListLadapter = new WiFiPeerListAdapter(MainActivity.this, wifiPeerListLadapter);
        //progBar.setVisibility(View.INVISIBLE);
    }

    public class TimeOutThread extends Thread {
        public void run() {
            for (int i = 1; i < 16; i++) {
                try {

                    Thread.sleep(1000);
                    progBar.setProgress(10 * i);

                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }
    }


    private void lookForPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Start discovery Wifi P2P");
                new Thread(new Runnable(){
                    public void run(){
                        for (int i = 1; i < 160; i++) {
                            try {
                                final int count = i;
                                Thread.sleep(100);

                                progBar.post(new Runnable(){
                                    public void run(){
                                        progBar.setProgress(count);
                                    }
                                });

                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    }
                }).start();
            }

            @Override
            public void onFailure(int reasonCode) {

            }


        });
    }


    /* Methods for Audio Analysis */


    private static byte[] short2byte(short[] sData) {

        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }
        return bytes;

    }

    IAudioReceiver mAudioReceiver;
    AudioCapturer mAudioCapturer;
    ReadTh mReadTh;
    UDPRunnableStream mUDPRunnableStream;
    String fileName;

    public void startRecording() {
        //TODO place the cumulative signals in read thread
        Log.d(TAG,"" + directWifiPeerAddress);
        if(mUDPRunnableStream == null){
            mUDPRunnableStream = new UDPRunnableStream(doubleBackFire,mGlobalNotifierUDPStream,STATIC_IP,directWifiPeerAddress,PORT_SERVER,PORT_DIRECT,MainActivity.this);
            mUDPRunnableStream.start();
            Log.d(TAG,"strarting UDP");
        }else{
            mUDPRunnableStream.start();
            Log.d(TAG,"strarting UDP");
        }
        if(mUDPRunnableLags == null) {
            mUDPRunnableLags = new UDPRunnableLags(mGlobalNotifierUDPLags, STATIC_IP, PORT_SERVER_LAGS, PORT_DIRECT_LAGS,directWifiPeerAddress, MainActivity.this);
            mUDPRunnableLags.start();
        }else{
            mUDPRunnableLags.start();
        }

        // the lag has to be separated from the stream but direct of not the stream can be sent from the same thread
        monitor = new Monitor();
        fileName = Environment.getExternalStorageDirectory().getPath()+"/myrecord.pcm";
        if(mReadTh == null) {
            mReadTh = new ReadTh(mGlobalNotifier, mGlobalNotifierUDPLags, mGlobalNotifierUDPStream,mUDPRunnableStream);
            mReadTh.addObserver(plotUpdater);
            mReadTh.addObserver2(plotUpdaterLag);
            mReadTh.start();
        }else{
            mReadTh.addObserver(plotUpdater);
            mReadTh.addObserver2(plotUpdaterLag);
            mReadTh.start();
        }

        //mAudioAnalyzer = AudioAnalyzer.getInstance();
        mAudioReceiver = new IAudioReceiver(MainActivity.this, fileName);
        mAudioCapturer = AudioCapturer.getInstance(mAudioReceiver, MIC_TYPE,SAMPLE_RATE,mGlobalNotifier,mReadTh,backFire);
        mAudioCapturer.start();
    }

    public void stopRecording() {
        //mReadTh.STOP = true;
        mReadTh = null;
        if(mAudioCapturer != null) {
            mAudioCapturer.stop();
            mAudioCapturer.destroy();
        }
        if(mAudioTrack != null && isPlayBack){
            playBack.callOnClick();
        }
        if(mUDPRunnableLags != null){
            mUDPRunnableLags = null;
        }
        if(mUDPRunnableStream != null){
            mUDPRunnableStream = null;
        }

        // Writing delays on fil

    }

    public int ACTUAL_COUNTER=0;

    public void updateGraphs(final float update) {


KBytesSent+=update;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    kbytes.setText(String.format("%.4f", KBytesSent));
            }
        });


    }



    public static class Monitor {

    }




    class PlayAudio extends Thread {

        public void run() {
            buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
                    ,buffersize, AudioTrack.MODE_STREAM);


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

            int bytesread = 0, ret;
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


    private byte[] copiedBuffer = new byte[Constants.FRAME_SIZE / 2];

    private AudioNotifier mAudioNotifier = new AudioNotifier();
    private boolean isPlayBack = false;

    class PlayAudioOnline extends Thread {

        byte[] buf;
        public void run() {
            buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT);
            Log.d(TAG,""+buffersize);

            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
                    , minNumberSamples, AudioTrack.MODE_STREAM);


            if (mAudioTrack == null) {
                Log.d("TCAudio", "audio track is not initialised ");
                return;
            }


            mAudioTrack.play();

            try {
                while (isPlayBack) {
                    mAudioNotifier.doWait();
                    Log.d(TAG, "Writing... " + mAudioNotifier.packet.length);
                    mAudioTrack.write(mAudioNotifier.packet, 0, mAudioNotifier.packet.length);
                    mAudioNotifier.doNotify();
                    Thread.sleep(20);
                }

                mAudioNotifier.doNotify();
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    public class ReadTh extends Thread {// This thread uses a lot of memory but in theory it allocate only in the beginning
        boolean STOP = false;
        private final String TAG_RD = "Analysis";
        int minimumNumberSamples = getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE);
        FFTHelper fft = new FFTHelper(minimumNumberSamples);
        UDPRunnableStream mUDPStream;
        //This calls will ask for 4096 * 6 byte which is 24.5MB!!
        //Maybe it will be precise enough with float instead of float
        //TODO change to float will give an allocation of 12.3 MB
        public short[] globalSignal = new short[Constants.FRAME_SIZE / 2];
        public double[] playbackSignalA = new double[Constants.FRAME_SIZE / 4];
        public double[] playbackSignalD = new double[Constants.FRAME_SIZE / 4];
        public short[] playbackSignalC = new short[Constants.FRAME_SIZE / 4];
        public double[] playbackSignalB = new double[Constants.FRAME_SIZE / 4];
        double[] cumulativeSignalA = new double[minimumNumberSamples];
        double[] cumulativeSignalB = new double[minimumNumberSamples];
        double[] cumulativeSignalC = new double[minimumNumberSamples];
        double[] emptySignalA = new double[minimumNumberSamples*INTERP_RATE];
        double[] emptySignalB = new double[minimumNumberSamples*INTERP_RATE];
        double[] emptySignalC = new double[minimumNumberSamples*INTERP_RATE];
        double[] cumulativeSignalD = new double[minimumNumberSamples];
        double[] convolution = new double[minimumNumberSamples];
        Number [] tograph;
        GlobalNotifier monitor;
        GlobalNotifierUDP monitorUDPLags;
        GlobalNotifierUDP monitorUDPStream;
        FileOutputStream os;
        double lagg, theta, val, theta2;
        byte[] toSend;
        boolean first = true;
        int oldPos = 4, newPos;
        int n;
        private MyObservable notifier;
        private MyObservable2 notifier2;
        int SAMPLE_SIZE=30;
        double lastPower, localPower;
        public static final int POWER = 0;

        public void addObserver(Observer observer) {
            notifier.addObserver(observer);
        }
        public void addObserver2(Observer observer) {
            notifier2.addObserver(observer);
        }

        public void removeObserver(Observer observer) {
            notifier.deleteObserver(observer);
        }

        class MyObservable2 extends Observable {
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        }

        {
            notifier2 = new MyObservable2();
        }


        public int getItemCountLag(int series) {
            return SAMPLE_SIZE;
        }

        public Number getXLag(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            return index;
        }

        public Number getYLag(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            double power = slowPowerCollector.get(index);
            switch (series) {
                case POWER:
                    return power;
                default:
                    throw new IllegalArgumentException();
            }
        }

        class MyObservable extends Observable {
            @Override
            public void notifyObservers() {
                setChanged();
                super.notifyObservers();
            }
        }

        {
            notifier = new MyObservable();
        }

        public int getItemCount(int series) {
            return SAMPLE_SIZE;
        }

        public Number getX(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            return index;
        }

        public Number getY(int series, int index) {
            if (index >= SAMPLE_SIZE) {
                throw new IllegalArgumentException();
            }
            double power = slowAngleCollector.get(index);
            switch (series) {
                case POWER:
                    return power;

                default:
                    throw new IllegalArgumentException();
            }
        }



        public ReadTh(GlobalNotifier monitor, GlobalNotifierUDP monitorUDPLags,GlobalNotifierUDP monitorUDPStream,UDPRunnableStream str){

            mUDPStream = str;
            this.monitor = monitor;
            this.monitorUDPLags = monitorUDPLags;
            this.monitorUDPStream = monitorUDPStream;
//            try {
//                os = new FileOutputStream(fileName);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }

        }


        @Override
        public void run() {
            Log.d(TAG,"Started FFT with "+minimumNumberSamples);
            countArrivedSamples = 0;
            while (!STOP) {
                try {

                    monitor.doWait();
                    n = monitor.length;
                    monitorUDPStream.length = n;

                    if(!first){
                        doubleBackFire.doWait();
                        System.arraycopy(globalSignal, 0, monitorUDPStream.packet, 0, n);
                        monitorUDPStream.doNotify();
                    }
                    else{
                        System.arraycopy(globalSignal, 0, monitorUDPStream.packet, 0, n);
                        monitorUDPStream.doNotify();
                        first = false;
                    }




                    //The signal I am sending is not demixed but I can send it this way and demix at the server side
                    //thing that I have to do if I am receiving that from the Direct;
                    //TODO get rid of the globalSignal will free 4096 byte of allocation for it
                    //I have to check:
                        // 1 - if  I have enough samples for the analysis
                        // 2 - if the new sample with explode my buffer but if  ihave enough should be ok
                    // I split the two and put it in cumulative if I can fit
                    // Since the size of the cumulative is exactly the samples we need to do the analysis I just fill it and do it


                    for (int i = 0,j = 0; i < Constants.FRAME_SIZE/2; i += 2,j++) {
                        playbackSignalA[j] = globalSignal[i];
                        playbackSignalB[j] = globalSignal[i+1];
                    }
                    backFire.doNotify();

                    //writeOnDOS(_logMicA, globalSignal);


                    switch(radioBeamSelected){
                        case R.id.dandsum:
                            fft.beam_fftw(playbackSignalA, playbackSignalB, playbackSignalD, (double) ANGLE, true);
                            for(int i = 0; i < playbackSignalC.length;i++){
                                playbackSignalC[i] = (short) playbackSignalD[i];
                                localPower += Math.pow(playbackSignalD[i],2);
                            }
                            break;
                        case R.id.dandsub:
                            fft.beam_fftw(playbackSignalA, playbackSignalB, playbackSignalD, (double) ANGLE, false);
                            for(int i = 0; i < playbackSignalC.length;i++){
                                playbackSignalC[i] = (short) playbackSignalD[i];
                                localPower += Math.pow(playbackSignalD[i],2);
                            }
                            break;
                        case R.id.nobeam:
                            //fft.corr_fftw(cumulativeSignalA, cumulativeSignalB, cumulativeSignalC);
                            for(int i = 0; i < playbackSignalC.length;i++){
                                playbackSignalC[i] = (short) playbackSignalA[i];
                                localPower += Math.pow(playbackSignalA[i],2);
                            }
                            break;
                    }


                    powerCollector.add(localPower);

                    if(isPlayBack){
                        mAudioTrack.write(playbackSignalC,0,playbackSignalC.length);
                    }


                    if(countArrivedSamples + n/2 < minimumNumberSamples){
                        for (int i = 0, j = 0; i < n - 1; i += 2,j++) {
                            cumulativeSignalA[countArrivedSamples] = playbackSignalA[j];
                            cumulativeSignalB[countArrivedSamples++] = playbackSignalB[j];
                        }
                        //backFire.doNotify();
                    }else { // I start the analysis
                        for (int i = 0,j = 0; i < n - 1; i += 2,j++) {
                            cumulativeSignalA[countArrivedSamples] =  playbackSignalA[j];
                            cumulativeSignalB[countArrivedSamples++] = playbackSignalB[j];
                        }
                        //backFire.doNotify();
                        samplesToPrint += minimumNumberSamples;
                        refreshPower += minimumNumberSamples;
                        countArrivedSamples = 0;
                        localPower = 0;


                //CubicInterpolation1d cubicInterpolation1d = new CubicInterpolation1d();
                //emptySignalA = cubicInterpolation1d.interpolate(cumulativeSignalA, INTERP_RATE);
                //emptySignalB = cubicInterpolation1d.interpolate(cumulativeSignalB, INTERP_RATE);


                //for (int j = 0; j < emptySignalA.length; j++) {
                //    emptySignalA[j] = emptySignalA[j] / INTERP_RATE;
                //    emptySignalB[j] = emptySignalB[j] / INTERP_RATE;
                //}


                        fft.corr_fftw(cumulativeSignalA, cumulativeSignalB, cumulativeSignalC);





                            //final double[] FT2 = x_interp;

                        //writeOnRAF(logMicA, cumulativeSignalA);
                        //writeOnRAF(logMicB, cumulativeSignalB);
                        //writeOnRAF(logBF,cumulativeSignalD);

                        //writeOnDOS(_logMicA, cumulativeSignalA);
                        //writeOnDOS(_logMicB, globalSignal);
                        //writeOnDOS(_logBF,playbackSignalC);

                            //does lag collector allocate memory every time?


                        lagCollector.add(calculateAngle(findMaxLag(cumulativeSignalC) * deltaT));
                        lagCollector.removeElementAt(0);
                        theta = meanLag(lagCollector); // in seconds
                        LAST_LAG = theta;
                        toSend = toByteArray(theta);
                        System.arraycopy(toSend, 0, monitorUDPLags.packetByte,0, 8); // I free the monitor.packet
                        toSend = toByteArray(LAST_SIGN);
                        System.arraycopy(toSend, 0, monitorUDPLags.packetByte,8, 8); // I free the monitor.packet
                        monitorUDPLags.doNotify();
                        // lagCollector.removeAllElements();
                        // samplesToPrint = 0;
                        // slowAngleCollector.add(theta);
                        // slowAngleCollector.removeElementAt(0);


//
                        if(refreshPower >= REFRESH_SAMPLES){
                            slowPowerCollector.add(meanPower(powerCollector) / 100);
                            slowPowerCollector.removeElementAt(0);
                            //notifier2.notifyObservers();
                            powerCollector.removeAllElements();
                            refreshPower = 0;
                            //logPower.write((slowPowerCollector.get(29)+"\n").getBytes());
                            //writeOnRAF(logPower, slowPowerCollector.get(slowPowerCollector.size() - 1));
                            //writeOnDOS(_logPower, slowPowerCollector.get(slowPowerCollector.size() - 1));

                        }

                        if (samplesToPrint >= TOTAL_SAMPLES) {
                            Log.d(TAG, "Working with" + lagCollector.size() + "samples");
                            //toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 50); //duration

                                //writeOnRAF(logAngles, slowAngleCollector.get(slowAngleCollector.size() - 1));
                                //writeOnDOS(_logAngles, slowAngleCollector.get(slowAngleCollector.size() - 1));
                                //notifier.notifyObservers();

                        }

                        //Clean
                        for(int i = 0;i<minimumNumberSamples;i++){
                            cumulativeSignalA[i] = 0;
                            cumulativeSignalB[i] = 0;
                            convolution[i] = 0;
                        }
                    }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
            }
        }
    }

    public double calculateAngle(double lag){
        double val = lag * 343f / 0.14f;
        LAST_SIGN = Math.signum(val);
        return Math.toDegrees( Math.asin(Math.signum(val) * Math.min(1.0, Math.abs(val))));
    }


    public static byte[] toByteArray(int value)
    {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).putInt(value);
        return bytes;
    }

    public static byte[] toByteArray(double value)
    {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value).order(ByteOrder.BIG_ENDIAN);
        return bytes;
    }


        public double mode(final Vector<Integer> n) {
            int maxKey = 0;
            int maxCounts = 0;

            int[] counts = new int[n.size()];
            Log.d(TAG,"" + n.size());
            for (int i = 0; i < n.size(); i++) {
                counts[n.get(i)]++;
                if (maxCounts < counts[n.get(i)]) {
                    maxCounts = counts[n.get(i)];
                    maxKey = n.get(i);
                }
            }
            return maxKey * deltaT;
        }


    public int findMaxLag(double[] x)
    {

        final int l = x.length;
        int index=0;
        int tap;
        for(int i = 1;i<l;i++)
            if(x[i]>x[index])
                index = i;
        if (index > l/2){
            tap = index - l;
        }else{
            tap = index;
        }

        if(Math.abs(tap) >= roofLags) tap =  (int) ( Math.signum(tap) * roofLags);
        //if(Math.abs(tap) >= roofLags) tap =  (int) ( roofLags);

//        if(Math.abs(indexOfZeroLag - index) >= 6){
//            if(indexOfZeroLag > index) index = indexOfZeroLag - 5;
//            if(indexOfZeroLag < index) index = indexOfZeroLag + 5;
//        }
//        Log.d(TAG,"" + index + " " + LAGS[index]);
//        Log.d(TAG,"" + tap);
        return tap;
    }

    public double meanLag(Vector<Double> x)
    {

        final int s = x.size();
        double mean=0;
        double weightsSum = 0;
        for(int i = 0;i < s; i++) {
            mean += x.get(i) * (s - i);
            if (Math.abs(x.get(i)) > 5.)
                weightsSum += s -i ;
        }
        if( weightsSum != 0)
            return (float) mean / weightsSum;
        else return 0.;
    }

    public double meanPower(Vector<Double> x)
    {
        final int s = x.size();
        double mean=0;
        for(int i = 0;i < s; i++)
            mean+=x.get(i);
        return mean / s * deltaT;
    }


    public static boolean isNumeric(String str)
    {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }




    /**
     * Change the minimum number of samples to capture the shift of waves of minFreq
     *
     * @param minFreq minimun detectable frequency
     * @param samplingRate actual sampling rate of the microphones
     * @return the minimun number of samples to get an entire period of the minFreq
     */
    public static int getMinNumberOfSamples(double minFreq,int samplingRate)
    {
        int n2;
        int nn = (int) Math.ceil(2 * ( 1f / minFreq ) * samplingRate);
        if ((nn & (nn - 1)) == 0) {
            if(nn < Constants.FRAME_SIZE/2) return Constants.FRAME_SIZE/2;
            else return nn;
        } else {
            n2 = nn;
            n2--;
            n2 |= n2 >> 1;   // Divide by 2^k for consecutive doublings of k up to 32,
            n2 |= n2 >> 2;   // and then or the results.
            n2 |= n2 >> 4;
            n2 |= n2 >> 8;
            n2 |= n2 >> 16;
            n2++;
//            if(n2 < Constants.FRAME_SIZE/2) return Constants.FRAME_SIZE/2;
//            else return n2;
            return Constants.MIN_NUM_SAMPLES;
        }
        //I'm sure that it is power of 2
    }



    //UDP Communication Handling
    //It's always better to use Thread in UDP Comm
    // The signal can be reconstructed at the server side

boolean FFF = true;

    public class UDPRunnable implements Runnable
    {

        private InetAddress Iaddress;
        private int port;
        private byte[] data;
        Thread thread;
        float start,stop;

        UDPRunnable(InetAddress add,int port,byte[] msg){
            try
            {
                Iaddress = add;
            }
            catch(Exception e)
            {
                Log.w(TAG,"Packet not sent, Invalid Address");
            }
            this.port = port;
            data = msg;
        }

        public void start(){
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run(){
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {

                if (mSocket == null) {
                    Log.d(TAG,"Socket is null");
                    mSocket = new DatagramSocket(null);
                    mSocket.setReuseAddress(true);
                    mSocket = new DatagramSocket();
                    mSocket.connect(Iaddress, port);
                    mSocket.setBroadcast(true);
                }



                while(FFF) {
                    //Log.d(TAG,"waiting...");
                    monitorUDPPing.doWait();
                    DatagramPacket sendPacket = new DatagramPacket(monitorUDPPing.packetByte, monitorUDPPing.packetByte.length, Iaddress, port);
                    mSocket.send(sendPacket);
                    //Update total data sent
                    //KBytesSent += (1.0 * data.length) / 1000;
                    //connectionLost = false;
                }
                //Update total data sent
                //KBytesSent += (1.0 * data.length)/1000;
                //connectionLost = false;
                Log.d(TAG, "exiting UDP");
            }catch(Exception e) {
                Log.e("UDP",e.toString());
            }

        }

    }






    public class UDPRunnableDirect implements Runnable
    {

        private InetAddress Iaddress;
        private int port;
        private byte[] data;
        Thread thread;
        //float start,stop;



        UDPRunnableDirect(InetAddress add,int port,byte[] msg){
            Iaddress = add;
            this.port = port;
            data = msg;
        }

        public void start(){
            thread = new Thread(this);
            thread.start();
        }

        @Override
        public void run(){
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {

                if (mSocketDirect == null) {
                    Log.d(TAG,"Socket is null");
                    mSocketDirect = new DatagramSocket(null);
                    mSocketDirect.setReuseAddress(true);
                    mSocketDirect = new DatagramSocket();
                    mSocketDirect.connect(Iaddress, port);
                    mSocketDirect.setBroadcast(true);
                }



                DatagramPacket sendPacket = new DatagramPacket(data, data.length, Iaddress, port);
                //Log.d(TAG,"PACKET: "+tmp.length+" bytes");
                mSocketDirect.send(sendPacket);
                //Update total data sent
                //KBytesSent += (1.0*data.length)/1000;

                //Message msg = hdl.obtainMessage(UPDATE_KBYTES_COUNT);
                //hdl.sendMessage(msg);
                //Log.d(TAG, "ts:" + data.length);
                //stop =System.currentTimeMillis() - start;
                //Log.d("UDP Sending","Time to ex "+stop+ " ms");

            }catch(Exception e) {
                //Log.e("UDP",e.toString());

            }
            }
        }






    /* TCP Communication Handling
    Thread or AsyncTask
    For faster comunication is better to use Thread cause Async Tasks have a limited queue */

    public class TCPThread extends Thread
    {

        String dstAddress;
        int dstPort;
        String response = "";
        byte[] data;

        TCPThread(String addr, int port,byte[] data){
            //Log.d(TAG,"Creating Task #"+COUNTER_A++);
            dstAddress = addr;
            dstPort = port;
            this.data = data;
        }

        @Override
        public void run() {

            Socket socket = null;

            try {
                //Log.d(TAG,"Action Sending #"+COUNTER_B++);
                socket = new Socket(dstAddress, dstPort);
                DataInputStream input = new DataInputStream( socket.getInputStream());
                DataOutputStream output = new DataOutputStream( socket.getOutputStream());


                //Step 1 send length
                //Log.d("Length", "" + data.length());
                long latency = System.currentTimeMillis();
                output.writeInt(data.length);
                //Step 2 send length
                //Log.d("act", "Writing.......");
                output.write(data);

                output.flush();

                //Step 1 read length
                /*
                int nb = input.readInt();
                byte[] digit = new byte[nb];
                //Step 2 read byte
                for (int i = 0; i < nb; i++)
                    digit[i] = input.readByte();
                final long lat = System.currentTimeMillis() - latency;
                String st = new String(digit);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLatency.setText("Latency: " + lat + " ms");
                    }
                });

                //Log.d("Latency Network: ", "" + latency/2+" ms");
*/

            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }



    }


    public class TCPTask extends AsyncTask<Void, Void, Void>
    {

        String dstAddress;
        int dstPort;
        String response = "";
        byte[] data;

        TCPTask(String addr, int port,byte[] data){
            Log.d(TAG, "Creating Task #"+COUNTER_A++);
            dstAddress = addr;
            dstPort = port;
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;

            try {
                Log.d(TAG,"Sending #"+COUNTER_B++);
                socket = new Socket(dstAddress, dstPort);
                DataInputStream input = new DataInputStream( socket.getInputStream());
                DataOutputStream output = new DataOutputStream( socket.getOutputStream());


                //Step 1 send length
                //Log.d("Length", "" + data.length());
                long latency = System.currentTimeMillis();
                output.writeInt(data.length);
                //Step 2 send length
                //Log.d("act", "Writing.......");
                output.write(data);

                output.flush();

                //Step 1 read length
                /*
                int nb = input.readInt();
                byte[] digit = new byte[nb];
                //Step 2 read byte
                for (int i = 0; i < nb; i++)
                    digit[i] = input.readByte();
                final long lat = System.currentTimeMillis() - latency;
                String st = new String(digit);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLatency.setText("Latency: " + lat + " ms");
                    }
                });
                */

                //Log.d("Latency Network: ", "" + latency/2+" ms");


            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }finally{
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            //textResponse.setText(response);
            super.onPostExecute(result);
        }



}

    Dialog dialog;
    WiFiPeerListAdapter la;

    public void showPeersList(List adapter)
    {

            listPeers = (ListView)findViewById(R.id.listView);
            la = new WiFiPeerListAdapter(MainActivity.this, adapter);
            listPeers.setAdapter(la);



            //if(mWifiPeerListLadapter != null)
            //listPeers.setAdapter(mWifiPeerListLadapter);
            listPeers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    WifiP2pDevice device = (WifiP2pDevice) mWifiPeerListLadapter.getItem(position);

                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = device.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;

                    if (isConnected) {

                        if (mManager != null && mChannel != null) {


                            mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                                @Override
                                public void onGroupInfoAvailable(WifiP2pGroup group) {
                                    if (group != null && mManager != null && mChannel != null
                                            ) {

                                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {


                                            @Override
                                            public void onSuccess() {
                                                listPeers.setAdapter(null);
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

                        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {

                                //unregisterReceiver(receiver);
                                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                            }

                            @Override
                            public void onFailure(int reason) {
                                Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }
            });


    }

    public void showTCPSettings()
    {
        dialog = new Dialog(MainActivity.this, 0);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.change_port_ip);
        newIP = (EditText)dialog.findViewById(R.id.newip);
        newIP.setHint(STATIC_IP);
        newPort = (EditText)dialog.findViewById(R.id.newport);
        newPort.setHint(""+PORT_SERVER);
        newPortLags = (EditText)dialog.findViewById(R.id.newportlags);
        newPortLags.setHint(""+PORT_SERVER_LAGS);
        goChanges = (Button)dialog.findViewById(R.id.gochanges);
        goChanges.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (ip(newIP.getText().toString())) {
                        STATIC_IP = newIP.getText().toString();

                    } else throw new Exception();
                    PORT_SERVER = Integer.parseInt(newPort.getText().toString());
                    PORT_SERVER_LAGS = Integer.parseInt(newPortLags.getText().toString());
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Wrong format of Port or IP", Toast.LENGTH_SHORT).show();
                }
            }
        });
        soloIP = (Button)dialog.findViewById(R.id.soloip);
        soloIP.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    if(ip(newIP.getText().toString())) {
                        STATIC_IP = newIP.getText().toString();
                        dialog.dismiss();
                    } else throw new Exception();
                } catch (Exception e) {
                    Log.e(TAG,e.toString());
                    Toast.makeText(MainActivity.this, "Wrong format IP", Toast.LENGTH_SHORT).show();
                }
            }
        });

        soloPort=(Button)dialog.findViewById(R.id.soloport);
        soloPort.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PORT_SERVER = Integer.parseInt(newPort.getText().toString());
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Wrong format Port", Toast.LENGTH_SHORT).show();
                }
            }
        });
        soloPortLags=(Button)dialog.findViewById(R.id.soloportlags);
        soloPortLags.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PORT_SERVER_LAGS = Integer.parseInt(newPortLags.getText().toString());
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Wrong format Port", Toast.LENGTH_SHORT).show();
                }
            }
        });
            dialog.show();
        mSocket = null;

        }

    public static boolean ip(String text) {
        Pattern p = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        Matcher m = p.matcher(text);
        return m.find();
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/Byte.SIZE);
        buffer.putLong(x);
        return buffer.array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/Byte.SIZE);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }


    // redraws a plot whenever an update is received:
    private class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        @Override
        public void update(Observable o, Object arg) {
            plot.redraw();
        }
    }


    class SampleDynamicSeries implements XYSeries {
        private ReadTh datasource;
        private int seriesIndex;
        private String title;

        public SampleDynamicSeries(ReadTh datasource, int seriesIndex, String title) {
            this.datasource = datasource;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return datasource.getItemCount(seriesIndex);
        }

        @Override
        public Number getX(int index) {
            return datasource.getX(seriesIndex, index);
        }

        @Override
        public Number getY(int index) {
            return datasource.getY(seriesIndex, index);
        }
    }

    class SampleDynamicSeries2 implements XYSeries {
        private ReadTh datasource;
        private int seriesIndex;
        private String title;

        public SampleDynamicSeries2(ReadTh datasource, int seriesIndex, String title) {
            this.datasource = datasource;
            this.seriesIndex = seriesIndex;
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int size() {
            return datasource.getItemCountLag(seriesIndex);
        }

        @Override
        public Number getX(int index) {
            return datasource.getXLag(seriesIndex, index);
        }

        @Override
        public Number getY(int index) {
            return datasource.getYLag(seriesIndex, index);
        }
    }


    public int assign(double sign, double angle, double compSign){
        if(compSign >= 0){
            if(angle > 77.5 && angle <= 90) return 1;
            if(angle > 22.5 && angle <= 77.5) return 2;
            if(angle > -22.5 && angle <= 22.5) return 5;
            if(angle > -77.5 && angle <= -22.5) return 8;
            if(angle >= -90 && angle <= -77.5) return 7;

        }else{
            if(angle > 77.5 && angle <= 90) return 1;
            if(angle > 22.5 && angle <= 77.5) return 0;
            if(angle > -22.5 && angle <= 22.5) return 3;
            if(angle > -77.5 && angle <= -22.5) return 6;
            if(angle >= -90 && angle <= -77.5) return 7;
        }
        return 4;
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }


    public DataOutputStream openDOS(String fileName){
        FileOutputStream fos;
        DataOutputStream dos = null;
        try {

            File sdCardDir = Environment.getExternalStorageDirectory();
            File targetFile;
            targetFile = new File(sdCardDir.getCanonicalPath());
            fos = new FileOutputStream(targetFile + "/" + fileName + "_" + getCurrentTimeStamp() + ".txt");
            dos = new DataOutputStream(fos);
        }catch(Exception e){
            e.printStackTrace();
        }
        return dos;
    }


    public boolean writeOnDOS(DataOutputStream obj, short[] list){
        boolean ret = true;
        int len = list.length;
        try {
            for(int i=0;i<len;i++)
                obj.writeShort(list[i]);
        } catch (IOException e) {
            Log.w(TAG, "Writing in closed file");
            return false;
        }

        return ret;
    }

    public boolean writeOnDOS(DataOutputStream obj, double value){
        boolean ret = true;
        try {
            obj.writeDouble(value);
        } catch (IOException e) {
            Log.w(TAG, "Writing in closed file");
            return false;
        }

        return ret;
    }

    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

}