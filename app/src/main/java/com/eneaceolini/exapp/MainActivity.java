package com.eneaceolini.exapp;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.util.Log;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;


public class MainActivity extends ActionBarActivity implements PopupMenu.OnMenuItemClickListener
{
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;

    private MediaRecorder mRecorder = null;

    private MediaPlayer   mPlayer = null;

    int buffersize;
    AudioRecord m_record;
    int SAMPLE_RATE = 16000;
    float quantStep = 2^16/5;
    private CheckBox RA,IA,RB,IB;
    private SeekBar omega,scaleFT,scaleMic1,scaleMic2;
    private int MAXFT=20,MAXAUDIO = 2;
    private int OMEGA;
    private TextView omegaText;
    private Switch rectA,rectB,anti,audio,convAct;
    private int minFreq2Detect = 100; //Hz
    private int minNumberSamples;
    private ActionBar actionBar;
    private double freqCall = 0.5;//ms
    private boolean SEND_ACTIVE = false;
    private boolean STOP_SENDING = false;
    private TextView showLatency;
    private int countArrivedSamples=0;
    private int samplesToPrint=0;
    private Vector<Double> lagCollector = new Vector<>();
    private ImageView statusNetwork;
    private boolean connectionLost = false;
    private final IntentFilter p2pFilter = new IntentFilter();
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WIfiP2PReceiver receiver;
    private List peers = new ArrayList();
    private ListView listPeers;
    private Dialog selectPeer;
    private WiFiPeerListAdapter mWifiPeerListLadapter;
    private Button findPeers;
    private ProgressBar progBar;
    public boolean isConnected = false;


    private double TOTAL_SAMPLES =  SAMPLE_RATE * freqCall;
    private double sumPeriods;

    private int INTERP_RATE = 6;
    private int PORT = 6880;
    private String STATIC_IP = "172.19.12.186";
    private byte[] MSG;
    private byte[] bufferToTransfer;


    short[]   buffer  ;



    ReadTh mRT;



    // Graphics

    GraphView mGraphView;
    GraphView mGraphView2;
    GraphView mGraphView3;
    GraphView mGraphView4;
    TextView myLog;
    TextView myLog2;
    TextView myLog3;
    TextView lag;
    private LinearLayout baseChangeIP;
    private EditText newIP, newPort;
    private Button goChanges,soloIP,soloPort;
    private Dialog dialog;
    // protocols & modalities
    public static D2xxManager ftD2xx = null;
    FT_Device ftDev;
    int DevCount = -1;
    int currentPortIndex = -1;
    int portIndex = -1;
    private boolean isWifiP2pEnabled;

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public void setWifiPeerListLadapter(List wifiPeerListLadapter) {
        this.mWifiPeerListLadapter = new WiFiPeerListAdapter(MainActivity.this,wifiPeerListLadapter);
        progBar.setVisibility(View.INVISIBLE);
        new TimeOutThread().start();
        try {
            selectPeer.dismiss();
        }catch(Exception e){}
        showPeersList();
    }


    enum DeviceStatus{
        DEV_NOT_CONNECT,
        DEV_NOT_CONFIG,
        DEV_CONFIG
    }

    boolean INTERNAL_DEBUG_TRACE = false; // Toast message for debug

    // menu item
    Menu myMenu;
    final int MENU_CONTENT_FORMAT = Menu.FIRST;
    final int MENU_FONT_SIZE = Menu.FIRST + 1;
    final int MENU_SAVE_CONTENT_DATA = Menu.FIRST + 2;
    final int MENU_CLEAN_SCREEN = Menu.FIRST + 3;
    final int MENU_ECHO = Menu.FIRST + 4;
    final int MENU_HELP = Menu.FIRST + 5;
    final int MENU_SETTING = Menu.FIRST + 6;

    final String[] contentFormatItems = {"Character","Hexadecimal"};
    final String[] fontSizeItems = {"5","6","7","8","10","12","14","16","18","20"};
    final String[] echoSettingItems = {"On","Off"};

    // log tag
    final String TT = "Trace";
    final String TXS = "XM-Send";
    final String TXR = "XM-Rec";
    final String TYS = "YM-Send";
    final String TYR = "YM-Rec";
    final String TZS = "ZM-Send";
    final String TZR = "ZM-Rec";

    // handler event
    final int UPDATE_TEXT_VIEW_CONTENT = 0;

    final int UPDATE_ASCII_RECEIVE_DATA_BYTES = 17;

    final int LAUNCH_COMMUNICATION = 22;



    final byte XON = 0x11;    /* Resume transmission */
    final byte XOFF = 0x13;    /* Pause transmission */

    // strings of file transfer protocols
    final String[] protocolItems = {"ASCII","XModem-CheckSum","XModem-CRC","XModem-1KCRC","YModem","ZModem"};
    String currentProtocol;

    final int MODE_GENERAL_UART = 0;


    int transferMode = MODE_GENERAL_UART;

    final byte ACK = 6;    /* ACKnowlege */
    final byte NAK = 0x15; /* Negative AcKnowlege */

    final byte CHAR_C = 0x43; /* Character 'C' */
    final byte CHAR_G = 0x47; /* Character 'G' */




    int[] modemReceiveDataBytes;
    byte[] modemDataBuffer;


    boolean bModemGetNak = false;
    boolean bModemGetAck = false;
    boolean bModemGetCharC = false;
    boolean bModemGetCharG = false;






    // general data count
    int totalReceiveDataBytes = 0;
    int totalUpdateDataBytes = 0;


    File mPath = new File(android.os.Environment.getExternalStorageDirectory() + "//DIR//");
    File fGetFile = null;

    static RelativeLayout mMenuSetting;
    static RelativeLayout mMenuKey;

    long back_button_click_time;
    boolean bBackButtonClick = false;


    // thread to read the data
    HandlerThread handlerThread; // update data to UI
    ReadThread readThread; // read data from USB


    boolean bSendHexData = false;


    CharSequence contentCharSequence; // contain entire text content
    boolean bContentFormatHex = false;
    int contentFontSize = 12;
    boolean bWriteEcho = true;

    // show information message while send data by tapping "Write" button in hex content format
    int timesMessageHexFormatWriteData = 0;

    // note: when this values changed, need to check main.xml - android:id="@+id/ReadValues - android:maxLines="5000"
    final int TEXT_MAX_LINE = 1000;

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

    String uartSettings  = "";

    //public static final int maxReadLength = 256;
    byte[] usbdata;
    char[] readDataToText;
    public int iavailable = 0;

    // file access//
    FileInputStream inputstream;
    FileOutputStream outputstream;

    FileWriter file_writer;
    FileReader file_reader;
    FileInputStream fis_open;
    FileOutputStream fos_save;
    BufferedOutputStream buf_save;
    boolean WriteFileThread_start = false;

    String fileNameInfo;
    String sFileName;
    int iFileSize = 0;
    int sendByteCount = 0;
    long start_time, end_time;
    long cal_time_1, cal_time_2;

    // data buffer
    byte[] writeDataBuffer;
    byte[] readDataBuffer; /* circular buffer */

    int iTotalBytes;
    int iReadIndex;

    final int MAX_NUM_BYTES = 65536;

    boolean bReadTheadEnable = false;


    public void createDeviceList()
    {
        int tempDevCount = ftD2xx.createDeviceInfoList(global_context);

        if (tempDevCount > 0)
        {
            if( DevCount != tempDevCount )
            {
                DevCount = tempDevCount;
                updatePortNumberSelector();
            }
        }
        else
        {
            DevCount = -1;
            currentPortIndex = -1;
        }
    }

    public void disconnectFunction()
    {
        DevCount = -1;
        currentPortIndex = -1;
        bReadTheadEnable = false;
        try
        {
            Thread.sleep(50);
        }
        catch (InterruptedException e) {e.printStackTrace();}

        if(ftDev != null)
        {
            if( true == ftDev.isOpen())
            {
                ftDev.close();
            }
        }
    }

    public void updatePortNumberSelector()
    {
        //midToast(DevCount + " port device attached", Toast.LENGTH_SHORT);


    }

    DeviceStatus checkDevice()
    {
        if(ftDev == null || false == ftDev.isOpen())
        {
            //midToast("Need to connect to cable.",Toast.LENGTH_SHORT);
            return DeviceStatus.DEV_NOT_CONNECT;
        }
        else if(false == uart_configured)
        {
            //midToast("CHECK: uart_configured == false", Toast.LENGTH_SHORT);
            //midToast("Need to configure UART.",Toast.LENGTH_SHORT);
            return DeviceStatus.DEV_NOT_CONFIG;
        }

        return DeviceStatus.DEV_CONFIG;

    }

    void setUARTInfoString()
    {
        String parityString, flowString;

        switch(parity)
        {
            case 0: parityString = new String("None"); break;
            case 1: parityString = new String("Odd"); break;
            case 2: parityString = new String("Even"); break;
            case 3: parityString = new String("Mark"); break;
            case 4: parityString = new String("Space"); break;
            default: parityString = new String("None"); break;
        }

        switch(flowControl)
        {
            case 0: flowString = new String("None"); break;
            case 1: flowString = new String("CTS/RTS"); break;
            case 2: flowString = new String("DTR/DSR"); break;
            case 3: flowString = new String("XOFF/XON"); break;
            default: flowString = new String("None"); break;
        }

        uartSettings = "Port " + portIndex + "; UART Setting  -  Baudrate:" + baudRate + "  StopBit:" + stopBit
                + "  DataBit:" + dataBit + "  Parity:" + parityString
                + "  FlowControl:" + flowString;

        resetStatusData();
    }

    void resetStatusData()
    {
        String tempStr = "Format - " + (bContentFormatHex?"Hexadecimal":"Character") +"\n"+ uartSettings;
        String tmp = tempStr.replace("\\n", "\n");
        //uartInfo.setText(tmp);
    }

    void updateStatusData(String str)
    {
        String temp;
        if(null == fileNameInfo)
            temp = "\n" + str;
        else
            temp = fileNameInfo + "\n" + str;

        String tmp = temp.replace("\\n", "\n");
        //uartInfo.setText(tmp);
    }

    void setConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl)
    {
        // configure port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits)
        {
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

        switch (stopBits)
        {
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

        switch (parity)
        {
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
        switch (flowControl)
        {
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



    public class TimeOutThread extends Thread{
        public void run(){
            try{
                Thread.sleep(15000);
                progBar.setVisibility(View.INVISIBLE);
            }catch (Exception e){
                Log.e("TimeOut",e.toString());
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        createDeviceList();
        if(DevCount > 0)
        {
            connectFunction();
            setUARTInfoString();
            setConfig(baudRate, dataBit, stopBit, parity, flowControl);
        }
    }

    protected void onResume()
    {
        super.onResume();

        receiver = new WIfiP2PReceiver(mManager, mChannel, this);
        registerReceiver(receiver, p2pFilter);

        if(null == ftDev || false == ftDev.isOpen())
        {
            createDeviceList();
            if(DevCount > 0)
            {
                connectFunction();
                setUARTInfoString();
                setConfig(baudRate, dataBit, stopBit, parity, flowControl);
            }
        }
    }


    @Override
    public void onCreate(Bundle icicle) {
        try
        {
            ftD2xx = D2xxManager.getInstance(this);
        }
        catch (D2xxManager.D2xxException e) {Log.e("FTDI_HT","getInstance fail!!");}

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

        MAXAUDIO = 32768 * 2;
        global_context = this;
        mGraphView = (GraphView) findViewById(R.id.graph);
        mGraphView.setMaxValue(MAXAUDIO);

        mGraphView2 = (GraphView) findViewById(R.id.graph2);
        mGraphView2.setMaxValue(MAXAUDIO);

        mGraphView3 = (GraphView) findViewById(R.id.graph3);
        mGraphView3.setMaxValue(MAXFT);

        mGraphView4 = (GraphView) findViewById(R.id.graph4);
        mGraphView4.setMaxValue(256);

        actionBar = getSupportActionBar();

        lag = (TextView)findViewById(R.id.showLag);

        myLog = (TextView)findViewById(R.id.log);
        statusNetwork = (ImageView)findViewById(R.id.status_net);
        progBar = (ProgressBar)findViewById(R.id.progressBar);

        findPeers = (Button)findViewById(R.id.button2);
        findPeers.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                lookForPeers(v);
                progBar.setVisibility(View.VISIBLE);
            }
        });



        /* allocate buffer */
        writeBuffer = new byte[512];
        readBuffer = new byte[UI_READ_BUFFER_SIZE*5];
        readBufferToChar = new char[UI_READ_BUFFER_SIZE];
        readDataBuffer = new byte[MAX_NUM_BYTES];
        actualNumBytes = 0;
        baudRate = 230400;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 1;
        portIndex = 0;


        rectA = (Switch)findViewById(R.id.switch1);
        rectB = (Switch)findViewById(R.id.switch2);
        anti = (Switch)findViewById(R.id.switch3);
        convAct = (Switch)findViewById(R.id.switch5);
        audio = (Switch)findViewById(R.id.switch4);
        audio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mGraphView!=null) {
                    if (isChecked){
                        MAXAUDIO = 32768 * 2;

                    }
                    else MAXAUDIO = 2;
                    mGraphView.setMaxValue(MAXAUDIO);
                    mGraphView2.setMaxValue(MAXAUDIO);
                }
            }
        });

        omegaText = (TextView)findViewById(R.id.omegatext);

        RA = (CheckBox)findViewById(R.id.checkBox);
        IA = (CheckBox)findViewById(R.id.checkBox2);
        RB = (CheckBox)findViewById(R.id.checkBox3);
        IB = (CheckBox)findViewById(R.id.checkBox4);

        RA.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
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
                if(isChecked){
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
                if(isChecked){
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

        omega = (SeekBar)findViewById(R.id.seekBar);
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

        scaleFT = (SeekBar)findViewById(R.id.seekBar2);
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

        scaleMic1 = (SeekBar)findViewById(R.id.seekBar4);
        scaleMic1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MAXAUDIO = 65536/progress;
                mGraphView.setMaxValue(65536/progress);
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

        scaleMic2 = (SeekBar)findViewById(R.id.seekBar4);
        scaleMic2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MAXAUDIO = 65536/progress;
                mGraphView2.setMaxValue(65536/progress);
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

        Button start = (Button)findViewById(R.id.start);

        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //myReadTask = new ReadTask(handler);
                //myReadTask.execute(mSensorValue);
                statusNetwork.setImageDrawable(getResources().getDrawable(R.mipmap.ic_net_on));
                mRT = new ReadTh();
                mRT.start();
                new SendMessage().start();
                STOP_SENDING = false;
                Log.d("pressed", "START");
            }
        });


        Button stop = (Button)findViewById(R.id.stop);
        stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("pressed", "Stop");
                statusNetwork.setImageDrawable(getResources().getDrawable(R.mipmap.ic_net_off));
                //myReadTask.myCancel(true);
                //myReadTask.cancel(true);
                mRT.myCancel(true);
                m_record.stop();
                STOP_SENDING = true;
                //myAsync = null;
                //stop =true;
            }
        });


        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);


    }




    private void lookForPeers(View v){
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d("Launched", "discovery");
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

    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(receiver);
        }catch(Exception e){}
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        //HOME = menu.findItem(android.R.id.home);
        //HOME.setIcon(new BitmapDrawable(toolBox.myPhoto));

        return super.onCreateOptionsMenu(menu);
    }

    public void showPopup(){
        View menuItemView = findViewById(R.id.action_rate);
        PopupMenu popup = new PopupMenu(MainActivity.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.context_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }
    public void showPopup2(){
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
        View itemView;
        switch (item.getItemId()) {
            case android.R.id.home:

                return true;
            case R.id.action_tcp:
                startActivity(new Intent(MainActivity.this,TCPClient.class));
                return true;
            case R.id.action_spc:
                startActivity(new Intent(MainActivity.this,SpectrogramActivity.class));
                return true;
            case R.id.action_tcp_set:
                showTCPSettings();
                return true;
            case R.id.action_rate:
                showPopup();
                return true;
            case R.id.action_min_freq:
                showPopup2();
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
                //myReadTask.myCancel(true);
                //myReadTask.cancel(true);
                mRT.myCancel(true);
                m_record.stop();

                minNumberSamples = getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE);
                Toast.makeText(MainActivity.this,"Changed to 8 kHz "+minNumberSamples,Toast.LENGTH_SHORT).show();

                //myReadTask = new ReadTask(handler);
                //myReadTask.execute(mSensorValue);
                //mRT.myCancel(false);
                mRT = new ReadTh();
                mRT.start();
                //RESTART
                return true;
            case R.id.rate_16:
                //STOP and change SAMPLE_RATE = 16E3;
                //myReadTask.myCancel(true);
                //myReadTask.cancel(true);

                mRT.myCancel(true);
                m_record.stop();

                SAMPLE_RATE = 16000;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE);
                Toast.makeText(MainActivity.this,"Changed to 16 kHz "+minNumberSamples,Toast.LENGTH_SHORT).show();
                //RESTART
                //myReadTask = new ReadTask(handler);
                //myReadTask.execute(mSensorValue);
                mRT = new ReadTh();
                mRT.start();
                return true;
            case R.id.hz_100:
                minFreq2Detect = 100;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE);
                Toast.makeText(MainActivity.this,"Changed to 100 Hz",Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
            case R.id.hz_1000:
                minFreq2Detect = 1000;
                minNumberSamples = getMinNumberOfSamples(minFreq2Detect,SAMPLE_RATE);
                Toast.makeText(MainActivity.this,"Changed to 1 kHz",Toast.LENGTH_SHORT).show();
                //RESTART
                return true;
        }
        return false;
    }








    class ReadTh extends Thread {
        boolean STOP = false;


        @Override
        public void run() {
            //return (sensorValue[0]);  // This goes to result

            try {
                buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
                buffer = new short[buffersize];

                m_record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, buffersize * 1);
            } catch (Throwable t) {
                Log.e("Error", "Initializing Audio Record and Play objects Failed " + t.getLocalizedMessage());
            }


            m_record.startRecording();

            while (!STOP) {
                try {
                    final int n = m_record.read(buffer, 0, buffersize);
                    // I receive a new packet from the buffer
                    //notifyNewBuffer(n);
                    double[] signalA = new double[n / 2];
                    double[] signalB = new double[n / 2];

                    int k = 0;
                    for (int i = 0; i < n - 1; i++) { // I extract the two channels and plot them
                        signalA[k] = buffer[i];
                        signalB[k] = buffer[i + 1];

                        i++;
                        k++;
                    }
                    final double[] tmpA = signalA.clone();
                    final double[] tmpB = signalB.clone();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (audio.isChecked()) {
                                for (int i = 0; i < tmpA.length; i += SAMPLE_RATE / 500) {
                                    mGraphView.addDataPoint((float) tmpA[i] + (MAXAUDIO / 2));
                                    mGraphView2.addDataPoint((float) tmpB[i] + (MAXAUDIO / 2));
                                }
                            }
                        }
                    });


                    //if the number of samples is not enough to compute correlation i should wait for the next packet
                    countArrivedSamples += n / 2;
                    samplesToPrint += n / 2;
                    if (countArrivedSamples < minNumberSamples) {
                        double[] tmpCA = new double[cumulativeSignalA.length + signalA.length];
                        for (int i = 0; i < cumulativeSignalA.length; i++)
                            tmpCA[i] = cumulativeSignalA[i];
                        double[] tmpCB = new double[cumulativeSignalB.length + signalB.length];
                        for (int i = 0; i < cumulativeSignalB.length; i++)
                            tmpCB[i] = cumulativeSignalB[i];
                        for (int i = 1; i <= signalA.length; i++) {
                            tmpCA[cumulativeSignalA.length - 1 + i] = signalA[i - 1];
                            tmpCB[cumulativeSignalB.length - 1 + i] = signalB[i - 1];
                        }
                        cumulativeSignalA = tmpCA;
                        cumulativeSignalB = tmpCB;
                    } else { // if they are enough i put the last part in the cumulative signal and compute DFT
                        countArrivedSamples = 0;
                        final double[] tmpCA = new double[cumulativeSignalA.length + signalA.length];
                        for (int i = 0; i < cumulativeSignalA.length; i++)
                            tmpCA[i] = cumulativeSignalA[i];
                        double[] tmpCB = new double[cumulativeSignalB.length + signalB.length];
                        for (int i = 0; i < cumulativeSignalB.length; i++)
                            tmpCB[i] = cumulativeSignalB[i];
                        for (int i = 1; i <= signalA.length; i++) {
                            tmpCA[cumulativeSignalA.length - 1 + i] = signalA[i - 1];
                            tmpCB[cumulativeSignalB.length - 1 + i] = signalB[i - 1];
                        }
                        cumulativeSignalA = tmpCA;
                        cumulativeSignalB = tmpCB;

                        int n2 = 0;
                        int nn = cumulativeSignalA.length;

                        //check power of 2
                        if ((nn & (nn - 1)) == 0) {
                            signalA = cumulativeSignalA;
                            signalB = cumulativeSignalB;
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
                            for (int i = 0; i < nn; i++) {
                                signalA[i] = cumulativeSignalA[i];
                                signalB[i] = cumulativeSignalB[i];
                            }
                        }


                        //Log.d("LENGTH 1",""+cumulativeSignalA.length);

                        //TRY with cosine
                    /*
                    double[][] cossin = new double[2][];
                    if(!audio.isChecked()) {
                        double[] newSignalA,newSignalB;
                        if (rectA.isChecked()) newSignalA = createRect(cumulativeSignalA.length);
                        else newSignalA = createCos(cumulativeSignalA.length);
                        if (rectB.isChecked()) newSignalB = createRect(cumulativeSignalA.length);
                        else newSignalB = createSin(cumulativeSignalA.length);
                        cossin[0] = newSignalA;
                        cossin[1] = newSignalB;

                        cumulativeSignalA = cossin[0]; // cos
                        cumulativeSignalB = cossin[1]; // sin
                    }else{
                        cossin[0] = cumulativeSignalA;
                        cossin[1] = cumulativeSignalB;
                    }
                    */
                        //Log.d("first of cumA",""+cumulativeSignalA[0]);
                        double[] signalAim = new double[signalA.length];
                        double[] signalBim = new double[signalB.length];
                        double[] convolution = new double[signalB.length];
                        double[] convolutionIm = new double[signalB.length];
                    /*
                    FFTHelper fft = new FFTHelper(cumulativeSignalA.length);
                    double[][] transfA = fft.fft(cumulativeSignalA, signalAim);
                    double[][] transfB = fft.fft(cumulativeSignalB, signalBim);
                    double[] transfARe = transfA[0];
                    double[] transfAIm = transfA[1];
                    double[] transfBRe = transfB[0];
                    double[] transfBIm = transfB[1];
                    double out = 0;

                    //Log.d("first of fftAIm",""+transfAIm[20]);
                    double[] matHelp;

                    //Log.d("first of conv",""+convolution[0]);
                    */

                        FFTHelper fft = new FFTHelper(signalA.length);
                        //final double[] F1 = cossin[0].clone(),F2 = cossin[1].clone();

                        //trasf
                        fft.fft(signalA, signalAim);
                        fft.fft(signalB, signalBim);

                        for (int i = 0; i < signalA.length; i++) {
                            //matHelp = multiplyComplex(newcumulativeSignalA)
                            convolution[i] = signalA[i] * signalB[i] + signalAim[i] * signalBim[i];
                            convolutionIm[i] = -signalAim[i] * signalB[i] + signalA[i] * signalBim[i]; // the minus is for complex conjugate
                        }

                        //inverse
                    /*
                    if(anti.isChecked()){
                        double[] zeroPaddingRe = new double[convolution.length];
                        double[] zeroPaddingIm = new double[convolution.length];
                        for(int i = 0;i<convolution.length;i++){
                            zeroPaddingRe[i]=convolution[i];
                            zeroPaddingIm[i]=convolutionIm[i];
                        }
                        fft.ifft(zeroPaddingRe, zeroPaddingIm);
                        for(int i = 0;i<convolution.length/2;i++){
                            convolution[i]=zeroPaddingRe[i];
                            convolutionIm[i] = zeroPaddingIm[i];
                        }
                        for(int i = convolution.length/2;i<convolution.length;i++){
                            convolution[i]=0;
                            convolutionIm[i]=0;
                        }
                    }


                    if(anti.isChecked()){
                        fft.ifft(convolution, convolutionIm);
                        fft.ifft(cossin[1], signalBim);
                        fft.ifft(cossin[0], signalAim);
                    }
                    */

                        //fft.ifft(cossin[1], signalBim);
                        //fft.ifft(cossin[0], signalAim);


                    /*
                    double[] FT;
                    switch(globalDESIRE) {

                        case 1:
                            if(convAct.isChecked()) FT = convolutionIm;
                            else FT = signalAim.clone();

                            break;
                        case 2:

                            if(convAct.isChecked()) FT = convolution;
                            else FT = cossin[1].clone();
                            break;
                        case 3:

                            if(convAct.isChecked()) FT = convolutionIm;
                            else FT = signalBim.clone();
                            break;
                        default:

                            if(convAct.isChecked()) FT = convolution;
                            else FT = cossin[0].clone();
                            break;
                    }
                    */
                        fft.ifft(convolution, convolutionIm);


                        CubicInterpolation1d cubicInterpolation1d =
                                new CubicInterpolation1d();
                        double y_interp[] = cubicInterpolation1d.interpolate(convolution, INTERP_RATE);
                        double x_interp[] = new double[convolution.length * INTERP_RATE];
                        for (int j = 0; j < y_interp.length; j++) {
                            x_interp[j] = y_interp[j] / (double) INTERP_RATE;
                        }
                        final double[] FT2 = x_interp;


                        //rearrange signal so lag 0 is in the middle

                        double[] tmp = new double[FT2.length / 2];
                        int con = 0;
                        for (int i = FT2.length / 2; i < FT2.length; i++) tmp[con++] = FT2[i];
                        con = 0;
                        for (int i = FT2.length / 2; i < FT2.length; i++) FT2[i] = FT2[con++];
                        for (int i = 0; i < FT2.length / 2; i++) FT2[i] = tmp[i];


                        //final double[][] invTrasf = fft.ifft(convolution, convolutionIm);
                        //final double[] conv = invTrasf[0];
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                for (int i = 0; i < FT2.length; i += (MAXCOLLECT + 6 + SAMPLE_RATE / 8000 - 1)) {
                                    mGraphView3.addDataPoint((float) FT2[i] / 1000000 + MAXFT / 2);
                                }
                            }
                        });

                        MSG = mergeArrayAndGetBytes(cumulativeSignalA, cumulativeSignalB);
                        SEND_ACTIVE = true;

                        cumulativeSignalA = new double[0];
                        cumulativeSignalB = new double[0];

                        //find the lag at which the corr is max
                        lagCollector.add(findMaxLag(FT2, 1f / (SAMPLE_RATE * INTERP_RATE)));


                        if (samplesToPrint >= TOTAL_SAMPLES) {

                            final double lagg = meanLag(lagCollector) * 1000;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    lag.setText(String.format("Mean lag " + "%.2f" + " ms", lagg));
                                }
                            });

                            //for(int i=0;i<lagCollector.size();i++) MSG+="a"+"\n";
                            //MSG = lagg;
                            //SEND_ACTIVE = true;
                            //Log.d("MSG=", "" + MSG);

                            lagCollector = new Vector<>();
                            samplesToPrint = 0;
                        }
                    }

            }catch(Exception e){Log.w("Reading Warning",e.toString());}

                }


        }

        public void myCancel(boolean stop){
            this.STOP = stop;
        }
    }


    public byte[] mergeArrayAndGetBytes(double[] x,double[] y ){
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
        Log.d("Value",""+x[0]);

        // would be faster if i pass only integers coming from the PCM

        for(int i = 0;i<x.length;i++){
            //merged[2 * i] = x[i];
            //merged[2 * i + 1] = y[i];

            tmpx = toByteArray(x[i]);
            tmpy = toByteArray(y[i]);
            for(int j = 0; j < 8; j++){ // 8 cause i know it's double
                merged[8 * 2 * i + j] = tmpx[j];
                merged[8 * (2 * i + 1 ) + j] = tmpy[j];
            }
        }


        return merged;
    }

    public static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }




    int globalCounter = 1;
    int globalDESIRE = 0;
    double[] cumulativeSignalA = new double[0];
    double[] cumulativeSignalB = new double[0];
    public int MAXCOLLECT = 5;

    public double[] multiplyComplex(double[] real,double[] im){
        double[] complexReturn = new double[2];
        complexReturn[0] = real[0] * real[1] - im[0] * im[1];
        complexReturn[1] = real[0] * im[1] + real[1] * im[0];
        return complexReturn;
    }



    public double findMaxLag(double[] x,double deltaT){
        int indexOfZeroLag = (x.length - 1)/2; // I take for granted the signal has an even number of samples
        double[] lags = new double[x.length];
        for(int i = 0; i <=indexOfZeroLag; i++) {
            lags[indexOfZeroLag-i] = - deltaT * i;
            lags[indexOfZeroLag+i+1] = deltaT * ( i + 1 );
        }
        int index=0;
        for(int i = 1;i<x.length;i++) if(x[i]>x[index]) index = i;
        return lags[index];
    }

    public double meanLag(Vector<Double> x){
        double mean=0;
        for(int i = 0;i < x.size(); i++)mean+=x.get(i);
        return mean/x.size();
    }

    public void connectFunction()
    {
        Toast.makeText(this, "connect function", Toast.LENGTH_SHORT).show();
        if( portIndex + 1 > DevCount)  portIndex = 0;


        if( currentPortIndex == portIndex
                && ftDev != null
                && true == ftDev.isOpen() )
        {
            Toast.makeText(global_context,"Port("+portIndex+") is already opened.", Toast.LENGTH_SHORT).show();
            return;
        }

        if(true == bReadTheadEnable)
        {
            bReadTheadEnable = false;
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException e) {e.printStackTrace();}
        }

        if(null == ftDev)
        {
            ftDev = ftD2xx.openByIndex(global_context, portIndex);
        }
        else
        {
            ftDev = ftD2xx.openByIndex(global_context, portIndex);
        }
        uart_configured = false;

        if(ftDev == null)
        {
            Toast.makeText(global_context, "Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG);
            return;
        }

        if (true == ftDev.isOpen())
        {
            currentPortIndex = portIndex;
            Toast.makeText(global_context, "open device port(" + portIndex + ") OK", Toast.LENGTH_SHORT).show();

            if(false == bReadTheadEnable)
            {
                readThread = new ReadThread(handler);
                readThread.start();
                handlerThread = new HandlerThread(handler);
                handlerThread.start();
            }
        }
        else
        {
            Toast.makeText(global_context, "Open port(" + portIndex + ") NG!", Toast.LENGTH_LONG);
        }
    }

    class SendMessage extends Thread
    {


        public void run()
        {
            while(!STOP_SENDING){
                if(SEND_ACTIVE){

                    try {

                        byte[] compressed = compress(MSG);
                        //byte[] decomopressed = decompress(compressed);
                        //new MyClientTask(STATIC_IP,PORT,compressed).execute();
                        //new MyClientThread(STATIC_IP,PORT,compressed).start();
                            new UDPThread(STATIC_IP, PORT, compressed).start();

                    }catch(Exception e){Log.e("COMPRESSION",e.toString());
                    }
                    SEND_ACTIVE=false;
                }
            }
            }

    }

    boolean aaa = true;



    class ReadThread extends Thread
    {
        final int USB_DATA_BUFFER = 8192;

        Handler mHandler;
        ReadThread(Handler h)
        {
            mHandler = h;
            this.setPriority(MAX_PRIORITY);
        }

        public void run()
        {

            byte[] usbdata = new byte[USB_DATA_BUFFER];
            int readcount = 0;
            int iWriteIndex = 0;
            bReadTheadEnable = true;
            Log.d("************","<<<<<<<<<<<<STARTED>>>>>>>>>>>");
            while (true == bReadTheadEnable)
            {


                readcount = ftDev.getQueueStatus(); // retrive number of bits ready to read...
                if (readcount > 0)
                {
                    if(readcount > USB_DATA_BUFFER)
                    {
                        readcount = USB_DATA_BUFFER;
                    }
                    ftDev.read(usbdata, readcount); // read in


                        totalReceiveDataBytes += readcount;

                        for (int count = 0; count < readcount; count++)
                        {
                            readDataBuffer[iWriteIndex] = usbdata[count];
                            iWriteIndex++;
                            iWriteIndex %= MAX_NUM_BYTES;
                        }


                        if (iWriteIndex >= iReadIndex)
                        {
                            iTotalBytes = iWriteIndex - iReadIndex;
                        }
                        else
                        {
                            iTotalBytes = (MAX_NUM_BYTES - iReadIndex) + iWriteIndex;
                        }
                }
            }
        }
    }



    // Update UI content
    class HandlerThread extends Thread
    {
        Handler mHandler;

        HandlerThread(Handler h)
        {
            mHandler = h;
        }

        public void run()
        {
            byte status;
            Message msg;

            while (true)
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {e.printStackTrace();}

                if(true == bContentFormatHex) // consume input data at hex content format
                {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);
                }
                else if(MODE_GENERAL_UART == transferMode)
                {
                    status = readData(UI_READ_BUFFER_SIZE, readBuffer);

                    if (0x00 == status)
                    {

                        // save data to file
                        if(true == WriteFileThread_start && buf_save != null)
                        {
                            try
                            {
                                buf_save.write(readBuffer, 0, globalCount);
                            }
                            catch (IOException e){e.printStackTrace();}
                        }

                        globalCount = 0;
                    }

                }
            }
        }
    }

    byte readData(int numBytes, byte[] buffer)
    {
        byte intstatus = 0x00; /* success by default */

		/* should be at least one byte to read */
        if ((numBytes < 1) || (0 == iTotalBytes))
        {
            actualNumBytes = 0;
            intstatus = 0x01;
            return intstatus;
        }

        if (numBytes > iTotalBytes)
        {
            numBytes = iTotalBytes;
        }

		/* update the number of bytes available */
        iTotalBytes -= numBytes;
        actualNumBytes = numBytes;

		/* copy to the user buffer */
        byte[] minibuf=null;

        String tmp = "-";
        int[] READ = new int[numBytes]; //just cause I know every byte is a measure

        for (int count = 0; count < numBytes ; count++)
        {
            READ[count] = (readDataBuffer[iReadIndex] & 0xFF);
            tmp = (READ[count]+"\n");


            minibuf = tmp.getBytes();

            for(int i = 0;i<minibuf.length;i++)
                buffer[globalCount+i] = minibuf[i];

            globalCount+=minibuf.length;
            //buffer[globalCount+1]=("\n").getBytes()[0];
            //globalCount++;

            //buffer[count]=((readDataBuffer[iReadIndex] & 0xFF)+"").getBytes()[0];

            iReadIndex++;
            iReadIndex %= MAX_NUM_BYTES;
        }
        final int[] t = READ.clone();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myLog.setText("" + t.length);
                for(int i = 0;i<t.length;i+=50) {
                    mGraphView4.addDataPoint(t[i]);
                }
            }
        });





        if(minibuf!=null) rectData = tmp;

        return intstatus;
    }

    public int globalCount = 0;
    String rectData = "";


    String savedFromBefore = "";
    boolean waitForData = false;
    boolean moreData = false;
    boolean wasLastNumeric = false;
    boolean isFirstNumeric = false;
    long lastTime=0;

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

    final Handler handler = new Handler()
    {
        public void handleMessage(Message msg)

        {
            final Message f = msg;

            switch(msg.what)
            {
                case LAUNCH_COMMUNICATION:
                    //new MyClientTask(STATIC_IP,PORT,""+MSG).execute();
                    break;

                case UPDATE_TEXT_VIEW_CONTENT:
                    if (actualNumBytes > 0)
                    {
                        totalUpdateDataBytes += actualNumBytes;
                        for(int i=0; i<actualNumBytes; i++)
                        {
                            readBufferToChar[i] = (char)readBuffer[i];
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
                            }else if(isNumeric(arr[0]))//appendData("\n"+arr[0]);
                                mGraphView3.addDataPoint(Float.parseFloat(arr[0]));
                            for (int i = 1; i < arr.length - 1; i++)
                                //if(isNumeric(arr[i]))//appendData("\n"+arr[i]);
                                mGraphView3.addDataPoint(Float.parseFloat(arr[i]));
                            //if(isNumeric(firstByte)&&isNumeric(lastByte)) appendData("\nATT");
                            //else for (int i = 0; i < arr.length - 1; i++)
                            //  mGraphView3.addDataPoint(Float.parseFloat(arr[i]));


                            if (isNumeric(lastByte)) wasLastNumeric = true;
                            else wasLastNumeric = false;
                            savedFromBefore = arr[arr.length - 1];
                            if(lastTime != 0) {
                                long delta =  System.currentTimeMillis() - lastTime;
                                //appendData("" + arr.length + " samples in " + delta + " ms\n");
                            }
                            lastTime = System.currentTimeMillis();
                        }catch(Exception e){

                        }
                        //Pattern p = Pattern.compile("\\d+");
                        //Matcher m = p.matcher(String.copyValueOf(readBufferToChar, 0, actualNumBytes));

                        //appendData("\n!"+(String.copyValueOf(readBufferToChar,0,1))+"!\n");
                    /*
                    try{
                        if((!(String.copyValueOf(readBufferToChar,actualNumBytes-1,1)).contains("\n"))&&
                                (!(String.copyValueOf(readBufferToChar,actualNumBytes-1,1)).isEmpty())){
                            appendData("\n!"+(String.copyValueOf(readBufferToChar,actualNumBytes-1,1))+"!\n");
                            waitForData = true;
                            savedFromBefore = arr[arr.length-1];
                        }
                    //mGraphView3.addDataPoint(Float.parseFloat(savedFromBefore+arr[0]));
                        //mGraphView3.addDataPoint(512);
                    if(!moreData) for(int i=0;i<arr.length-1;i++) mGraphView3.addDataPoint(Float.parseFloat(arr[i]));
                        else {
                        mGraphView3.addDataPoint(Float.parseFloat(arr[0]+savedFromBefore));
                        for(int i=1;i<arr.length-1;i++) mGraphView3.addDataPoint(Float.parseFloat(arr[i]));
                    }
                    if(!waitForData) {
                        mGraphView3.addDataPoint(Float.parseFloat(arr[arr.length-1]));
                        moreData = false;
                    }
                        else moreData = true;
                        //mGraphView3.addDataPoint(512);
                    //savedFromBefore = arr[arr.length-1];
                    }catch(Exception e){
                        Toast.makeText(global_context,e.toString(),Toast.LENGTH_LONG).show();
                    }
                    /*

                    while (m.find()) {
                       tApp+=m.group()+"\n";
                    }

                    */
                        //appendData(toApp);
                        //appendData("\n"+String.copyValueOf(readBufferToChar,0,1)+"  "+actualNumBytes);
                        //Toast.makeText(global_context,"->"+String.copyValueOf(readBufferToChar,actualNumBytes-1,1)+"<-",Toast.LENGTH_SHORT).show();
                        //appendData(String.copyValueOf(readBufferToChar, 0, actualNumBytes));
                        //appendData("\n!"+(String.copyValueOf(readBufferToChar,actualNumBytes-1,1))+"!\n");
                    }
                    break;



                case UPDATE_ASCII_RECEIVE_DATA_BYTES:
                {
                    String temp = currentProtocol;
                    if(totalReceiveDataBytes <= 10240)
                        temp += " Receive " + totalReceiveDataBytes + "Bytes";
                    else
                        temp += " Receive " +  new java.text.DecimalFormat("#.00").format(totalReceiveDataBytes/(double)1024) + "KBytes";

                    long tempTime = System.currentTimeMillis();
                    Double diffime = (double)(tempTime-start_time)/1000;
                    temp += " in " + diffime.toString() + " seconds";

                    //updateStatusData(temp);
                }
                break;


                default:
                    Toast.makeText(global_context,"NG CASE", Toast.LENGTH_LONG);
                    //Toast.makeText(global_context, ".", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    private int COUNTER_A = 0;
    private int COUNTER_B = 0;


    class ReadingsToCorrelate {

        private Vector<Integer[]> firstTrace;
        private Vector<Integer[]> secondTrace;

    }


    public double[] createCos(int n) {
        int MAX = OMEGA;

        double[] cos = new double[n];


        for (int i = 0; i < n; i++) {
            double f = i;

            cos[i] = Math.cos(MAX*Math.PI * f / n);
            //
            //Log.d("cos",""+cos[i]);
        }



        return cos;
    }

    public double[] createSin(int n) {
        int MAX = OMEGA;

        double[] sin = new double[n];

        for (int i = 0; i < n; i++) {
            double f = i;

            sin[i] = Math.sin(MAX * Math.PI * f / n);

        }

        return sin;
    }

    public double[] createRect(int n){
        int DUTY = OMEGA;
        double[] rect = new double[n];
        for(int i=0;i<n;i++)rect[i]=1;
        if(DUTY<n) for(int i=DUTY;i<n;i++)rect[i]=0;
        return rect;
    }

    public int getMinNumberOfSamples(double minFreq,int samplingRate){
         return (int) Math.ceil(2 * ( 1f / minFreq ) * samplingRate);
    }

    private DatagramSocket mSocket;
    private int COUNT = 0;

    public class UDPThread extends Thread{

        private String address;
        private int port;
        private byte[] data;

        UDPThread(String add,int port,byte[] msg){
            address = add;
            this.port = port;
            data = msg;
        }

        public void run(){
            try {
                //byte[] receiveData = new byte[1024];
                byte[] sendData = new byte[data.length];
                InetAddress IPAddress = InetAddress.getByName(address);
                if (mSocket == null) {
                    mSocket = new DatagramSocket(null);
                    mSocket.setReuseAddress(true);
                    mSocket = new DatagramSocket();
                    mSocket.connect(IPAddress, PORT);
                    mSocket.setBroadcast(true);
                }

                    //DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    //serverSocket.receive(receivePacket);
                    //String sentence = new String(receivePacket.getData());
                    //System.out.println("RECEIVED: " + sentence);

                    //int port = receivePacket.getPort();
                    //String capitalizedSentence = sentence.toUpperCase();
                    //sendData = capitalizedSentence.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
                    mSocket.send(sendPacket);
                    connectionLost = false;

            }catch(Exception e){
                Log.e("UDP",e.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectionLost = true;
                        statusNetwork.setImageDrawable(getResources().getDrawable(R.mipmap.ic_net_off));
                    }
                });
            }finally{
                if(connectionLost) {
                    connectionLost = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusNetwork.setImageDrawable(getResources().getDrawable(R.mipmap.ic_net_on));
                        }
                    });
                }
            }
    }

    }




    public class TCPThread extends Thread {

        String dstAddress;
        int dstPort;
        String response = "";
        byte[] data;

        TCPThread(String addr, int port,byte[] data){
            Log.d("CREATING","TASK #"+COUNTER_A++);
            dstAddress = addr;
            dstPort = port;
            this.data = data;
        }

        @Override
        public void run() {

            Socket socket = null;

            try {
                Log.d("ACTION","SENDING #"+COUNTER_B++);
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


    public class MyClientTask extends AsyncTask<Void, Void, Void> {

        String dstAddress;
        int dstPort;
        String response = "";
        byte[] data;

        MyClientTask(String addr, int port,byte[] data){
            Log.d("CREATING","TASK #"+COUNTER_A++);
            dstAddress = addr;
            dstPort = port;
            this.data = data;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            Socket socket = null;

            try {
                Log.d("ACTION","SENDING #"+COUNTER_B++);
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



    public void showPeersList(){

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
                    Log.d("entered","isconnectec");
                    if (mManager != null && mChannel != null) {
                        Log.d("entered","isconnectec2");

                        mManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener() {
                            @Override
                            public void onGroupInfoAvailable(WifiP2pGroup group) {
                                if (group != null && mManager != null && mChannel != null
                                        ) {
                                    Log.d("entered","remove");
                                    mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {


                                        @Override
                                        public void onSuccess() {
                                            mWifiPeerListLadapter.notifyDataSetChanged();

                                        }

                                        @Override
                                        public void onFailure(int reason) {
                                            Log.d("inClosure", "removeGroup onFailure -" + reason);
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





    public void showTCPSettings(){
        dialog = new Dialog(MainActivity.this, 0);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.change_port_ip);
        newIP = (EditText)dialog.findViewById(R.id.newip);
        newIP.setHint(STATIC_IP);
        newPort = (EditText)dialog.findViewById(R.id.newport);
        newPort.setHint(""+PORT);
        goChanges = (Button)dialog.findViewById(R.id.gochanges);
        goChanges.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    STATIC_IP = newIP.getText().toString();
                    PORT = Integer.parseInt(newPort.getText().toString());
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
                    STATIC_IP = newIP.getText().toString();
                    if(Pattern.matches("%i.%i.%i.%i",""+STATIC_IP))
                    dialog.dismiss();
                    else throw new Exception();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Wrong format IP", Toast.LENGTH_SHORT).show();
                }
            }
        });

        soloPort=(Button)dialog.findViewById(R.id.soloport);
        soloPort.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PORT = Integer.parseInt(newPort.getText().toString());
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Wrong format Port", Toast.LENGTH_SHORT).show();
                }
            }
        });
            dialog.show();
        }




        public  byte[] compress(byte[] data) throws IOException {
            Deflater deflater = new Deflater();
            deflater.setInput(data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);

            deflater.finish();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer); // returns the generated code... index
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            byte[] output = outputStream.toByteArray();


            return output;
        }

        public  byte[] decompress(byte[] data) throws IOException, DataFormatException {
            Inflater inflater = new Inflater();
            inflater.setInput(data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
            byte[] output = outputStream.toByteArray();


            return output;
        }



}