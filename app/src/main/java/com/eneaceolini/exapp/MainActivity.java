package com.eneaceolini.exapp;


        import android.app.ActionBar;
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
        import android.text.SpannableString;
        import android.text.style.ForegroundColorSpan;
        import android.view.Menu;
        import android.widget.ArrayAdapter;
        import android.widget.EditText;
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
        import android.widget.RelativeLayout;
        import android.widget.ScrollView;
        import android.widget.Spinner;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.ftdi.j2xx.D2xxManager;
        import com.ftdi.j2xx.FT_Device;

        import java.io.BufferedOutputStream;
        import java.io.BufferedReader;
        import java.io.File;
        import java.io.FileInputStream;
        import java.io.FileOutputStream;
        import java.io.FileReader;
        import java.io.FileWriter;
        import java.io.IOException;
        import java.io.StringReader;


public class MainActivity extends Activity
{
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName = null;

    private MediaRecorder mRecorder = null;

    private MediaPlayer   mPlayer = null;

    int buffersize;
    AudioRecord m_record;
    int SAMPLE_RATE = 8000;
    float quantStep = 2^16/5;


    short[]   buffer  ;


    Integer[] mSensorValue = new Integer[1];
    ReadTask myReadTask;




    // Graphics

    GraphView mGraphView;
    GraphView mGraphView2;
    GraphView mGraphView3;
    TextView myLog;
    TextView myLog2;
    TextView myLog3;

    // protocols & modalities
    public static D2xxManager ftD2xx = null;
    FT_Device ftDev;
    int DevCount = -1;
    int currentPortIndex = -1;
    int portIndex = -1;

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
    final int UPDATE_SEND_FILE_STATUS = 1;
    final int UPDATE_SEND_FILE_DONE = 2;
    final int ACT_SELECT_SAVED_FILE_NAME = 3;
    final int ACT_SELECT_SAVED_FILE_FOLDER = 4;
    final int ACT_SAVED_FILE_NAME_CREATED = 5;
    final int ACT_SELECT_SEND_FILE_NAME = 6;
    final int MSG_SELECT_FOLDER_NOT_FILE = 7;
    final int MSG_XMODEM_SEND_FILE_TIMEOUT = 8;
    final int UPDATE_MODEM_RECEIVE_DATA = 9;
    final int UPDATE_MODEM_RECEIVE_DATA_BYTES = 10;
    final int UPDATE_MODEM_RECEIVE_DONE = 11;
    final int MSG_MODEM_RECEIVE_PACKET_TIMEOUT = 12;
    final int ACT_MODEM_SELECT_SAVED_FILE_FOLDER = 13;
    final int MSG_MODEM_OPEN_SAVE_FILE_FAIL = 14;
    final int MSG_YMODEM_PARSE_FIRST_PACKET_FAIL = 15;
    final int MSG_FORCE_STOP_SEND_FILE = 16;
    final int UPDATE_ASCII_RECEIVE_DATA_BYTES = 17;
    final int UPDATE_ASCII_RECEIVE_DATA_DONE = 18;
    final int MSG_FORCE_STOP_SAVE_TO_FILE = 19;
    final int UPDATE_ZMODEM_STATE_INFO = 20;
    final int ACT_ZMODEM_AUTO_START_RECEIVE = 21;

    final int MSG_SPECIAL_INFO = 98;
    final int MSG_UNHANDLED_CASE = 99;

    final byte XON = 0x11;    /* Resume transmission */
    final byte XOFF = 0x13;    /* Pause transmission */

    // strings of file transfer protocols
    final String[] protocolItems = {"ASCII","XModem-CheckSum","XModem-CRC","XModem-1KCRC","YModem","ZModem"};
    String currentProtocol;

    final int MODE_GENERAL_UART = 0;
    final int MODE_X_MODEM_CHECKSUM_RECEIVE = 1;
    final int MODE_X_MODEM_CHECKSUM_SEND = 2;
    final int MODE_X_MODEM_CRC_RECEIVE = 3;
    final int MODE_X_MODEM_CRC_SEND = 4;
    final int MODE_X_MODEM_1K_CRC_RECEIVE = 5;
    final int MODE_X_MODEM_1K_CRC_SEND = 6;
    final int MODE_Y_MODEM_1K_CRC_RECEIVE = 7;
    final int MODE_Y_MODEM_1K_CRC_SEND = 8;
    final int MODE_Z_MODEM_RECEIVE = 9;
    final int MODE_Z_MODEM_SEND = 10;
    final int MODE_SAVE_CONTENT_DATA = 11;

    int transferMode = MODE_GENERAL_UART;
    int tempTransferMode = MODE_GENERAL_UART;

    // X, Y, Z modem - UART MODE: Asynchronous�B8 data��bits�Bno parity�Bone stop��bit
    // X modem + //
    final int PACTET_SIZE_XMODEM_CHECKSUM = 132; // SOH,pkt,~ptk,128data,checksum
    final int PACTET_SIZE_XMODEM_CRC = 133;  	 // SOH,pkt,~ptk,128data,CRC-H,CRC-L
    final int PACTET_SIZE_XMODEM_1K_CRC = 1029;	 // STX,pkt,~ptk,1024data,CRC-H,CRC-L

    final byte SOH = 1;    /* Start Of Header */
    final byte STX = 2;    /* Start Of Header 1K */
    final byte EOT = 4;    /* End Of Transmission */
    final byte ACK = 6;    /* ACKnowlege */
    final byte NAK = 0x15; /* Negative AcKnowlege */
    final byte CAN = 0x18; /* Cancel */
    final byte CHAR_C = 0x43; /* Character 'C' */
    final byte CHAR_G = 0x47; /* Character 'G' */

    final int DATA_SIZE_128 = 128;
    final int DATA_SIZE_256 = 256;
    final int DATA_SIZE_512 = 512;
    final int DATA_SIZE_1K = 1024;

    final int MODEM_BUFFER_SIZE = 2048;
    int[] modemReceiveDataBytes;
    byte[] modemDataBuffer;
    byte[] zmDataBuffer;
    byte receivedPacketNumber = 1;

    boolean bModemGetNak = false;
    boolean bModemGetAck = false;
    boolean bModemGetCharC = false;
    boolean bModemGetCharG = false;



    final byte ZPAD = 0x2A; // '*' 052 Padding character begins frames
    final byte ZDLE = 0x18;


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

        global_context = this;
        mGraphView = (GraphView) findViewById(R.id.graph);
        mGraphView.setMaxValue(65536);

        mGraphView2 = (GraphView) findViewById(R.id.graph2);
        mGraphView2.setMaxValue(65536);

        mGraphView3 = (GraphView) findViewById(R.id.graph3);
        mGraphView3.setMaxValue(1024);

        myLog = (TextView)findViewById(R.id.textView3);
        myLog2 = (TextView)findViewById(R.id.textView4);
        myLog3 = (TextView)findViewById(R.id.textView5);

        /* allocate buffer */
        writeBuffer = new byte[512];
        readBuffer = new byte[UI_READ_BUFFER_SIZE];
        readBufferToChar = new char[UI_READ_BUFFER_SIZE];
        readDataBuffer = new byte[MAX_NUM_BYTES];
        actualNumBytes = 0;
        baudRate = 115200;
        stopBit = 1;
        dataBit = 8;
        parity = 0;
        flowControl = 1;
        portIndex = 0;

        Button start = (Button)findViewById(R.id.start);

        start.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                myReadTask = new ReadTask();
                myReadTask.execute(mSensorValue);
                Log.d("pressed","START");
            }
        });


        Button stop = (Button)findViewById(R.id.stop);
        stop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("pressed","Stop");
                myReadTask.myCancel(true);
                myReadTask.cancel(true);
                m_record.stop();
                //myAsync = null;
                //stop =true;
            }
        });


    }


    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
    }





    class ReadTask extends AsyncTask<Integer, Integer, Integer> {
        boolean STOP = false;
        @Override
        protected Integer doInBackground(Integer... sensorValue) {
            //return (sensorValue[0]);  // This goes to result

            try {
                buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
                buffer = new short[buffersize];

                m_record = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                        SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, buffersize * 1);
            } catch (Throwable t) {
                Log.e("Error", "Initializing Audio Record and Play objects Failed "+t.getLocalizedMessage());
            }

            m_record.startRecording();

            while(!STOP) {
                final int n = m_record.read(buffer, 0, buffersize);
                //notifyNewBuffer(n);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0;i<n;i++) {
                            mGraphView.addDataPoint(buffer[i]+32678);
                            mGraphView2.addDataPoint(buffer[i+1]+32678);
                            i++;
                        }
                    }
                });

            }
            return 1;

        }

        // Called when there's a status to be updated
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // Not used in this case
        }

        // Called once the background activity has completed
        @Override
        protected void onPostExecute(Integer result) {

        }

        public void myCancel(boolean stop){
            this.STOP = stop;
        }
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
                //try
                //{
                //    Thread.sleep(10);
                //}
                //catch (InterruptedException e) {e.printStackTrace();}


                //while(iTotalBytes > (MAX_NUM_BYTES - (USB_DATA_BUFFER+1)))
                //{
                  //  try
                    //{
                      //  Thread.sleep(50);
                    //}
                    //catch (InterruptedException e) {e.printStackTrace();}
                //}

                readcount = ftDev.getQueueStatus(); // retrive number of bits ready to read...
                if (readcount > 0)
                {
                    if(readcount > USB_DATA_BUFFER)
                    {
                        readcount = USB_DATA_BUFFER;
                    }
                    ftDev.read(usbdata, readcount); // read in


                    if( (MODE_X_MODEM_CHECKSUM_SEND == transferMode)
                            ||(MODE_X_MODEM_CRC_SEND == transferMode)
                            ||(MODE_X_MODEM_1K_CRC_SEND == transferMode) )
                    {
                        for (int i = 0; i < readcount; i++)
                        {
                            modemDataBuffer[i] = usbdata[i];

                        }

                        if(NAK == modemDataBuffer[0])
                        {

                            bModemGetNak = true;
                        }
                        else if(ACK == modemDataBuffer[0])
                        {

                            bModemGetAck = true;
                        }
                        else if(CHAR_C == modemDataBuffer[0])
                        {

                            bModemGetCharC = true;
                        }
                        if(CHAR_G == modemDataBuffer[0])
                        {

                            bModemGetCharG = true;
                        }
                    }
                    else
                    {

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

                        if( (MODE_X_MODEM_CHECKSUM_RECEIVE == transferMode)
                                || (MODE_X_MODEM_CRC_RECEIVE == transferMode)
                                || (MODE_X_MODEM_1K_CRC_RECEIVE == transferMode)
                                || (MODE_Y_MODEM_1K_CRC_RECEIVE == transferMode)
                                || (MODE_Z_MODEM_RECEIVE == transferMode)
                                || (MODE_Z_MODEM_SEND == transferMode) )
                        {
                            modemReceiveDataBytes[0] += readcount;

                        }
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
                                buf_save.write(readBuffer, 0, actualNumBytes);
                            }
                            catch (IOException e){e.printStackTrace();}
                        }

                        msg = mHandler.obtainMessage(UPDATE_TEXT_VIEW_CONTENT);
                        mHandler.sendMessage(msg);
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
        for (int count = 0; count < numBytes; count++)
        {
            buffer[count] = readDataBuffer[iReadIndex];
            iReadIndex++;
            iReadIndex %= MAX_NUM_BYTES;
        }

        return intstatus;
    }

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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myLog2.setText("---> messagge # "+ f.what+"<---");
                }
            });
            switch(msg.what)
            {
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



    // add data to UI(@+id/ReadValues)
    void appendData(String data)
    {

            Log.d("first","LOG");
            final String d = data;
            StringReader reader = new StringReader(d);
            BufferedReader bufRead = new BufferedReader(reader);
            String myLine = null;
            final String actual = myLog3.getText().toString();
            try {
                while ((myLine = bufRead.readLine()) != null) {
                    final String line = myLine;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mGraphView3.addDataPoint(Float.parseFloat(line));
                            //myLog2.setText("-->" + line);
                            myLog3.setText(actual+"\n"+d);
                        }
                    });
                }
            }catch(Exception e){}
    }



}