package com.dack.musicplayer_dack;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;


/**
 * Created by Dack on 2015/4/19.
 * welcome
 */

public class MainActivity extends ActionBarActivity  {

    private Chronometer chronometer;
    private int waitTime=3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*******************************沉浸式通知栏***********************************
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //******************************************************************************/
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(R.layout.layout);


        chronometer = (Chronometer) findViewById(R.id.chronometer);
        chronometer.start();
        chronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                //如果开始计时到现在超过了startime秒
                if (SystemClock.elapsedRealtime() - chronometer.getBase() > waitTime * 1000) {
                    //停止计时
                    chronometer.stop();
                    Intent intent = new Intent();
                    intent.setClass(MainActivity.this, MusicPlayerActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

    }

}