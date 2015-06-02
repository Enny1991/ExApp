package com.eneaceolini.exapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.musicg.dsp.FastFourierTransform;
import com.musicg.dsp.WindowFunction;
import com.musicg.wave.Wave;
//import com.musicg.wave.extension.Spectrogram;

import java.util.Random;


public class SpectrogramActivity extends ActionBarActivity {

    RelativeLayout rl;
    Paint paint = new Paint();
    Spectrogram sp;
    double [][] data = new double[500][500];


    Random r  = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spectrogram);


        rl = (RelativeLayout)findViewById(R.id.rl);
        sp = new Spectrogram(SpectrogramActivity.this,data);
        initData(data);
        rl.addView(sp);
        rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((RelativeLayout)v).removeAllViews();
                sp = new Spectrogram(SpectrogramActivity.this,data);
                initData(data);
                ((RelativeLayout)v).addView(sp);
            }
        });



    }

    public void initData(double[][] d){

        for (int i = 0; i < d.length; i++) {
            for (int j = 0; j < d[i].length; j++) {
                d[i][j] = r.nextInt(256);
            }
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spectrogram, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
