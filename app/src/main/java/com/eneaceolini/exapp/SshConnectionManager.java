package com.eneaceolini.exapp;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Enea on 28/08/15.
 * Project COCOHA
 */
public class SshConnectionManager extends AppCompatActivity {

    private static Session session;
    private static ChannelShell channel;
    private static String username = "enea";
    private static String password = "songoldon";
    private static String hostname = "77.109.166.105";
    private static final String TAG = "SSHConnector";
    private int port  = 22;
    private TextView result,current;
    private Dialog dialog;
    EditText newUser,newHost,newPass,comand;
    private static String COMMAND;
    private static PrintStream commander;
    private ScrollView scrollView;
    private InputStream fromChannel;
    private InputStream dataIn;


    private static Session getSession(){
        if(session == null || !session.isConnected()){
            session = connect(hostname,username,password);
        }
        return session;
    }

    private static Channel getChannel(){
        if(channel == null || !channel.isConnected()){
            try{
                channel = (ChannelShell)getSession().openChannel("shell");
                channel.connect();

            }catch(Exception e){
                System.out.println("Error while opening channel: "+ e);
            }
        }
        return channel;
    }

    private static Session connect(String hostname, String username, String password){

        JSch jSch = new JSch();

        try {

            session = jSch.getSession(username, hostname, 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(password);

            System.out.println("Connecting SSH to " + hostname + " - Please wait for few seconds... ");
            session.connect();
            System.out.println("Connected!");
        }catch(Exception e){
            System.out.println("An error occurred while connecting to "+hostname+": "+e);
        }

        return session;

    }

    private static void executeCommands(List<String> commands){

        try{
            Channel channel=getChannel();

            System.out.println("Sending commands...");
            sendCommands(channel, commands);

            readChannelOutput(channel);
            System.out.println("Finished sending commands!");

        }catch(Exception e){
            System.out.println("An error ocurred during executeCommands: "+e);
        }
    }

    public class Listen extends AsyncTask<Void, Void, Void>
    {

        List<String> commands;

        Listen(List<String> cmd){
            commands = cmd;
        }


        @Override
        protected Void doInBackground(Void... arg0) {
            try{
                Channel channel=getChannel();

                System.out.println("Sending commands...");
                sendCommands(channel, commands);

                readChannelOutput(channel);
                System.out.println("Finished sending commands!");

            }catch(Exception e){
                System.out.println("An error occurred during executeCommands: "+e);
            }
            return null;

        }

        @Override
        protected void onPostExecute(Void res) {

            super.onPostExecute(res);
        }



    }

    private static void sendCommands(Channel channel, List<String> commands){

        try{
            PrintStream out = new PrintStream(channel.getOutputStream());

            out.println("#!/bin/bash");
            for(String command : commands){
                out.println(command);
            }
            //out.println("exit");

            out.flush();
        }catch(Exception e){
            System.out.println("Error while sending commands: "+ e);
        }

    }

    private static void readChannelOutput(Channel channel){

        byte[] buffer = new byte[1024];

        try{
            InputStream in = channel.getInputStream();
            String line = "";
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    System.out.print(new String(buffer, 0, i));//It is printing the response to console
                }
                System.out.println("done");

                //channel.close();  // this closes the jsch channel

                if (channel.isClosed()) {
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
                try{Thread.sleep(1000);}catch(Exception ee){
                    ee.printStackTrace();
                }
            }
        }catch(Exception e){
            System.out.println("Error while reading channel output: "+ e);
        }

    }

    public static void close(){
        if (channel != null) channel.disconnect();
        if (session != null) session.disconnect();
        System.out.println("Disconnected channel and session");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sshconnector);
        result = (TextView)findViewById(R.id.result);
        current = (TextView) findViewById(R.id.current);
        comand = (EditText)findViewById(R.id.comand);
        scrollView = (ScrollView)findViewById(R.id.scrollView);


        comand.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //Log.d(TAG, "Triggered editor action");
                    List<String> commands = new ArrayList<>();
                    commands.add(v.getText().toString());
                    new Listen(commands).execute();
                    //close();
                    return true;
                }
                return false;
            }


        });

    }


}