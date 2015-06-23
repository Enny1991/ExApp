package com.eneaceolini.exapp;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Enea on 18/06/15.
 */
public class UDPCommunicationManager {


    private static final String TAG = "UDPCommunicationManager";
    private static final int KEEP_ALIVE_TIME = 500;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT;
    private static final int CORE_POOL_SIZE = 10;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private final BlockingQueue<Runnable> mUDPStreamQueue;
    private final BlockingQueue<Runnable> mUDPDirectQueue;
    private final BlockingQueue<Runnable> mUDPLagsQueue;
    private final BlockingQueue<Runnable> mAnalysisQueue;
    private final ThreadPoolExecutor mUDPStreamThreadPool;
    private final ThreadPoolExecutor mUDPDirectThreadPool;
    private final ThreadPoolExecutor mUDPLagsThreadPool;
    private final ThreadPoolExecutor mAnalysisThreadPool;
    private static UDPCommunicationManager sInstance = null;



    static{
        KEEP_ALIVE_TIME_UNIT = TimeUnit.MILLISECONDS;
        sInstance = new UDPCommunicationManager();
    }

    private UDPCommunicationManager() {

        mUDPStreamQueue = new LinkedBlockingQueue<Runnable>();
        mUDPDirectQueue = new LinkedBlockingQueue<Runnable>();
        mUDPLagsQueue = new LinkedBlockingQueue<Runnable>();
        mAnalysisQueue = new LinkedBlockingQueue<Runnable>();

        mUDPStreamThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,mUDPStreamQueue);

        mUDPDirectThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,mUDPDirectQueue);

        mUDPLagsThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,mUDPLagsQueue);

        mAnalysisThreadPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT,mAnalysisQueue);
    }

    public static UDPCommunicationManager getInstance() {

        return sInstance;
    }

    static public void startSending(MainActivity.UDPRunnable a){

         sInstance.mUDPStreamThreadPool.execute(a);
        //Log.d(TAG,"executing packet stream");

    }

    static public void startSending(MainActivity.ReadTh a){

        sInstance.mUDPStreamThreadPool.execute(a);
        //Log.d(TAG, "executing analysis");
    }

    static public void startSending(String add,int port,byte[] data,int mode){

        //sInstance.mUDPStreamThreadPool.execute(new UDPRunnableLags(add,port,data));
        //Log.d(TAG, "executing analysis");
    }


    static public void startSending(UDPRunnableLags a){

        sInstance.mUDPStreamThreadPool.execute(a);
        //Log.d(TAG, "executing packet lags");
    }

    static public void startSending(MainActivity.UDPRunnableDirect a){

        sInstance.mUDPStreamThreadPool.execute(a);
        //Log.d(TAG, "executing packet direct");

    }




}
