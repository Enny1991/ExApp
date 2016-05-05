package com.eneaceolini.ftdi;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.eneaceolini.exapp.R;
import com.eneaceolini.utility.Constants;
import com.eneaceolini.utility.MakeACopy;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.File;
import java.io.RandomAccessFile;

// For commit in the new desktop


public class FTDIActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {


    private static final String TAG = "FTDIActivity";


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    // Graphics

    // protocols & modalities
    public static D2xxManager ftD2xx = null;
    FT_Device ftDev;
    int DevCount = -1;
    int currentPortIndex = -1;
    int portIndex = -1;


    enum DeviceStatus {
        DEV_NOT_CONNECT,
        DEV_NOT_CONFIG,
        DEV_CONFIG
    }





    // handler event


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

    RandomAccessFile RAF2;
    double[] LAGS;
    int indexOfZeroLag;
    double deltaT,roofLags;

    /* Activity Methods */

    @Override
    public void onCreate(Bundle icicle) {

        global_context = this;

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
        verifyStoragePermissions(this);
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);


        try {
            File sdCardDir = Environment.getExternalStorageDirectory();
            File targetFile;
            targetFile = new File(sdCardDir.getCanonicalPath());
            File file = new File(targetFile + "/" + "stop_"+Constants.FRAME_SIZE+".txt");
            RAF2 = new RandomAccessFile(file, "rw");
            RAF2.seek(file.length());
        }catch(Exception e){}



        //menage actionBar

        /* GRAPHICS set up */



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
    /* END Activity Methods */
    }

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
        PopupMenu popup = new PopupMenu(FTDIActivity.this, menuItemView);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.context_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
    }

    public void showPopupMenuMinimumFrequency() {
        View menuItemView = findViewById(R.id.action_rate);
        PopupMenu popup = new PopupMenu(FTDIActivity.this, menuItemView);
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
                return true;
            case R.id.action_rate:

                return true;
            case R.id.action_start_ssh:

                return true;
            case R.id.action_start_loc:

                return true;
            case R.id.action_min_freq:
                return true;
            case R.id.action_p2p:
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

                //RESTART
                return true;
            case R.id.rate_16:

                return true;
            case R.id.rate_32:

                //RESTART
                return true;
            case R.id.rate_44:

                //RESTART
                return true;
            case R.id.hz_100:

                //RESTART
                return true;
            case R.id.hz_1000:

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

}