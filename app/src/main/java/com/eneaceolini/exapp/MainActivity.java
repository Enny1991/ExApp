package com.eneaceolini.exapp;



import android.app.Dialog;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.ActionBar;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class MainActivity extends ActionBarActivity implements PopupMenu.OnMenuItemClickListener {


    private static final String TAG = "MainActivity";
    private MediaRecorder mRecorder = null;
    private AudioTrack mAudioTrack;
    private ToggleButton micType;
    private int buffersize;
    int SAMPLE_RATE = 16000;
    private CheckBox RA, IA, RB, IB;
    private SeekBar omega, scaleFT, scaleMic1, scaleMic2;
    private int MAXFT = 20, MAXAUDIO = 2, MAXAUDIO2 = 2;
    private int OMEGA;
    private TextView omegaText;
    private Switch rectA, rectB, anti, audio, convAct,server1,server2,peer1,peer2;
    private boolean streamToServer1,streamToServer2,streamToPeer1,streamToPeer2;
    private int minFreq2Detect = 100; //Hz
    private int minNumberSamples;
    private ActionBar actionBar;
    private double freqCall = 0.5;//ms
    private int countArrivedSamples = 0;
    private int samplesToPrint = 0;
    private Vector<Double> lagCollector = new Vector<>();
    private boolean connectionLost = false;
    private final IntentFilter p2pFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2PReceiverV2 receiver;
    private ListView listPeers;
    private WiFiPeerListAdapter mWifiPeerListLadapter;
    private ProgressBar progBar;
    public boolean isConnected = false;
    private double TOTAL_SAMPLES = SAMPLE_RATE * freqCall;
    private final int INTERP_RATE = 6;
    private int PORT_SERVER = 6880;
    private final int PORT_DIRECT = 7880;
    private String STATIC_IP = "172.19.12.186";
    private InetAddress directWifiPeerAddress;
    private Button play;
    private byte[] blankSignal;
    private int radioButtonSelected;
    private RadioGroup radioGroup;
    private float KBytesSent = 0.0f;


    // Graphics
    GraphView mGraphView;
    GraphView mGraphView2;
    GraphView mGraphView3;
    GraphView mGraphView4;
    TextView myLog;
    TextView lag;
    private Button start,stop;
    private TextView kbytes;
    private EditText newIP, newPort;
    private Button goChanges, soloIP, soloPort;
    private Dialog dialog;
    // protocols & modalities
    public static D2xxManager ftD2xx = null;
    FT_Device ftDev;
    int DevCount = -1;
    int currentPortIndex = -1;
    int portIndex = -1;
    private boolean isWifiP2pEnabled;

    enum DeviceStatus {
        DEV_NOT_CONNECT,
        DEV_NOT_CONFIG,
        DEV_CONFIG
    }


    private DatagramSocket mSocket,mSocketDirect;


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
    HandlerThread handlerThread; // update data to UI
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
    double[] cumulativeSignalA = new double[0];
    double[] cumulativeSignalB = new double[0];
    public final int MAXCOLLECT = 5;
    public int globalCount = 0;
    String rectData = "";

    String savedFromBefore = "";
    boolean wasLastNumeric = false;
    boolean isFirstNumeric = false;
    long lastTime = 0;
    private int COUNTER_A = 0;
    private int COUNTER_B = 0;
    private int MIC_TYPE;



    /* Activity Methods */

    @Override
    public void onCreate(Bundle icicle) {

        global_context = this;

        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException e) {
            Log.e(TAG, "FTDI_HT getInstance fail!!");
        }

        super.onCreate(icicle);
        setContentView(R.layout.activity_main);

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

        omega = (SeekBar) findViewById(R.id.seekBar);
        omega.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                OMEGA = progress;
                omegaText.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "StartTracking", Toast.LENGTH_SHORT);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "StopTracking", Toast.LENGTH_SHORT);
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
                Toast.makeText(MainActivity.this, "StartTracking", Toast.LENGTH_SHORT);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "StopTracking", Toast.LENGTH_SHORT);
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
                Toast.makeText(MainActivity.this, "StartTracking", Toast.LENGTH_SHORT);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "StopTracking", Toast.LENGTH_SHORT);
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
                Toast.makeText(MainActivity.this, "StartTracking", Toast.LENGTH_SHORT);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "StopTracking", Toast.LENGTH_SHORT);
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

        receiver = new WifiP2PReceiverV2(mManager, mChannel, this);
        registerReceiver(receiver, p2pFilter);

        if (null == ftDev || false == ftDev.isOpen()) {
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
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 8 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
            case R.id.rate_16:
                SAMPLE_RATE = 16000;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 16 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.rate_32:
                SAMPLE_RATE = 32000;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 32 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
            case R.id.rate_44:
                SAMPLE_RATE = 44100;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 44.1 kHz " + minNumberSamples + "\n Press Start", Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
            case R.id.hz_100:
                minFreq2Detect = 100;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 100 Hz", Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
            case R.id.hz_1000:
                minFreq2Detect = 1000;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect, SAMPLE_RATE);
                Toast.makeText(MainActivity.this, "Changed to 1 kHz", Toast.LENGTH_SHORT).show();
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
            if (true == ftDev.isOpen()) {
                ftDev.close();
            }
        }
    }

    public void updatePortNumberSelector() {
        //midToast(DevCount + " port device attached", Toast.LENGTH_SHORT);
    }

    DeviceStatus checkDevice() {
        if (ftDev == null || false == ftDev.isOpen()) {
            //midToast("Need to connect to cable.",Toast.LENGTH_SHORT);
            return DeviceStatus.DEV_NOT_CONNECT;
        } else if (false == uart_configured) {
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
                parityString = new String("None");
                break;
            case 1:
                parityString = new String("Odd");
                break;
            case 2:
                parityString = new String("Even");
                break;
            case 3:
                parityString = new String("Mark");
                break;
            case 4:
                parityString = new String("Space");
                break;
            default:
                parityString = new String("None");
                break;
        }

        switch (flowControl) {
            case 0:
                flowString = new String("None");
                break;
            case 1:
                flowString = new String("CTS/RTS");
                break;
            case 2:
                flowString = new String("DTR/DSR");
                break;
            case 3:
                flowString = new String("XOFF/XON");
                break;
            default:
                flowString = new String("None");
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

        Handler mHandler;

        ReadThread(Handler h) {
            mHandler = h;
            this.setPriority(MAX_PRIORITY);
        }

        public void run() {

            byte[] usbdata = new byte[USB_DATA_BUFFER];
            int readcount = 0;
            int iWriteIndex = 0;
            bReadTheadEnable = true;
            Log.d(TAG, "<<<<<<<<<<<<STARTED>>>>>>>>>>>");
            while (true == bReadTheadEnable) {

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

    // Update UI content
    class HandlerThread extends Thread {
        Handler mHandler;

        HandlerThread(Handler h) {
            mHandler = h;
        }

        public void run() {
            byte status;
            Message msg;

            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (true == bContentFormatHex) // consume input data at hex content format
                {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);
                } else if (MODE_GENERAL_UART == transferMode) {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);

                    if (0x00 == status) {

                        // save data to file
                        if (true == WriteFileThread_start && buf_save != null) {
                            try {
                                buf_save.write(readBuffer, 0, globalCount);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        globalCount = 0;
                    }

                }
            }
        }
    }


    Handler handler = new Handler() {


        public void handleMessage(Message msg)

        {
            final Message f = msg;

            switch (msg.what) {

                case UPDATE_KBYTES_COUNT:
                    //Log.d(TAG,"Called UPDATE");
                    //double[] sAD = msg.getData().getDoubleArray("signalA");
                    //double[] sBD = msg.getData().getDoubleArray("signalB");

                    //byte[] sAD = msg.getData().getByteArray("signalA");
                    //byte[] sBD = msg.getData().getByteArray("signalB");



                    break;
                case LAUNCH_COMMUNICATION:
                    //new MyClientTask(STATIC_IP,PORT,""+MSG).execute();
                    break;

                case UPDATE_TEXT_VIEW_CONTENT:
                    if (actualNumBytes > 0) {
                        totalUpdateDataBytes += actualNumBytes;

                        for (int i = 0; i < actualNumBytes; i++) {
                            readBufferToChar[i] = (char) readBuffer[i];
                        }
                        try {
                            String firstByte = String.copyValueOf(readBufferToChar, 0, 1);
                            String lastByte = String.copyValueOf(readBufferToChar, actualNumBytes - 1, 1);
                            String[] arr = (String.copyValueOf(readBufferToChar, 0, actualNumBytes)).split("\\n");
                            String toApp = "";
                            isFirstNumeric = isNumeric(firstByte);
                            //appendData(firstByte + " <-> " + savedFromBefore);
                            if (wasLastNumeric && isFirstNumeric) {
                                //appendDat
                                // a("\n"+savedFromBefore + arr[0]);
                                mGraphView3.addDataPoint(Float.parseFloat(savedFromBefore + arr[0]));

                                //}
                            } else if (isNumeric(arr[0]))//appendData("\n"+arr[0]);
                                mGraphView3.addDataPoint(Float.parseFloat(arr[0]));
                            for (int i = 1; i < arr.length - 1; i++)
                                //if(isNumeric(arr[i]))//appendData("\n"+arr[i]);
                                mGraphView3.addDataPoint(Float.parseFloat(arr[i]));


                            if (isNumeric(lastByte)) wasLastNumeric = true;
                            else wasLastNumeric = false;
                            savedFromBefore = arr[arr.length - 1];
                            if (lastTime != 0) {
                                long delta = System.currentTimeMillis() - lastTime;
                                //appendData("" + arr.length + " samples in " + delta + " ms\n");
                            }
                            lastTime = System.currentTimeMillis();
                        } catch (Exception e) {

                        }

                    }
                    break;


                case UPDATE_ASCII_RECEIVE_DATA_BYTES: {
                    String temp = currentProtocol;
                    if (totalReceiveDataBytes <= 10240)
                        temp += " Receive " + totalReceiveDataBytes + "Bytes";
                    else
                        temp += " Receive " + new java.text.DecimalFormat("#.00").format(totalReceiveDataBytes / (double) 1024) + "KBytes";

                    long tempTime = System.currentTimeMillis();
                    Double diffime = (double) (tempTime - start_time) / 1000;
                    temp += " in " + diffime.toString() + " seconds";

                    //updateStatusData(temp);
                }
                break;


                default:
                    Toast.makeText(global_context, "NG CASE", Toast.LENGTH_LONG);
                    //Toast.makeText(global_context, ".", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };




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

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myLog.setText("" + t.length);
                for (int i = 0; i < t.length; i += 50) {
                    mGraphView4.addDataPoint(t[i]);
                }
            }
        });


        if (minibuf != null) rectData = tmp;

        return intstatus;
    }


    public void connectFunction() {
        Toast.makeText(this, "connect function", Toast.LENGTH_SHORT).show();
        if (portIndex + 1 > DevCount) portIndex = 0;


        if (currentPortIndex == portIndex
                && ftDev != null
                && true == ftDev.isOpen()) {
            Toast.makeText(global_context, "Port(" + portIndex + ") is already opened.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (true == bReadTheadEnable) {
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

        if (true == ftDev.isOpen()) {
            currentPortIndex = portIndex;
            Toast.makeText(global_context, "open device port(" + portIndex + ") OK", Toast.LENGTH_SHORT).show();

            if (false == bReadTheadEnable) {
                readThread = new ReadThread(handler);
                readThread.start();
                handlerThread = new HandlerThread(handler);
                handlerThread.start();
            }
        } else {
            Toast.makeText(global_context, "Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG);
        }
    }


    /* End of UART Methods */


    /* Methods for WifiP2P */

    private boolean sendToDirect = false;

    public void setDirectWifiPeerAddress(InetAddress address) {
        directWifiPeerAddress = address;
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

    public static short toShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }


    public class UDPServer extends Thread
    {

        public void run()
        {

            try{
                DatagramSocket serverSocket = new DatagramSocket(PORT_DIRECT);
                byte[] receiveData = new byte[2048];

                //byte[] sendData = new byte[1024];
                byte[] rc;
                Log.d(TAG,"UDP Server Started: Waiting for Packets...");
                //FileWriter out = new FileWriter("SignalA.txt");
                //FileWriter out2 = new FileWriter("SignalB.txt");
                //BufferedWriter bufWriter = new BufferedWriter(out);
                //BufferedWriter bufWriter2 = new BufferedWriter(out2);
                short[] signalA,signalB;
                short[] mergedSignal;
                while(true)
                {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    serverSocket.receive(receivePacket);
                    receiveData = new byte[receivePacket.getLength()];
                    System.arraycopy(receivePacket.getData(), 0, receiveData, 0, receivePacket.getLength());
                    try{
                        //receiveData = receivePacket.getData();
                    }catch(Exception e){
                        System.out.println(e.toString());
                    }

                    int N = 2;
                    mergedSignal = new short[receiveData.length/N];
                    byte[] tmp = new byte[N];
                    for(int i = 0;i<receiveData.length/N;i++){
                        for(int j = 0;j<N;j++) tmp[j] = receiveData[i*N + j];
                        mergedSignal[i] = toShort(tmp);
                    }

                    signalA = new short[mergedSignal.length/2];
                    signalB = new short[mergedSignal.length/2];
                    System.arraycopy(mergedSignal, 0, signalA, 0, mergedSignal.length/2);
                    System.arraycopy(mergedSignal, signalA.length, signalB, 0, mergedSignal.length/2);

                    short[] toPrint = null;

                    switch(radioButtonSelected){
                        case R.id.m1:
                            toPrint = MakeACopy.makeACopy(signalA);
                            break;
                        case R.id.m2:
                            toPrint = MakeACopy.makeACopy(signalB);
                            break;
                    }

                    final short[] toPrint2 = toPrint;

                    if(null != toPrint2) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int l = toPrint2.length;

                                for (int i = 0; i < l; i += (MAXCOLLECT + 5 + SAMPLE_RATE / 500 )) {
                                    mGraphView3.addDataPoint((float) toPrint2[i] / 10 + MAXFT / 2);
                                }

                            }
                        });
                    }

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

    public void setWifiPeerListLadapter(List wifiPeerListLadapter) {
        this.mWifiPeerListLadapter = new WiFiPeerListAdapter(MainActivity.this, wifiPeerListLadapter);
        //progBar.setVisibility(View.INVISIBLE);

        showPeersList();
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
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.
            }

            @Override
            public void onFailure(int reasonCode) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
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

    public void startRecording() {
        String filePath = "/sdcard/myrecord.pcm";

        //mAudioAnalyzer = AudioAnalyzer.getInstance();
        mAudioReceiver = new IAudioReceiver(MainActivity.this, filePath, handler);
        mAudioCapturer = AudioCapturer.getInstance(mAudioReceiver, MIC_TYPE,SAMPLE_RATE);
        mAudioCapturer.start();
    }

    public void stopRecording() {
        mAudioCapturer.stop();
        mAudioCapturer.destroy();
    }

    public int ACTUAL_COUNTER=0;

    public void updateGraphs(final byte[] sA, final byte[] sB, final short[] sAS, final short[] sBS, double[] sAD,  double[] sBD) {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < sAS.length; i += sAS.length / 10) {
                    mGraphView.addDataPoint((float) sAS[i] + (MAXAUDIO / 2));
                    mGraphView2.addDataPoint((float) sBS[i] + (MAXAUDIO2 / 2));
                    kbytes.setText(String.format("%.4f", KBytesSent));
                }
            }
        });


        try {

            //send to wifidirect if active
            blankSignal = new byte[sA.length];
            if(streamToPeer1 && streamToPeer2) new UDPRunnableDirect(directWifiPeerAddress, PORT_DIRECT, mergeArray(sA,sB)).start();
            else if(streamToPeer1) new UDPRunnableDirect(directWifiPeerAddress, PORT_DIRECT, mergeArray(sA,blankSignal)).start();
            else if(streamToPeer2) new UDPRunnableDirect(directWifiPeerAddress, PORT_DIRECT, mergeArray(blankSignal,sB)).start();
            if(streamToServer1 && streamToServer2) new UDPRunnable(STATIC_IP, PORT_SERVER, mergeArray(sA,sB)).start();
            else if(streamToServer1) new UDPRunnable(STATIC_IP, PORT_SERVER, mergeArray(sA,blankSignal)).start();
            else if(streamToServer2) new UDPRunnable(STATIC_IP, PORT_SERVER,mergeArray(blankSignal,sB)).start();

            //new TCPThread(STATIC_IP, PORT, MakeACopy.makeACopy(sA)).start();
            ACTUAL_COUNTER++;
        } catch (Exception e) {
            Log.w(TAG,"Packet Not Sent");
        }


        //put it on handler



        int n = sAD.length * 2;

        countArrivedSamples += n / 2;
        samplesToPrint += n / 2;
        double[] tmpCA = new double[cumulativeSignalA.length + sAD.length];
        double[] tmpCB = new double[cumulativeSignalB.length + sBD.length];

        System.arraycopy(cumulativeSignalA,0,tmpCA,0,cumulativeSignalA.length);
        System.arraycopy(cumulativeSignalB,0,tmpCB,0,cumulativeSignalB.length);
        System.arraycopy(sAD,0,tmpCA,cumulativeSignalA.length,sAD.length);
        System.arraycopy(sBD,0,tmpCB,cumulativeSignalB.length,sBD.length);

        cumulativeSignalA = new double[cumulativeSignalA.length + sAD.length];
        cumulativeSignalB = new double[cumulativeSignalB.length + sBD.length];

        System.arraycopy(tmpCA,0,cumulativeSignalA,0,tmpCA.length);
        System.arraycopy(tmpCB,0,cumulativeSignalB,0,tmpCB.length);

        if (countArrivedSamples >= minNumberSamples) {
            new ReadTh(MakeACopy.makeACopy(cumulativeSignalA), MakeACopy.makeACopy(cumulativeSignalB)).start();
            cumulativeSignalA = new double[0];
            cumulativeSignalB = new double[0];
            countArrivedSamples = 0;
        }


    }




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
            byte[] byteData = null;
            File file = null;
            file = new File("/sdcard/myrecord.pcm");

            byteData = new byte[(int) count];
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            int bytesread = 0, ret = 0;
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
            }

        }
    }


    class ReadTh extends Thread {
        boolean STOP = false;
        double[] x,y;

        public ReadTh(double[]x, double[] y){
            this.x = x;
            this.y = y;
        }


        @Override
        public void run() {


            try {
                double[] signalA, signalB;


                int n2 ;
                int nn = x.length;

                //check power of 2
                if ((nn & (nn - 1)) == 0) {
                    signalA = x;
                    signalB = y;
                } else {
                    n2 = nn;
                    n2--;
                    n2 |= n2 >> 1;   // Divide by 2^k for consecutive doublings of k up to 32,
                    n2 |= n2 >> 2;   // and then or the results.
                    n2 |= n2 >> 4;
                    n2 |= n2 >> 8;
                    n2 |= n2 >> 16;
                    n2++;
                    signalA = new double[n2];
                    signalB = new double[n2];
                    System.arraycopy(x,0,signalA,0,nn);
                    System.arraycopy(y, 0, signalB, 0, nn);
                }


                double[] signalAim = new double[signalA.length];
                double[] signalBim = new double[signalB.length];
                double[] convolution = new double[signalB.length];
                double[] convolutionIm = new double[signalB.length];


                FFTHelper fft = new FFTHelper(signalA.length);


                //trasf
                fft.fft(signalA, signalAim);
                fft.fft(signalB, signalBim);

                for (int i = 0; i < signalA.length; i++) {
                    convolution[i] = signalA[i] * signalB[i] + signalAim[i] * signalBim[i];
                    convolutionIm[i] = -signalAim[i] * signalB[i] + signalA[i] * signalBim[i]; // the minus is for complex conjugate
                }

                fft.ifft(convolution, convolutionIm);

                CubicInterpolation1d cubicInterpolation1d = new CubicInterpolation1d();
                double y_interp[] = cubicInterpolation1d.interpolate(convolution, INTERP_RATE);
                double x_interp[] = new double[convolution.length * INTERP_RATE];
                for (int j = 0; j < y_interp.length; j++) {
                    x_interp[j] = y_interp[j] / INTERP_RATE;
                }

                final double[] FT2 = x_interp;


                //rearrange signal so lag 0 is in the middle
                final int l = FT2.length / 2;
                double[] tmp = new double[l];
                //int con = 0;
                System.arraycopy(FT2,l,tmp,0,l);
                //for (int i = l; i < l*2; i++) tmp[con++] = FT2[i];
                //con = 0;
                System.arraycopy(FT2,0,FT2,l,l);
                //for (int i = l; i < l*2; i++) FT2[i] = FT2[con++];
                System.arraycopy(tmp,0,FT2,0,l);
                //for (int i = 0; i < l; i++) FT2[i] = tmp[i];



                if(radioButtonSelected == R.id.corr)
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < l; i += (MAXCOLLECT + 5 + SAMPLE_RATE / 500)) {
                                mGraphView3.addDataPoint((float) FT2[i] / 1000000 + MAXFT / 2);
                            }
                        }
                    });
                }


                lagCollector.add(findMaxLag(FT2, 1f / (SAMPLE_RATE * INTERP_RATE)));


                if (samplesToPrint >= TOTAL_SAMPLES) {

                    final double lagg = meanLag(lagCollector) * 1000; // in seconds
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            lag.setText(String.format("Mean lag " + "%.2f" + " ms", lagg));
                        }
                    });

                    lagCollector = new Vector<>();
                    samplesToPrint = 0;
                }

            }catch(Exception e)
            {
                Log.w(TAG,""+e.toString());
            }

    }


    }


    public static byte[] mergeArray(byte[] x,byte[] y){
        //double[] merged = new double[x.length + y.length];
        byte[] merged = new byte[(x.length + y.length)];
        System.arraycopy(x,0,merged,0,x.length);
        System.arraycopy(y,0,merged,x.length,y.length);


        return merged;
    }

    public static byte[] mergeArrayAndGetBytes(double[] x, double[] y )
    {
        //double[] merged = new double[x.length + y.length];
        byte[] merged = new byte[(x.length + y.length)*8];
        /*
        int min;
        boolean flag = true;
        if(x.length<=y.length) min = x.length;
        else {
            min = y.length;
            flag = false;
        }
        if(flag){
            for(int i = 0;i<min;i++){
                merged[2 * i] = x[i];
                merged[2 * i + 1] = y[i];
            }
            for(int i = x.length;i < y.length;i++) merged[i+2*x.length] = y[i];
        }else{

            for(int i = 0;i<min;i++){
                merged[2 * i] = x[i];
                merged[2 * i + 1] = y[i];
            }
            for(int i = y.length;i < x.length;i++) merged[i+2*x.length] = y[i];
        }
        */
        byte[] tmpx,tmpy;
        //Log.d(TAG,"First Value = "+x[0]);

        // would be faster if i pass only integers coming from the PCM

        for(int i = 0;i<x.length;i++)
        {
            //merged[2 * i] = x[i];
            //merged[2 * i + 1] = y[i];

            tmpx = toByteArray(x[i]);
            tmpy = toByteArray(y[i]);
            for(int j = 0; j < 8; j++)
            { // 8 cause i know it's double
                merged[8 * 2 * i + j] = tmpx[j];
                merged[8 * (2 * i + 1 ) + j] = tmpy[j];
            }
        }


        return merged;
    }

    public static byte[] mergeArrayAndGetBytes(short[] x, short[] y )
    {
        //double[] merged = new double[x.length + y.length];
        byte[] merged = new byte[(x.length + y.length)*2];

        byte[] tmpx = short2byte(x);
        byte[] tmpy = short2byte(y);


        // would be faster if i pass only integers coming from the PCM

        System.arraycopy(tmpx,0,merged,0,x.length);
        System.arraycopy(tmpy,0,merged,x.length,x.length);
        Log.d(TAG,"Merging");


        return merged;
    }


    public static byte[] mergeArrayAndGetBytes(int[] x, int[] y )
    {
        //double[] merged = new double[x.length + y.length];
        byte[] merged = new byte[(x.length + y.length)*4];
        /*
        int min;
        boolean flag = true;
        if(x.length<=y.length) min = x.length;
        else {
            min = y.length;
            flag = false;
        }
        if(flag){
            for(int i = 0;i<min;i++){
                merged[2 * i] = x[i];
                merged[2 * i + 1] = y[i];
            }
            for(int i = x.length;i < y.length;i++) merged[i+2*x.length] = y[i];
        }else{

            for(int i = 0;i<min;i++){
                merged[2 * i] = x[i];
                merged[2 * i + 1] = y[i];
            }
            for(int i = y.length;i < x.length;i++) merged[i+2*x.length] = y[i];
        }
        */
        byte[] tmpx,tmpy;
        Log.d(TAG,"First Value = "+x[0]);

        // would be faster if i pass only integers coming from the PCM

        for(int i = 0;i<x.length;i++)
        {
            //merged[2 * i] = x[i];
            //merged[2 * i + 1] = y[i];

            tmpx = toByteArray(x[i]);
            tmpy = toByteArray(y[i]);
            for(int j = 0; j < 4; j++)
            { // 8 cause i know it's double
                merged[4 * 2 * i + j] = tmpx[j];
                merged[4 * (2 * i + 1 ) + j] = tmpy[j];
            }
        }


        return merged;
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




    public static double findMaxLag(double[] x,double deltaT)
    {
        final int l = x.length;
        int indexOfZeroLag = (l - 1)/2; // I take for granted the signal has an even number of samples
        double[] lags = new double[l];
        for(int i = 0; i <=indexOfZeroLag; i++)
        {
            lags[indexOfZeroLag-i] = - deltaT * i;
            lags[indexOfZeroLag+i+1] = deltaT * ( i + 1 );
        }
        int index=0;
        for(int i = 1;i<l;i++)
            if(x[i]>x[index])
                index = i;
        return lags[index];
    }

    public double meanLag(Vector<Double> x)
    {
        final int s = x.size();
        double mean=0;
        for(int i = 0;i < s; i++)
            mean+=x.get(i);
        return mean/s;
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
            double f = i;
            cos[i] = Math.cos(MAX * Math.PI * f / n);
        }
        return cos;
    }

    public double[] createSin(int n)
    {
        int MAX = OMEGA;

        double[] sin = new double[n];

        for (int i = 0; i < n; i++)
        {
            double f = i;
            sin[i] = Math.sin(MAX * Math.PI * f / n);

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
        return (int) Math.ceil(2 * ( 1f / minFreq ) * samplingRate);
    }



    //UDP Communication Handling
    //It's always better to use Thread in UDP Comm
    // The signal can be reconstructed at the server side


    public class UDPRunnable implements Runnable
    {

        private InetAddress Iaddress;
        private int port;
        private byte[] data;
        Thread thread;

        UDPRunnable(String add,int port,byte[] msg){
            try
            {
                Iaddress = InetAddress.getByName(add);
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



                DatagramPacket sendPacket = new DatagramPacket(data, data.length, Iaddress, port);
                mSocket.send(sendPacket);
                //Update total data sent
                KBytesSent += (1.0 * data.length)/1000;
                connectionLost = false;

            }catch(Exception e){
                Log.e("UDP",e.toString());
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



    public class UDPThread extends Thread
    {

        private InetAddress Iaddress;
        private int port;
        private byte[] data;

        UDPThread(String add,int port,byte[] msg){
            try {
                Iaddress = InetAddress.getByName(add);
            }catch(Exception e){};
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
                Log.e("UDP",e.toString());
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

    public class UDPRunnableDirect implements Runnable
    {

        private InetAddress Iaddress;
        private int port;
        private byte[] data;
        Thread thread;



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
                KBytesSent += (1.0*data.length)/1000;

                //Message msg = hdl.obtainMessage(UPDATE_KBYTES_COUNT);
                //hdl.sendMessage(msg);
                //Log.d(TAG, "ts:" + data.length);
                connectionLost = false;

            }catch(Exception e){
                Log.e("UDP",e.toString());
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

    public void showPeersList()
    {

        listPeers = (ListView)findViewById(R.id.listView);

        if(mWifiPeerListLadapter != null)
        listPeers.setAdapter(mWifiPeerListLadapter);
        listPeers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiP2pDevice device = (WifiP2pDevice) mWifiPeerListLadapter.getItem(position);

                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;

                if(isConnected){

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
                }
                else
                {

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
        goChanges = (Button)dialog.findViewById(R.id.gochanges);
        goChanges.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(ip(newIP.getText().toString())) {
                        STATIC_IP = newIP.getText().toString();

                    } else throw new Exception();
                    PORT_SERVER = Integer.parseInt(newPort.getText().toString());

                    dialog.dismiss();
                }catch(Exception e){
                    Toast.makeText(MainActivity.this,"Wrong format of Port or IP",Toast.LENGTH_SHORT).show();
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
            dialog.show();
        mSocket = null;

        }

    public static boolean ip(String text) {
        Pattern p = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
        Matcher m = p.matcher(text);
        return m.find();
    }
}