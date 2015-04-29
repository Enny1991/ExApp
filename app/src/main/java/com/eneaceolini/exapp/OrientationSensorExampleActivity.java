package com.eneaceolini.exapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.androidplot.Plot;
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.*;
import java.text.DecimalFormat;
import java.util.Arrays;


public class OrientationSensorExampleActivity extends Activity {

    private static final int HISTORY_SIZE = 1000;            // number of points to plot in history
    private SensorManager sensorMgr = null;
    private Sensor orSensor = null;

    //private XYPlot aprLevelsPlot = null;
    private XYPlot aprHistoryPlot = null;
    private XYPlot aprHistoryPlot2 = null;

    private CheckBox hwAcceleratedCb;
    private CheckBox showFpsCb;
    //private SimpleXYSeries aprLevelsSeries = null;
    private SimpleXYSeries aLvlSeries;
    private SimpleXYSeries pLvlSeries;
    private SimpleXYSeries rLvlSeries;
    private SimpleXYSeries azimuthHistorySeries = null;
    private SimpleXYSeries pitchHistorySeries = null;
    private SimpleXYSeries rollHistorySeries = null;

    private Redrawer redrawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orientation_sensor_example);

        // setup the APR Levels plot:
        //aprLevelsPlot = (XYPlot) findViewById(R.id.aprLevelsPlot);
        //aprLevelsPlot.setDomainBoundaries(-1, 1, BoundaryMode.FIXED);
        //aprLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);

        aLvlSeries = new SimpleXYSeries("A");
        pLvlSeries = new SimpleXYSeries("P");
        rLvlSeries = new SimpleXYSeries("R");

        /*
        aprLevelsPlot.addSeries(aLvlSeries,
                new BarFormatter(Color.rgb(0, 200, 0), Color.rgb(0, 80, 0)));
        aprLevelsPlot.addSeries(pLvlSeries,
                new BarFormatter(Color.rgb(200, 0, 0), Color.rgb(0, 80, 0)));
        aprLevelsPlot.addSeries(rLvlSeries,
                new BarFormatter(Color.rgb(0, 0, 200), Color.rgb(0, 80, 0)));

        aprLevelsPlot.setDomainStepValue(3);
        aprLevelsPlot.setTicksPerRangeLabel(3);

        // per the android documentation, the minimum and maximum readings we can get from
        // any of the orientation sensors is -180 and 359 respectively so we will fix our plot's
        // boundaries to those values.  If we did not do this, the plot would auto-range which
        // can be visually confusing in the case of dynamic plots.
        aprLevelsPlot.setRangeBoundaries(-32678, 32678, BoundaryMode.FIXED);

        // update our domain and range axis labels:
        aprLevelsPlot.setDomainLabel("");
        aprLevelsPlot.getDomainLabelWidget().pack();
        aprLevelsPlot.setRangeLabel("Angle (Degs)");
        aprLevelsPlot.getRangeLabelWidget().pack();
        aprLevelsPlot.setGridPadding(15, 0, 15, 0);
        aprLevelsPlot.setRangeValueFormat(new DecimalFormat("#"));
        */
        // setup the APR History plot:
        aprHistoryPlot = (XYPlot) findViewById(R.id.aprHistoryPlot);
        aprHistoryPlot2 = (XYPlot) findViewById(R.id.aprHistoryPlot2);

        azimuthHistorySeries = new SimpleXYSeries("Az.");
        azimuthHistorySeries.useImplicitXVals();
        pitchHistorySeries = new SimpleXYSeries("Pitch");
        pitchHistorySeries.useImplicitXVals();
        rollHistorySeries = new SimpleXYSeries("Roll");
        rollHistorySeries.useImplicitXVals();

        aprHistoryPlot.setRangeBoundaries(-32678, +32678, BoundaryMode.FIXED);
        aprHistoryPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        //aprHistoryPlot.addSeries(azimuthHistorySeries,
          //      new LineAndPointFormatter(
            //            Color.rgb(100, 100, 200), null, null, null));
        aprHistoryPlot.addSeries(pitchHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 200, 100), null, null, null));
        //aprHistoryPlot.addSeries(rollHistorySeries,
          //      new LineAndPointFormatter(
            //            Color.rgb(200, 100, 100), null, null, null));
        aprHistoryPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        aprHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        aprHistoryPlot.setTicksPerRangeLabel(3);
        aprHistoryPlot.setDomainLabel("Sample Index");
        aprHistoryPlot.getDomainLabelWidget().pack();
        aprHistoryPlot.setRangeLabel("Angle (Degs)");
        aprHistoryPlot.getRangeLabelWidget().pack();

        aprHistoryPlot.setRangeValueFormat(new DecimalFormat("#"));
        aprHistoryPlot.setDomainValueFormat(new DecimalFormat("#"));

        //2
        aprHistoryPlot2.setRangeBoundaries(-32678, +32678, BoundaryMode.FIXED);
        aprHistoryPlot2.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        aprHistoryPlot2.addSeries(azimuthHistorySeries,
                new LineAndPointFormatter(
                        Color.rgb(100, 100, 200), null, null, null));
        //aprHistoryPlot2.addSeries(pitchHistorySeries,
          //      new LineAndPointFormatter(
            //            Color.rgb(100, 200, 100), null, null, null));
        //aprHistoryPlot2.addSeries(rollHistorySeries,
          //      new LineAndPointFormatter(
            //            Color.rgb(200, 100, 100), null, null, null));
        aprHistoryPlot2.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        aprHistoryPlot2.setDomainStepValue(HISTORY_SIZE/10);
        aprHistoryPlot2.setTicksPerRangeLabel(3);
        aprHistoryPlot2.setDomainLabel("Sample Index");
        aprHistoryPlot2.getDomainLabelWidget().pack();
        aprHistoryPlot2.setRangeLabel("Angle (Degs)");
        aprHistoryPlot2.getRangeLabelWidget().pack();

        aprHistoryPlot2.setRangeValueFormat(new DecimalFormat("#"));
        aprHistoryPlot2.setDomainValueFormat(new DecimalFormat("#"));
        //

        // setup checkboxes:
        hwAcceleratedCb = (CheckBox) findViewById(R.id.hwAccelerationCb);
        final PlotStatistics levelStats = new PlotStatistics(500, false);
        final PlotStatistics histStats = new PlotStatistics(500, false);

        //aprLevelsPlot.addListener(levelStats);
        //aprHistoryPlot.addListener(histStats);
        hwAcceleratedCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    //aprLevelsPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                } else {
                    //aprLevelsPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    aprHistoryPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            }
        });

        showFpsCb = (CheckBox) findViewById(R.id.showFpsCb);
        showFpsCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                levelStats.setAnnotatePlotEnabled(b);
                histStats.setAnnotatePlotEnabled(b);
            }
        });

        // get a ref to the BarRenderer so we can make some changes to it:
        //BarRenderer barRenderer = (BarRenderer) aprLevelsPlot.getRenderer(BarRenderer.class);
        //if(barRenderer != null) {
            // make our bars a little thicker than the default so they can be seen better:
            //barRenderer.setBarWidth(25);
        //}

        // register for orientation sensor events:


        //sensorMgr.registerListener(this, orSensor, SensorManager.SENSOR_DELAY_UI);

        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{aprHistoryPlot,aprHistoryPlot2}),
                1000, false);

        myReadTask = new ReadTask();
        myReadTask.execute(mSensorValue);
        Log.d("DONE","onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        redrawer.start();
    }

    @Override
    public void onPause() {
        myReadTask.myCancel(true);
        myReadTask.cancel(true);
        m_record.stop();
        redrawer.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }

    private void cleanup() {
        // aunregister with the orientation sensor before exiting:
        //sensorMgr.unregisterListener(this);
        finish();
    }

    class NewRead{

        float internal1;
        float internal2;
        float arduino1;
        float arduino2;

        public NewRead(float i1, float i2, float a1, float a2){
            internal1 = i1;
            internal2 = i2;
            arduino1 = a1;
            arduino2 = a2;
        }

    }

    public synchronized void onNewRead(float a1,float a2,float a3) {

        // update level data:
        aLvlSeries.setModel(Arrays.asList(
                        new Number[]{a1}),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

        pLvlSeries.setModel(Arrays.asList(
                        new Number[]{a2}),
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

//        rLvlSeries.setModel(Arrays.asList(
  //                      new Number[]{a3}),
    //            SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

        // get rid the oldest sample in history:
        if (azimuthHistorySeries.size() > HISTORY_SIZE) {
      //      rollHistorySeries.removeFirst();
            pitchHistorySeries.removeFirst();
            azimuthHistorySeries.removeFirst();
        }

        // add the latest history sample:
        azimuthHistorySeries.addLast(null, a1);
        pitchHistorySeries.addLast(null, a2);
       // rollHistorySeries.addLast(null, a3);
    }





    class ReadTask extends AsyncTask<Integer, Integer, Integer> {
        boolean STOP = false;
        @Override
        protected Integer doInBackground(Integer... sensorValue) {
            //return (sensorValue[0]);  // This goes to result

            try {
                buffersize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
                buffer = new short[buffersize];

                m_record = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER,
                        SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, buffersize * 1);
            } catch (Throwable t) {
                Log.e("Error", "Initializing Audio Record and Play objects Failed " + t.getLocalizedMessage());
            }

            m_record.startRecording();

            while(!STOP) {
                final int n = m_record.read(buffer, 0, buffersize);
                            for(int i = 0;i<n;i+=29){
                                onNewRead(buffer[i]+10000,buffer[i+1],0.0f);
                                i++;
                            }

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

    GraphView mGraphView;
    GraphView mGraphView2;



    int buffersize;
    Thread m_thread;
    AudioRecord m_record;
    int SAMPLE_RATE = 16000;
    float quantStep = 2^16/5;
    AudioTrack m_track;
    short[]   buffer  ;
    boolean m_isRun=true;

    Integer[] mSensorValue = new Integer[1];

    ReadTask myReadTask;





    public void notifyNewBuffer(int n){
        mSensorValue[0]=n;

    }


}
