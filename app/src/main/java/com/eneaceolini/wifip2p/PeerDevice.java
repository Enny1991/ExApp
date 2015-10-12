package com.eneaceolini.wifip2p;

import java.net.InetAddress;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class PeerDevice{

    private InetAddress address,ownerAddress;
    private String name,info;
    private double delay;
    private double angle;
    private boolean delayAvailable,pingAvailable;
    private int angleAvailable;

    public PeerDevice(InetAddress  address, String name){
        this.address = address;
        this.name = name;
        this.setPingAvailable(true);
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

    public void setDelay(double v) {
        this.delay = v;
        setDelayAvailable(true);
    }

    public double getDelay(){
        return this.delay;
    }

    public void setAngle(double v) {
        this.angle = v;
        setAngleAvailable(Requests.COMP_RECEIVED);
    }

    public double getAngle(){
        return this.angle;
    }

    public void setDelayAvailable(boolean state){this.delayAvailable = state;}

    public void setAngleAvailable(int state){this.angleAvailable = state;}

    public boolean isDelayAvailable(){return delayAvailable;}

    public int isAngleAvailable(){return angleAvailable;}

    public void setPingAvailable(boolean state){this.pingAvailable = state;}

    public boolean isPingAvailable(){return pingAvailable;}
}

