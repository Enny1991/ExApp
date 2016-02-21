package com.eneaceolini.exapp;

/**
 * Created by enea on 08/02/16.
 * Project COCOHA
 */
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class JNImathActivity extends Activity {
    /** Called when the activity is first created. */

    public EditText x;
    public EditText y;
    public EditText z;

    public EditText x2;
    public EditText y2;
    public EditText z2;

    public float[] vecArray;

    public TextView textView1;
    public Button run;

    float[] array3 = new float[3];
    float[] array1 = new float[3];
    float[] array2 = new float[3];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matrix);

        x = (EditText)findViewById(R.id.x);
        y = (EditText)findViewById(R.id.y);
        z = (EditText)findViewById(R.id.z);

        x2 = (EditText)findViewById(R.id.x);
        y2 = (EditText)findViewById(R.id.y);
        z2 = (EditText)findViewById(R.id.z);




        textView1 = (TextView)findViewById(R.id.textView1);
        run = (Button)findViewById(R.id.run);

        run.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {

                array1[0] = Float.parseFloat(x.getText().toString());
                array1[1] = Float.parseFloat(y.getText().toString());
                array1[2] = Float.parseFloat(z.getText().toString());

                array2[0] = Float.parseFloat(x2.getText().toString());
                array2[1] = Float.parseFloat(y2.getText().toString());
                array2[2] = Float.parseFloat(z2.getText().toString());
                array3 = test(array1, array2);

                String text = array3[0]+" "+array3[1]+" "+array3[2];
                textView1.setText(text);

            }

        });

    }

    public native float[] test(float[] array1, float[] array2);

    static {
        System.loadLibrary("test_eigen");
    }
}