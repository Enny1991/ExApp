package com.eneaceolini.wifip2p;

import android.content.Context;

/**
 * Created by enea on 07/10/15.
 * Project COCOHA
 */
public abstract class StopPoolThreadAdv extends Thread {

    abstract public boolean amIRunning();

    abstract public void destroyMe();

    abstract public String describeMe();
}
