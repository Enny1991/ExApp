package com.eneaceolini.exapp;

import android.app.Dialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

public class SSHConnector extends AppCompatActivity {

    private static final String TAG = "SSHConnector";
    private String usName = "Enea";
    private String password = "astro1991";
    private String host = "172.19.12.186";
    private int port  = 22;
    private TextView result,current;
    private Dialog dialog;
    EditText newUser,newHost,newPass,comand;
    private static String COMMAND;
    private static Session session;
    private static Channel channel;
    private static PrintStream commander;
    private ScrollView scrollView;
    private InputStream fromChannel;
    private InputStream dataIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sshconnector);
        result = (TextView)findViewById(R.id.result);
        current = (TextView) findViewById(R.id.current);
        comand = (EditText)findViewById(R.id.comand);
        scrollView = (ScrollView)findViewById(R.id.scrollView);
        new SSHConnection(usName, password, host, port).execute();

        comand.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(TAG, "Triggered editor action");
                    COMMAND = v.getText().toString();
                    new SSHTask().execute();
                    return true;
                }
                return false;
            }


        });





    }

    public class SSHConnection extends AsyncTask<Void, Void, String>
    {

        String username,hostname,password;
        int port;

        SSHConnection(String username,String password,String hostname,int port){
            this.username = username;
            this.password = password;
            this.hostname = hostname;
            this.port = port;

        }

        @Override
        protected String doInBackground(Void... arg0) {

            String ret = "error";

            try {
                ret = openConnection(usName, password, host, port);
            }catch(Exception e){
                Log.e(TAG,e.toString());
            }
            Log.d(TAG,ret);

            return ret;
        }

        @Override
        protected void onPostExecute(String res) {
            current.setText(res);
            super.onPostExecute(res);
        }



    }

    public class SSHTask extends AsyncTask<Void, Void, String>
    {

        String username,hostname,password;
        int port;

        SSHTask(){
        }

        @Override
        protected String doInBackground(Void... arg0) {

            String ret = "error";
            Log.d(TAG,"background...");

            try {
                ret = executeRemoteCommand2();
            }catch(Exception e){
                Log.e(TAG,e.toString());
            }
            Log.d(TAG,ret);

            return ret;
        }

        @Override
        protected void onPostExecute(String res) {
            super.onPostExecute(res);
        }



    }


    private String openConnection(
            String username,
            String password,
            String hostname,
            int port) throws Exception {
        String header = "";
        JSch jsch = new JSch();
        session = jsch.getSession(username, hostname, 22);
        session.setPassword(password);

        // Avoid asking for key confirmation
        Properties prop = new Properties();
        prop.put("StrictHostKeyChecking", "no");
        session.setConfig(prop);
        session.connect();
        Log.d(TAG, "to: " + session.getUserName());
        header += session.getUserName()
                + "@"
                + session.getHost()+": $ ";

        channel = session.openChannel("shell");

        OutputStream inputstream_for_the_channel = channel.getOutputStream();
        commander = new PrintStream(inputstream_for_the_channel, true);
        channel.setOutputStream(System.out);
        //channel.setInputStream(null);
        //fromChannel = channel.getInputStream();
        //dataIn = channel.getInputStream();
        channel.connect();

        //new ReaderTask().execute();

        // and print the response


        //new ReaderTask().execute();

        return header;



    }


    public static String executeRemoteCommand2() throws Exception {




        commander.println(COMMAND);


        return "done";
    }

    public static String executeRemoteCommand() throws Exception {


    String ret = "";
        ChannelShell mChannelShell = (ChannelShell)
                session.openChannel("shell");

        // SSH Channel
        ChannelExec channelssh = (ChannelExec)
                session.openChannel("exec");
        //channelssh.setPty(true);
        //channelssh.setPtyType("vt100");
        //channelssh.sendSignal("cd Des"+"\t");
        channelssh.setCommand("cd Desktop/");

        channelssh.setInputStream(null);


        //channel.setOutputStream(System.out);

        //FileOutputStream fos=new FileOutputStream("/tmp/stderr");
        //((ChannelExec)channel).setErrStream(fos);
        ((ChannelExec)channelssh).setErrStream(System.err);

        InputStream in=channelssh.getInputStream();

        channelssh.connect();

        byte[] tmp=new byte[1024];
        while(true){
            while(in.available()>0){
                int i=in.read(tmp, 0, 1024);
                if(i<0)break;
                //System.out.print(new String(tmp, 0, i));
                ret += new String(tmp, 0, i)+"\n";
            }
            if(channelssh.isClosed()){
                if(in.available()>0) continue;
                System.out.println("exit-status: "+channelssh.getExitStatus());
                break;
            }
            try{Thread.sleep(1000);}catch(Exception ee){}
        }
        channelssh.disconnect();
        //session.disconnect();


        // Execute command


        return ret;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sshconnector, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.action_settings:
                return true;
            case R.id.action_ssh_set:
                showSSHDialog();
                break;
        }
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    private void showSSHDialog() {
        dialog = new Dialog(SSHConnector.this, 0);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.change_ssh_set);
        newUser = (EditText)dialog.findViewById(R.id.newuser);
        newUser.setHint(usName);

        newPass = (EditText)dialog.findViewById(R.id.password);

        newHost = (EditText)dialog.findViewById(R.id.hostname);
        newHost.setHint(host);

        Button set = (Button)dialog.findViewById(R.id.gochanges);
        set.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                usName = newUser.getText().toString();
                host = newHost.getText().toString();
                password = newPass.getText().toString();
                dialog.dismiss();
            }
        });



        dialog.show();
    }


    class ReaderTask extends AsyncTask<String, Void, Void> {

        TextView mText = (TextView) findViewById(R.id.result);
        PipedOutputStream mPOut;
        PipedInputStream mPIn;
        LineNumberReader mReader;
        String RES ;


        @Override
        protected Void doInBackground(String... params) {

            byte[] buffer = new byte[2048];
            String res = "";
            try {
                while(true){
                    Log.d(TAG,"reading...");
                    Log.d(TAG,"EOF: "+ channel.isEOF());
                    int x = dataIn.read(buffer,0,2048);

                        Log.i("TAG", "Line: "+new String(buffer,0,x));

                    //publishProgress();
            }
            }catch(Exception e){
                Log.e(TAG,"error in reading output");
            }

            return null;
        }
        @Override
        protected void onProgressUpdate(Void... values) {

                result.setText(result.getText().toString() +RES);

        }
    }

}
