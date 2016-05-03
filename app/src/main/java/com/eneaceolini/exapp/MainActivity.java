package com.eneaceolini.exapp;



import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Debug;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.util.Log;
import android.media.MediaRecorder;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
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
import com.eneaceolini.utility.GraphView;
import com.eneaceolini.utility.MakeACopy;
import com.eneaceolini.wifip2p.WiFiPeerListAdapter;
import com.eneaceolini.wifip2p.WifiP2PReceiverMain;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// For commit in the new desktop


public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {


    private static final String TAG = "MainActivity";
    private static final int UDP_MODE_STREAM = 0;
    private static final int UDP_MODE_DIRECT = 1;
    private static final int UDP_MODE_LAG = 2;
    private UDPCommunicationManager mUDPCOmmunicationManager;
    private MediaRecorder mRecorder = null;
    private AudioTrack mAudioTrack;
    private ToggleButton micType;
    private int buffersize;
    protected static int SAMPLE_RATE = 44100;
    private CheckBox RA, IA, RB, IB;
    private SeekBar omega, scaleFT, scaleMic1, scaleMic2, seekAngle;
    private int MAXFT = 20, MAXAUDIO = 2, MAXAUDIO2 = 2;
    private int OMEGA, ANGLE;
    private TextView omegaText;
    private Switch rectA, rectB, anti, audio, convAct,server1,server2,peer1,peer2;
    public boolean streamToServer1,streamToServer2,streamToPeer1,streamToPeer2;
    private int minFreq2Detect = 100; //Hz
    private int minNumberSamples;
    private ActionBar actionBar;
    private double freqCall = 0.5;//ms
    private int countArrivedSamples = 0;
    private int samplesToPrint = 0;
    private Vector<Integer> lagCollector = new Vector<>();
    private boolean connectionLost = false;
    private final IntentFilter p2pFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2PReceiverMain receiver;
    private ListView listPeers;
    private WiFiPeerListAdapter mWifiPeerListLadapter;
    private ProgressBar progBar;
    public boolean isConnected = false;
    private double TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
    private final int INTERP_RATE = 6;
    private int PORT_SERVER = 6880;
    private int PORT_SERVER_LAGS = 6890;
    private final int PORT_DIRECT = 7880;
    private String STATIC_IP = "172.19.8.136";
    // private String STATIC_IP = "172.19.12.113";
    // private String STATIC_IP = "77.109.166.135";
    private InetAddress directWifiPeerAddress;
    private Button play;
    private byte[] blankSignal;
    private int radioButtonSelected;
    private RadioGroup radioGroup;
    private float KBytesSent = 0.0f;
    private UDPRunnableLags mUDPRunnableLags;
    private long lastRec = 0;
    private Vector<Long> collectionDelays = new Vector<>();
    private Random rm = new Random();
    GlobalNotifierUDP monitorUDPPing = new GlobalNotifierUDP();

    // Graphics
    GraphView mGraphView;
    GraphView mGraphView2;
    GraphView mGraphView3;
    GraphView mGraphView4;
    TextView myLog;
    TextView lag;
    private Button start,stop;
    private TextView kbytes;
    private EditText newIP, newPort,newPortLags;
    private Button goChanges, soloIP, soloPort,soloPortLags, ping;
    private CheckBox playBack;
    // protocols & modalities
    public static D2xxManager ftD2xx = null;
    FT_Device ftDev;
    int DevCount = -1;
    int currentPortIndex = -1;
    int portIndex = -1;
    private boolean isWifiP2pEnabled;
    private TextView seekAngleLabel;

    enum DeviceStatus {
        DEV_NOT_CONNECT,
        DEV_NOT_CONFIG,
        DEV_CONFIG
    }

    private static final int NO_BEAM = 2;
    private static final int D_AND_SUM = 0;
    private static final int D_AND_SUB = 1;
    private RadioGroup radioBeam;
    private int radioBeamSelected = 2;


    private DatagramSocket mSocket,mSocketDirect,mSocketLags;
    private XYPlot graph;
    XYSeries gData;
    LineAndPointFormatter gDataFormat;


    // handler event
    private final int UPDATE_TEXT_VIEW_CONTENT = 0;
    private final int UPDATE_KBYTES_COUNT = 1;
    private final int UPDATE_ASCII_RECEIVE_DATA_BYTES = 17;
    private final int LAUNCH_COMMUNICATION = 22;


    private final byte XON = 0x11;    /* Resume transmission */
    private final byte XOFF = 0x13;    /* Pause transmission */

    // strings of file transfer protocols
    String currentProtocol;

    final int MODE_GENERAL_UART = 0;
    int transferMode = MODE_GENERAL_UART;


    // general data count
    int totalReceiveDataBytes = 0;
    int totalUpdateDataBytes = 0;

    // thread to read the data
    ReadThread readThread; // read data from USB


    boolean bContentFormatHex = false;


    // variables
    final int UI_READ_BUFFER_SIZE = 10240; // Notes: 115K:1440B/100ms, 230k:2880B/100ms
    byte[] writeBuffer;
    byte[] readBuffer;
    char[] readBufferToChar;
    int actualNumBytes;
    int baudRate; /* baud rate */
    byte stopBit; /* 1:1stop bits, 2:2 stop bits */
    byte dataBit; /* 8:8bit, 7: 7bit */
    byte parity; /* 0: none, 1: odd, 2: even, 3: mark, 4: space */
    byte flowControl; /* 0:none, 1: CTS/RTS, 2:DTR/DSR, 3:XOFF/XON */
    public Context global_context;
    boolean uart_configured = false;
    String uartSettings = "";

    BufferedOutputStream buf_save;
    boolean WriteFileThread_start = false;

    String fileNameInfo;
    long start_time;

    // data buffer
    byte[] readDataBuffer; /* circular buffer */

    int iTotalBytes;
    int iReadIndex;

    final int MAX_NUM_BYTES = 65536;

    boolean bReadTheadEnable = false;


    int globalCounter = 1;
    int globalDESIRE = 0;

    public final int MAXCOLLECT = 5;
    public int globalCount = 0;
    String rectData = "";
    double[] FT2 = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)]; //TODO check realloc when changing SR
    double[] tmpSwapBuffer = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)];
    double[] tmpPrint1 = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)];
    double[] tmpPrint2 = new double[getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE)];
    String savedFromBefore = "";
    boolean wasLastNumeric = false;
    boolean isFirstNumeric = false;
    long lastTime = 0;
    private int COUNTER_A = 0;
    private int COUNTER_B = 0;
    private int MIC_TYPE;
    Monitor monitor;
    GlobalNotifier mGlobalNotifier = new GlobalNotifier();
    GlobalNotifier backFire = new GlobalNotifier();
    GlobalNotifier doubleBackFire = new GlobalNotifier();
    GlobalNotifierUDP mGlobalNotifierUDPStream= new GlobalNotifierUDP();
    GlobalNotifierUDP mGlobalNotifierUDPLags = new GlobalNotifierUDP();
    RandomAccessFile RAF;
    RandomAccessFile RAF2;
    UDPRunnable toPingRunnable;
    double[] LAGS;
    int indexOfZeroLag;
    double deltaT,roofLags;

    /* Activity Methods */

    @Override
    public void onCreate(Bundle icicle) {

        global_context = this;
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
        // Log.d(TAG,"" + LAGS[indexOfZeroLag]);
        // for(int i = 0; i < LAGS.length; i++) Log.d(TAG,"" + LAGS[i]);

        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException e) {
            Log.e(TAG, "FTDI_HT getInstance fail!!");
        }

        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

        try {
            File sdCardDir = Environment.getExternalStorageDirectory();
            File targetFile;
            targetFile = new File(sdCardDir.getCanonicalPath());
            File file = new File(targetFile + "/" + "fft_"+getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE)+".txt");
            RAF = new RandomAccessFile(file, "rw");
            RAF.seek(file.length());
        }catch(Exception e){}

        try {
            File sdCardDir = Environment.getExternalStorageDirectory();
            File targetFile;
            targetFile = new File(sdCardDir.getCanonicalPath());
            File file = new File(targetFile + "/" + "stop_"+Constants.FRAME_SIZE+".txt");
            RAF2 = new RandomAccessFile(file, "rw");
            RAF2.seek(file.length());
        }catch(Exception e){}

        monitor = new Monitor();

        // playback
        // prepare audio playback
        buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        Log.d(TAG,""+buffersize);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                , buffersize, AudioTrack.MODE_STREAM);


        ping = (Button)findViewById(R.id.ping);
        ping.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                START = System.currentTimeMillis();

                FFF = false;
                toPingRunnable = new UDPRunnable(directWifiPeerAddress, PORT_DIRECT, new byte[Constants.FRAME_SIZE]);
                FFF = true;
                toPingRunnable.start();
                monitorUDPPing.doNotify();
            }
        });

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

        graph = (XYPlot) findViewById(R.id.main_graph);
        gDataFormat = new LineAndPointFormatter(Color.rgb(0, 200, 0), null,null,null);
        MAXAUDIO = 32768 * 2;
        MAXAUDIO2 = 32768 * 2;

        mGraphView = (GraphView) findViewById(R.id.graph);
        mGraphView.setMaxValue(MAXAUDIO);

        mGraphView2 = (GraphView) findViewById(R.id.graph2);
        mGraphView2.setMaxValue(MAXAUDIO2);

        mGraphView3 = (GraphView) findViewById(R.id.graph3);
        mGraphView3.setMaxValue(MAXFT);

        mGraphView4 = (GraphView) findViewById(R.id.graph4);
        mGraphView4.setMaxValue(256);

        kbytes = (TextView)findViewById(R.id.kbytes);

        lag = (TextView) findViewById(R.id.showLag);

        myLog = (TextView) findViewById(R.id.log);

        progBar = (ProgressBar) findViewById(R.id.progressBar);


        radioGroup = (RadioGroup)findViewById(R.id.radio_button);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                radioButtonSelected = checkedId;
            }
        });

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
        server2 = (Switch) findViewById(R.id.server2);
        server2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                streamToServer2 = isChecked;
            }
        });
        peer1 = (Switch) findViewById(R.id.peer1);
        peer1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                streamToPeer1 = isChecked;
            }
        });
        peer2 = (Switch) findViewById(R.id.peer2);
        peer2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                streamToPeer2 = isChecked;
            }
        });


        micType = (ToggleButton) findViewById(R.id.switch6);
        micType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked)
                    MIC_TYPE = MediaRecorder.AudioSource.CAMCORDER;
                else MIC_TYPE = MediaRecorder.AudioSource.MIC;


            }
        });

        play = (Button) findViewById(R.id.play);
        play.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new PlayAudio().start();
            }
        });
        rectA = (Switch) findViewById(R.id.switch1);
        rectB = (Switch) findViewById(R.id.switch2);
        anti = (Switch) findViewById(R.id.switch3);
        convAct = (Switch) findViewById(R.id.switch5);
        audio = (Switch) findViewById(R.id.switch4);
        audio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mGraphView != null) {
                    if (isChecked) {
                        MAXAUDIO = 32768 * 2;

                    } else MAXAUDIO = 2;
                    mGraphView.setMaxValue(MAXAUDIO);
                    mGraphView2.setMaxValue(MAXAUDIO);
                }
            }
        });

        playBack = (CheckBox) findViewById(R.id.playback);
        playBack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    isPlayBack = true;
                    mAudioTrack.play();
                    //new PlayAudioOnline().start();
                }else{
                    isPlayBack = false;
                    mAudioTrack.stop();
                    //mAudioTrack.release();
                }
            }
        });

        omegaText = (TextView) findViewById(R.id.omegatext);

        RA = (CheckBox) findViewById(R.id.checkBox);
        IA = (CheckBox) findViewById(R.id.checkBox2);
        RB = (CheckBox) findViewById(R.id.checkBox3);
        IB = (CheckBox) findViewById(R.id.checkBox4);

        RA.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    globalDESIRE = 0;
                    IA.setChecked(false);
                    RB.setChecked(false);
                    IB.setChecked(false);
                }
            }
        });

        IA.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    globalDESIRE = 1;
                    RA.setChecked(false);
                    RB.setChecked(false);
                    IB.setChecked(false);
                }
            }
        });

        RB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    globalDESIRE = 2;
                    IA.setChecked(false);
                    RA.setChecked(false);
                    IB.setChecked(false);
                }
            }
        });

        IB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    globalDESIRE = 3;
                    IA.setChecked(false);
                    RB.setChecked(false);
                    RA.setChecked(false);
                }
            }
        });


        seekAngle = (SeekBar) findViewById(R.id.seekAngle);
        seekAngle.setProgress(13);
        seekAngleLabel = (TextView) findViewById(R.id.textView);
        seekAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ANGLE = progress - 13;
                seekAngleLabel.setText("" + ANGLE);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        omega = (SeekBar) findViewById(R.id.seekBar);
        omega.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                OMEGA = progress;
                omegaText.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        scaleFT = (SeekBar) findViewById(R.id.seekBar2);
        scaleFT.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MAXFT = progress;
                mGraphView3.setMaxValue(progress);
                //omegaText.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        scaleMic1 = (SeekBar) findViewById(R.id.seekBar4);
        scaleMic1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress != 0) {
                    MAXAUDIO = 65536 / progress;
                    mGraphView.setMaxValue(65536 / progress);
                }
                //omegaText.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        scaleMic2 = (SeekBar) findViewById(R.id.seekBar3);
        scaleMic2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress != 0) {
                    MAXAUDIO2 = 65536 / progress;
                    mGraphView2.setMaxValue(65536 / progress);
                }
                //omegaText.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        start = (Button) findViewById(R.id.start);

        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {


                startRecording();
                v.setEnabled(false);
                stop.setEnabled(true);

                Log.d(TAG, "Pressed START");
            }
        });


        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Pressed STOP");

                try {
                    stopRecording();
                    countArrivedSamples = 0;
                    server1.setChecked(false);
                    server2.setChecked(false);
                    peer1.setChecked(false);
                    peer2.setChecked(false);
                    kbytes.setText("0.0");
                    KBytesSent = 0.0f;
                    v.setEnabled(false);
                    start.setEnabled(true);
                } catch (Exception e) {
                    Log.w(TAG,"Error in stopping: "+e.toString());
                }
            }
        });

        /* END of Graphics set up

        /* allocate buffer */
        writeBuffer = new byte[512];
        readBuffer = new byte[UI_READ_BUFFER_SIZE * 5];
        readBufferToChar = new char[UI_READ_BUFFER_SIZE];
        readDataBuffer = new byte[MAX_NUM_BYTES];
        actualNumBytes = 0;
        baudRate = 230400;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 1;
        portIndex = 0;
        /* end of buffer allocation */

        // Menage WifiP2P connectivity
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
    }


    @Override
    protected void onStart() {
        super.onStart();
        createDeviceList();
        if (DevCount > 0) {
            connectFunction();
            setUARTInfoString();
            setConfig(baudRate, dataBit, stopBit, parity, flowControl);
        }
    }

    protected void onResume() {
        super.onResume();

        //receiver = new WifiP2PReceiverMain(mManager, mChannel, this);
        //registerReceiver(receiver, p2pFilter);

        if (null == ftDev || !ftDev.isOpen()) {
            createDeviceList();
            if (DevCount > 0) {
                connectFunction();
                setUARTInfoString();
                setConfig(baudRate, dataBit, stopBit, parity, flowControl);
            }
        }
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

        //HOME = menu.findItem(android.R.id.home);
        //HOME.setIcon(new BitmapDrawable(toolBox.myPhoto));

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


    /* UART Methods to menage Arduino Connectivity*/

    public void createDeviceList() {
        int tempDevCount = ftD2xx.createDeviceInfoList(global_context);

        if (tempDevCount > 0) {
            if (DevCount != tempDevCount) {
                DevCount = tempDevCount;
                updatePortNumberSelector();
            }
        } else {
            DevCount = -1;
            currentPortIndex = -1;
        }
    }

    public void disconnectFunction() {
        DevCount = -1;
        currentPortIndex = -1;
        bReadTheadEnable = false;
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (ftDev != null) {
            if (ftDev.isOpen()) {
                ftDev.close();
            }
        }
    }

    public void updatePortNumberSelector() {
        //midToast(DevCount + " port device attached", Toast.LENGTH_SHORT);
    }

    DeviceStatus checkDevice() {
        if (ftDev == null || !ftDev.isOpen()) {
            //midToast("Need to connect to cable.",Toast.LENGTH_SHORT);
            return DeviceStatus.DEV_NOT_CONNECT;
        } else if (uart_configured) {
            //midToast("CHECK: uart_configured == false", Toast.LENGTH_SHORT);
            //midToast("Need to configure UART.",Toast.LENGTH_SHORT);
            return DeviceStatus.DEV_NOT_CONFIG;
        }

        return DeviceStatus.DEV_CONFIG;

    }

    void setUARTInfoString() {
        String parityString, flowString;

        switch (parity) {
            case 0:
                parityString = "None";
                break;
            case 1:
                parityString = "Odd";
                break;
            case 2:
                parityString = "Even";
                break;
            case 3:
                parityString = "Mark";
                break;
            case 4:
                parityString = "Space";
                break;
            default:
                parityString = "None";
                break;
        }

        switch (flowControl) {
            case 0:
                flowString = "None";
                break;
            case 1:
                flowString = "CTS/RTS";
                break;
            case 2:
                flowString = "DTR/DSR";
                break;
            case 3:
                flowString = "XOFF/XON";
                break;
            default:
                flowString = "None";
                break;
        }

        uartSettings = "Port " + portIndex + "; UART Setting  -  Baudrate:" + baudRate + "  StopBit:" + stopBit
                + "  DataBit:" + dataBit + "  Parity:" + parityString
                + "  FlowControl:" + flowString;

        resetStatusData();
    }

    void resetStatusData() {
        String tempStr = "Format - " + (bContentFormatHex ? "Hexadecimal" : "Character") + "\n" + uartSettings;
        String tmp = tempStr.replace("\\n", "\n");
        //uartInfo.setText(tmp);
    }

    void updateStatusData(String str) {
        String temp;
        if (null == fileNameInfo)
            temp = "\n" + str;
        else
            temp = fileNameInfo + "\n" + str;

        String tmp = temp.replace("\\n", "\n");
        //uartInfo.setText(tmp);
    }

    void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl) {
        // configure port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        ftDev.setFlowControl(flowCtrlSetting, XON, XOFF);

        setUARTInfoString();
        //midToast(uartSettings,Toast.LENGTH_SHORT);

        uart_configured = true;
    }

    class ReadThread extends Thread {
        final int USB_DATA_BUFFER = 8192;


        ReadThread() {
            this.setPriority(MAX_PRIORITY);
        }

        public void run() {

            byte[] usbdata = new byte[USB_DATA_BUFFER];
            int readcount ;
            int iWriteIndex = 0;
            bReadTheadEnable = true;
            Log.d(TAG, "<<<<<<<<<<<<STARTED>>>>>>>>>>>");
            while (bReadTheadEnable) {

                readcount = ftDev.getQueueStatus(); // retrive number of bits ready to read...
                if (readcount > 0) {
                    if (readcount > USB_DATA_BUFFER) {
                        readcount = USB_DATA_BUFFER;
                    }
                    ftDev.read(usbdata, readcount); // read in


                    totalReceiveDataBytes += readcount;

                    for (int count = 0; count < readcount; count++) {
                        readDataBuffer[iWriteIndex] = usbdata[count];
                        iWriteIndex++;
                        iWriteIndex %= MAX_NUM_BYTES;
                    }


                    if (iWriteIndex >= iReadIndex) {
                        iTotalBytes = iWriteIndex - iReadIndex;
                    } else {
                        iTotalBytes = (MAX_NUM_BYTES - iReadIndex) + iWriteIndex;
                    }
                }
            }
        }
    }





    byte readData(int numBytes, byte[] buffer) {
        byte intstatus = 0x00; /* success by default */

		/* should be at least one byte to read */
        if ((numBytes < 1) || (0 == iTotalBytes)) {
            actualNumBytes = 0;
            intstatus = 0x01;
            return intstatus;
        }

        if (numBytes > iTotalBytes) {
            numBytes = iTotalBytes;
        }

		/* update the number of bytes available */
        iTotalBytes -= numBytes;
        actualNumBytes = numBytes;

		/* copy to the user buffer */
        byte[] minibuf = null;

        String tmp = "-";
        int[] READ = new int[numBytes]; //just cause I know every byte is a measure

        for (int count = 0; count < numBytes; count++) {
            READ[count] = (readDataBuffer[iReadIndex] & 0xFF);
            tmp = (READ[count] + "\n");


            minibuf = tmp.getBytes();

            for (int i = 0; i < minibuf.length; i++)
                buffer[globalCount + i] = minibuf[i];

            globalCount += minibuf.length;

            iReadIndex++;
            iReadIndex %= MAX_NUM_BYTES;
        }
        final int[] t = MakeACopy.makeACopy(READ);

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                myLog.setText("" + t.length);
//                for (int i = 0; i < t.length; i += 50) {
//                    mGraphView4.addDataPoint(t[i]);
//                }
//            }
//        });


        if (minibuf != null) rectData = tmp;

        return intstatus;
    }


    public void connectFunction() {
        Toast.makeText(this, "connect function", Toast.LENGTH_SHORT).show();
        if (portIndex + 1 > DevCount) portIndex = 0;


        if (currentPortIndex == portIndex
                && ftDev != null
                && ftDev.isOpen()) {
            Toast.makeText(global_context, "Port(" + portIndex + ") is already opened.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bReadTheadEnable) {
            bReadTheadEnable = false;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (null == ftDev) {
            ftDev = ftD2xx.openByIndex(global_context, portIndex);
        } else {
            ftDev = ftD2xx.openByIndex(global_context, portIndex);
        }
        uart_configured = false;

        if (ftDev == null) {
            Toast.makeText(global_context, "Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG);
            return;
        }

        if (ftDev.isOpen()) {
            currentPortIndex = portIndex;
            Toast.makeText(global_context, "open device port(" + portIndex + ") OK", Toast.LENGTH_SHORT).show();

            if (!bReadTheadEnable) {
                readThread = new ReadThread();
                readThread.start();
            }
        } else {
            Toast.makeText(global_context, "Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG).show();
        }
    }


    /* End of UART Methods */


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
                peer2.setEnabled(true);
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

        byte[] receiveData = new byte[Constants.FRAME_SIZE];
        byte[] signalA = new byte[Constants.FRAME_SIZE/2];
        short[] signalCS = new short[Constants.FRAME_SIZE/4];
        byte[] signalB = new byte[Constants.FRAME_SIZE/2];
        short[] mergedSignal = new short[Constants.FRAME_SIZE/2];
        short[] toPrint = new short[Constants.FRAME_SIZE/4];
        short[] signalAS = new short[Constants.FRAME_SIZE/4];
        short[] signalBS = new short[Constants.FRAME_SIZE/4];
        byte[] tmp = new byte[2];
        DatagramPacket receivePacket;

        public long time, back, diff;
        int k = 0;


        public void run()
        {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            //end
//            fileName = Environment.getExternalStorageDirectory().getPath()+"/myrecord.pcm";
//            try {
//                os = new FileOutputStream(fileName);
//            }catch(Exception e){
//                Log.w("os","definition");
//            }

            try{
                DatagramSocket serverSocket = new DatagramSocket(PORT_DIRECT);


                //byte[] sendData = new byte[1024];
                Log.d(TAG,"UDP Server Started: Waiting for Packets...");
                //FileWriter out = new FileWriter("SignalA.txt");
                //FileWriter out2 = new FileWriter("SignalB.txt");
                //BufferedWriter bufWriter = new BufferedWriter(out);
                //BufferedWriter bufWriter2 = new BufferedWriter(out2);

                while(true)
                {
                    receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);

                    time = System.currentTimeMillis() - lastRec;
                    //System.arraycopy(receivePacket.getData(), 0, receiveData, 0, receivePacket.getLength());

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
                    //Log.d("received data byte","" + receiveData.length);
//                    k = 0;
//                    mergedSignal = new short[receiveData.length/N];

                    for(int i = 0;i<Constants.FRAME_SIZE/2;i+=2){
                        signalA[i] = receiveData[2*i];
                        signalA[i + 1] = receiveData[2*i + 1];
                        signalB[i] = receiveData[2*i + 2];
                        signalB[i + 1] = receiveData[2*i + 3];
                    }
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

                    if(isPlayBack ){
                        mAudioTrack.write(signalA, 0, Constants.FRAME_SIZE / 2);
                        //Log.d(TAG,"writing on audio track!");
                    }
//                     toShort(mergedSignal,receiveData);
                    lastRec = System.currentTimeMillis();
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



                    switch(radioButtonSelected){
                        case R.id.m1:
                            toShort(toPrint,signalA);
                            if(isPlayBack ){
                                mAudioTrack.write(signalA, 0, signalA.length);
                            }
                            break;
                        case R.id.m2:
                            toShort(toPrint,signalB);
                            if(isPlayBack ){
                                mAudioTrack.write(signalB, 0, signalB.length);
                            }
                            break;
                    }
//////
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

        if(mUDPRunnableStream == null){
            mUDPRunnableStream = new UDPRunnableStream(doubleBackFire,mGlobalNotifierUDPStream,STATIC_IP,directWifiPeerAddress,PORT_SERVER,PORT_DIRECT,MainActivity.this);
            mUDPRunnableStream.start();
            Log.d(TAG,"strarting UDP");
        }
        if(mUDPRunnableLags == null) {
            mUDPRunnableLags = new UDPRunnableLags(mGlobalNotifierUDPLags, STATIC_IP, PORT_SERVER_LAGS);
            mUDPRunnableLags.start();
        }

        // the lag has to be separated from the stream but direct of not the stream can be sent from the same thread
        fileName = Environment.getExternalStorageDirectory().getPath()+"/myrecord.pcm";
        if(mReadTh == null) {
            mReadTh = new ReadTh(mGlobalNotifier, mGlobalNotifierUDPLags, mGlobalNotifierUDPStream,mUDPRunnableStream);
            mReadTh.start();
        }



        //mAudioAnalyzer = AudioAnalyzer.getInstance();
        mAudioReceiver = new IAudioReceiver(MainActivity.this, fileName);
        mAudioCapturer = AudioCapturer.getInstance(mAudioReceiver, MIC_TYPE,SAMPLE_RATE,mGlobalNotifier,mReadTh,backFire);
        mAudioCapturer.start();
    }

    public void stopRecording() {
        //mReadTh.STOP = true;
        //mReadTh.stop();
        if(mAudioCapturer != null) {
            mAudioCapturer.stop();
            mAudioCapturer.destroy();
        }

        // Writing delays on file


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
        public short[] playbackSignalA = new short[Constants.FRAME_SIZE / 4];
        public short[] playbackSignalB = new short[Constants.FRAME_SIZE / 4];
        double[] cumulativeSignalA = new double[minimumNumberSamples];
        double[] cumulativeSignalB = new double[minimumNumberSamples];
        double[] convolution = new double[minimumNumberSamples];
        Number [] tograph;
        GlobalNotifier monitor;
        GlobalNotifierUDP monitorUDPLags;
        GlobalNotifierUDP monitorUDPStream;
        FileOutputStream os;
        double lagg, theta, val;
        byte[] toSend = new byte[8];
        boolean first = true;
        int n;

        public ReadTh(GlobalNotifier monitor, GlobalNotifierUDP monitorUDPLags,GlobalNotifierUDP monitorUDPStream,UDPRunnableStream str){

            mUDPStream = str;
            this.monitor = monitor;
            this.monitorUDPLags = monitorUDPLags;
            this.monitorUDPStream = monitorUDPStream;
            try {
                os = new FileOutputStream(fileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

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
                    // Since the size of the cumulative is exactely the samples we need to do the analysis I just fill it and do it


                    for (int i = 0,j = 0; i < Constants.FRAME_SIZE/2; i += 2,j++) {
                        playbackSignalA[j] = globalSignal[i];
                        playbackSignalB[j] = globalSignal[i+1];
                    }

                    switch(radioBeamSelected){
                        case 0:
                            for(int i = 0,j=0; i < Constants.FRAME_SIZE/4; i++,j+=2) {
                                if (i + ANGLE < Constants.FRAME_SIZE / 4 && i + ANGLE > 0)
                                    playbackSignalA[i] = (short) (globalSignal[j] + globalSignal[j+1 + 2*ANGLE]);
                            }
                            break;
                        case 1:
                            for(int i = 0,j=0; i < Constants.FRAME_SIZE/4; i++,j+=2) {
                                if (i + ANGLE < Constants.FRAME_SIZE / 4 && i + ANGLE > 0)
                                    playbackSignalA[i] = (short) (globalSignal[j] - globalSignal[j+1 + 2*ANGLE]);
                            }
                            break;
                        case 2:
                            break;
                    }


                    if(isPlayBack){
                        mAudioTrack.write(playbackSignalA,0,playbackSignalA.length);
                    }

                    if(countArrivedSamples + n/2 < minimumNumberSamples){
                        for (int i = 0, j = 0; i < n - 1; i += 2,j++) {
                            cumulativeSignalA[countArrivedSamples] = (double) globalSignal[i];
                            cumulativeSignalB[countArrivedSamples++] = (double) globalSignal[i + 1];
                        }
                        backFire.doNotify();
                    }else { // I start the analysis
                        for (int i = 0,j = 0; i < n - 1; i += 2,j++) {
                            cumulativeSignalA[countArrivedSamples] = (double) globalSignal[i];
                            cumulativeSignalB[countArrivedSamples++] = (double) globalSignal[i + 1];
                        }
                        backFire.doNotify();
                        samplesToPrint += n / 2;
                        countArrivedSamples = 0;

//                        System.arraycopy(cumulativeSignalA,0,tmpPrint1,0,minimumNumberSamples);
//                        System.arraycopy(cumulativeSignalB,0,tmpPrint2,0,minimumNumberSamples);
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    for (int i = 0; i < minimumNumberSamples; i += (MAXCOLLECT + 5 + SAMPLE_RATE / 500)) {
//
//                                        mGraphView.addDataPoint((float) tmpPrint1[i] + (MAXAUDIO / 2));
//                                        mGraphView2.addDataPoint((float) tmpPrint2[i] + (MAXAUDIO2 / 2));
//                                    }
//                                }
//                            });

                        //While I am doing the analysis the thread cannot fill the cumulative signal,
                        //this mean that If I run slower than A new samples I will probabibly loose it
                        // in that case I think I have to decrease the minNumberos samples.


                        //I just fill the cumulative so I don't need this
                    /*
                    float[] tmpCA = new float[cumulativeSignalA.length + sAD.length];
                    float[] tmpCB = new float[cumulativeSignalB.length + sBD.length];

                    System.arraycopy(cumulativeSignalA, 0, tmpCA, 0, cumulativeSignalA.length);
                    System.arraycopy(cumulativeSignalB, 0, tmpCB, 0, cumulativeSignalB.length);
                    System.arraycopy(sAD, 0, tmpCA, cumulativeSignalA.length, sAD.length);
                    System.arraycopy(sBD, 0, tmpCB, cumulativeSignalB.length, sBD.length);

                    cumulativeSignalA = new float[cumulativeSignalA.length + sAD.length];
                    cumulativeSignalB = new float[cumulativeSignalB.length + sBD.length];

                    System.arraycopy(tmpCA, 0, cumulativeSignalA, 0, tmpCA.length);
                    System.arraycopy(tmpCB, 0, cumulativeSignalB, 0, tmpCB.length);
                    */

                    // I fill the buffer complitely this mean that i know that every signal is a power of 2 so i can start the analys

                            //trasf

                            // Native way

                        //START_FFT= System.nanoTime();

//                        fft.fft(cumulativeSignalA, signalAim);
//                        fft.fft(cumulativeSignalB, signalBim);
//                        for (int i = 0; i < minimumNumberSamples; i++) {
//                            convolution[i] = cumulativeSignalA[i] * cumulativeSignalB[i] + signalAim[i] * signalBim[i];
//                            convolutionIm[i] = -signalAim[i] * cumulativeSignalB[i] + cumulativeSignalA[i] * signalBim[i]; // the minus is for complex conjugate
//                        }
//                        fft.ifft(convolution, convolutionIm);
//                        STOP_FFT = System.nanoTime() - START_FFT;
//
//                        try {
//                        try {
//
//                            RAF.write((STOP_FFT+"\n").getBytes());
//                            //RAF.close();
//                            Log.d(TAG,"" +STOP_FFT);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        complexA = fft.fftw(cumulativeSignalA);
//                        complexB = fft.fftw(cumulativeSignalB);
////
////                        //
////
////
//                        for (int i = 0; i < minimumNumberSamples; i++) {
//                            complexConv[0][i] = complexA[0][i] * complexB[0][i] + complexA[1][i] * complexB[1][i];
//                            complexConv[1][i] = -complexA[1][i] * complexB[0][i] + complexA[0][i] * complexB[1][i]; // the minus is for complex conjugate
//                        }


                            convolution = fft.corr_fftw(cumulativeSignalA,cumulativeSignalB);


//                        tograph = new Number[convolution.length];
//                        for(int i = 0;i < convolution.length; i++)
//                            tograph[i] = convolution[i];
//                        //clear existing graph
//                        //graph.clear();
//                        //put data from tograph into a series to be added to the graph
//                        gData = new SimpleXYSeries(Arrays.asList(tograph), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "gData");
//                        //add series with line and dot format specified earlier to graph
//                        graph.addSeries(gData, gDataFormat);
                        //graph.redraw();
//                        for(int i = 0; i <minimumNumberSamples; i++)
//                            Log.d(TAG,""+convolution[i]);

                        //fft.ifft(convolution, convolutionIm);


                //TODO re-add the interpolation for better quality
                /*
                CubicInterpolation1d cubicInterpolation1d = new CubicInterpolation1d();
                float y_interp[] = cubicInterpolation1d.interpolate(convolution, INTERP_RATE);
                float x_interp[] = new float[convolution.length * INTERP_RATE];
                for (int j = 0; j < y_interp.length; j++) {
                    x_interp[j] = y_interp[j] / INTERP_RATE;
                }
                */
                            //final double[] FT2 = x_interp;

                        //now convolution has the signal that i need to show and
                        // since I don't want to allocate every time I will have a global variable that will be used
                            //final double[] FT2 = convolution;


                            //rearrange signal so lag 0 is in the middle
                            //System.arraycopy(convolution, 0, FT2, 0, minimumNumberSamples);
//
//                            //int con = 0;
//                            System.arraycopy(FT2, l, tmpSwapBuffer, 0, l);
//                            //for (int i = l; i < l*2; i++) tmp[con++] = FT2[i];
//                            //con = 0;
//                            System.arraycopy(FT2, 0, FT2, l, l);
//                            //for (int i = l; i < l*2; i++) FT2[i] = FT2[con++];
//                            System.arraycopy(tmpSwapBuffer, 0, FT2, 0, l);
//                            //for (int i = 0; i < l; i++) FT2[i] = tmp[i];


//                            if (radioButtonSelected == R.id.corr) {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        for (int i = 0; i < l*2; i += (MAXCOLLECT + 5 + SAMPLE_RATE / 500)) {
//                                            mGraphView3.addDataPoint((float) FT2[i] / 1000000 + MAXFT / 2);
//                                        }
//                                    }
//                                });
//                            }

                            //does lag collector allocate memory every time?
                            lagCollector.add(findMaxLag(convolution));
//                            lagg = findMaxLag(FT2); // in seconds
//                            lagCollector.add(lagg);
//                            val = lagg * 343f / 0.14f;
//                            theta = Math.toDegrees(Math.asin(Math.signum(val) * Math.min(1.0, Math.abs(val))));
//                            runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                lag.setText(String.format("Mean lag " + "%.2f" + " degrees", theta));
//                                }
//                            });
                        if (samplesToPrint >= TOTAL_SAMPLES) {
                                lagg = meanLag(lagCollector); // in seconds
                                val = lagg * 343f / 0.14f;
                                theta = Math.toDegrees(Math.acos(Math.signum(val) * Math.min(1.0, Math.abs(val))));
                                toSend = toByteArray(lagg);
                                System.arraycopy(toSend, 0, monitorUDPLags.packetByte,0, 8); // I free the monitor.packet
                                monitorUDPLags.doNotify();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        lag.setText(String.format("Mean Theta " + "%.2f" + " degrees" , theta));
                                    }
                                });

                                lagCollector.removeAllElements();
                                samplesToPrint = 0;

                        }

                        //Clean
                        for(int i = 0;i<minimumNumberSamples;i++){
                            cumulativeSignalA[i] = 0;
                            cumulativeSignalB[i] = 0;
                            convolution[i] = 0;
//                            convolutionIm[i] = 0;
//                            signalAim[i] = 0;
//                            signalBim[i]= 0;
                        }
                    }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
            }
        }
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
        ByteBuffer.wrap(bytes).putDouble(value);
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
//        if(Math.abs(indexOfZeroLag - index) >= 6){
//            if(indexOfZeroLag > index) index = indexOfZeroLag - 5;
//            if(indexOfZeroLag < index) index = indexOfZeroLag + 5;
//        }
//        Log.d(TAG,"" + index + " " + LAGS[index]);
//        Log.d(TAG,"" + tap);
        return tap;
    }

    public double meanLag(Vector<Integer> x)
    {
        final int s = x.size();
        double mean=0;
        for(int i = 0;i < s; i++)
            mean+=x.get(i);
        return (float) mean / s * deltaT;
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



    public double[] createCos(int n)
    {
        int MAX = OMEGA;

        double[] cos = new double[n];


        for (int i = 0; i < n; i++)
        {
            cos[i] = Math.cos(MAX * Math.PI * (double) i / n);
        }
        return cos;
    }

    public double[] createSin(int n)
    {
        int MAX = OMEGA;

        double[] sin = new double[n];

        for (int i = 0; i < n; i++)
        {
            sin[i] = Math.sin(MAX * Math.PI * (double) i / n);

        }

        return sin;
    }

    public double[] createRect(int n)
    {
        int DUTY = OMEGA;
        double[] rect = new double[n];
        for(int i=0;i<n;i++)
            rect[i]=1;
        if(DUTY<n)
            for(int i=DUTY;i<n;i++)
                rect[i]=0;
        return rect;
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
            return Constants.FRAME_SIZE / 2;
        }
        //I'm sure that it is power of 2
    }



    //UDP Communication Handling
    //It's always better to use Thread in UDP Comm
    // The signal can be reconstructed at the server side



    public class UDPRunnableTot implements Runnable
    {
        private InetAddress Iaddress;
        private int port;
        private byte[] data;
        Thread thread;
        float start,stop;
        DatagramSocket currentSocket;

        UDPRunnableTot(String add,int port,byte[] msg,int mode){
            try
            {
                Iaddress = InetAddress.getByName(add);
            }
            catch(Exception e)
            {
                Log.w(TAG,"Packet not sent, Invalid Address");
            }

            switch(mode) {
                case UDP_MODE_STREAM:
                    currentSocket = mSocket;
                    break;
                case UDP_MODE_DIRECT:
                    currentSocket = mSocketDirect;
                    break;
                case UDP_MODE_LAG:
                    currentSocket = mSocketLags;
                    break;
            }

            this.port = port;
            data = msg;
        }

        UDPRunnableTot(InetAddress add,int port,byte[] msg,int mode){
                Iaddress = add;
            switch(mode) {
                case UDP_MODE_STREAM:
                    currentSocket = mSocket;
                    break;
                case UDP_MODE_DIRECT:
                    currentSocket = mSocketDirect;
                    break;
                case UDP_MODE_LAG:
                    currentSocket = mSocketLags;
                    break;
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

                if (currentSocket == null) {
                    Log.d(TAG,"Socket is null");
                    currentSocket = new DatagramSocket(null);
                    currentSocket.setReuseAddress(true);
                    currentSocket = new DatagramSocket();
                    currentSocket.connect(Iaddress, port);
                    currentSocket.setBroadcast(true);
                }

                while(true) {
                    monitorUDPPing.doWait();
                    DatagramPacket sendPacket = new DatagramPacket(monitorUDPPing.packetByte, monitorUDPPing.packetByte.length, Iaddress, port);
                    currentSocket.send(sendPacket);
                    //Update total data sent
                    KBytesSent += (1.0 * data.length) / 1000;
                    connectionLost = false;
                }
            }catch(Exception e){
                //Log.e("UDP",e.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionLost = true;

                    }
                });
            }finally{
                if(connectionLost) {
                    connectionLost = false;
                }
            }

        }
    }

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




    public class UDPThread extends Thread
    {

        private InetAddress Iaddress;
        private int port;
        private byte[] data;

        UDPThread(String add,int port,byte[] msg){
            try {
                Iaddress = InetAddress.getByName(add);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
            this.port = port;
            data = msg;
        }




        public void run(){
            try {

                if (mSocket == null) {
                    Log.d(TAG,"Socket is null");
                    mSocket = new DatagramSocket(null);
                    mSocket.setReuseAddress(true);
                    mSocket = new DatagramSocket();
                    mSocket.connect(Iaddress, port);
                    mSocket.setBroadcast(true);
                }



                DatagramPacket sendPacket = new DatagramPacket(data, data.length, Iaddress, port);
                mSocket.send(sendPacket);
                //Update total data sent
                KBytesSent += (1.0*data.length)/1000;
                connectionLost = false;


            }catch(Exception e){
                //Log.e("UDP",e.toString());


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
                connectionLost = false;
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
    ListView pp;
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


//    private native void SuperpoweredExample(String apkPath, long[] offsetAndLength);
//    private native void onPlayPause(boolean play);
//    private native void onCrossfader(int value);
//    private native void onFxSelect(int value);
//    private native void onFxOff();
//    private native void onFxValue(int value);
//
//    static {
//        System.loadLibrary("SuperpoweredExample");
//    }


}