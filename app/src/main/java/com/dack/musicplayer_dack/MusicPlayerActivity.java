package com.dack.musicplayer_dack;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import com.dack.musicplayer_dack.MusicLoader.MusicInfo;
import com.dack.musicplayer_dack.MusicService.MusicBinder;



public class MusicPlayerActivity extends ActionBarActivity implements SensorEventListener,View.OnTouchListener,GestureDetector.OnGestureListener {

    GestureDetector mGestureDetector;

    //声明控件
    //private ImageView albumPciture;       //专辑图片
    public static TextView musicName = null;//歌曲名
    public static TextView artist = null;   //歌手名
    private TextView currentTime;           //当前时间
    private TextView allTime;               //总歌曲时长
    public static ImageButton playStyle;    //播放模式
    public static ImageButton last;         //上一曲
    public static ImageButton play;         //播放
    public static ImageButton next;         //下一曲
    public static ImageButton musiclist;    //歌曲列表
    public static ImageButton love;         //
    public static ImageButton set;          //
    public static SeekBar mprogress;        //进度条

    public static LrcView lrcView;          //自定义歌词


    public List<MusicInfo> musicList;
    private int currentMusic;           // 当前播放歌曲
    private int currentPosition;        // 当前播放歌曲位置

    SensorManager sensorManager = null; //摇一摇
    Vibrator vibrator = null;           //...


    /**
     * 在Activity中我们通过BindService来获得一个Binder,然后就可以通过Binder来跟Service交互
     */
    private ProgressReceiver progressReceiver;
    private MusicBinder musicBinder;

    // 创建一个ServiceConnection 对象，在其onServiceConnected方法中返回我们的Binder
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicBinder = (MusicBinder) service;
        }
    };

    //接下来就是调用bindService了，将serviceConnection作为参数传进去
    private void connectToMusciService(){
        Intent intent = new Intent(MusicPlayerActivity.this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //*******************************沉浸式通知栏***********************************
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //******************************************************************************/
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);


        MusicLoader musicLoader = MusicLoader.instance(getContentResolver());//获取音乐
        musicList = musicLoader.getMusicList();

        connectToMusciService();                                //连接服务
        initView();                                             //初始化
        mGestureDetector = new GestureDetector((GestureDetector.OnGestureListener)this);
        RelativeLayout ll = (RelativeLayout) findViewById(R.id.mlist);
        ll.setOnTouchListener(this);
        ll.setLongClickable(true);

    }
    public void onResume(){
        super.onResume();
        registerReceiver();
        if(musicBinder != null){
            musicBinder.notifyActivity();
        }
        artist.setText(musicList.get(currentMusic).getArtist());
        musicName.setText(musicList.get(currentMusic).getTitle());
        allTime.setText(FormatHelper.formatDuration(musicList.get(currentMusic).getDuration()));

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onPause(){

        super.onPause();
        unregisterReceiver(progressReceiver);
        sensorManager.unregisterListener(this);
    }

    public void onStop(){

        super.onStop();
    }

    public void onDestroy(){
        super.onDestroy();
        if(musicBinder != null){
            unbindService(serviceConnection);
        }
    }


    private void initView() {

        //albumPciture = (ImageView) findViewById(R.id.albumPciture);
        musicName=(TextView) findViewById(R.id.musicName); //歌曲名
        artist=(TextView) findViewById(R.id.artist);
        currentTime=(TextView) findViewById(R.id.currentTime);
        allTime = (TextView) findViewById(R.id.allTime);
        playStyle = (ImageButton) findViewById(R.id.playStyle);
        last=(ImageButton) findViewById(R.id.left);
        play = (ImageButton) findViewById(R.id.play);
        next = (ImageButton) findViewById(R.id.next);
        musiclist= (ImageButton) findViewById(R.id.musiclist);
        set= (ImageButton) findViewById(R.id.set);
        love= (ImageButton) findViewById(R.id.search);
        mprogress = (SeekBar) findViewById(R.id.mprogress);

        ViewOnclickListener ViewOnClickListener = new ViewOnclickListener();

        playStyle.setOnClickListener(ViewOnClickListener);
        last.setOnClickListener(ViewOnClickListener);
        play.setOnClickListener(ViewOnClickListener);
        next.setOnClickListener(ViewOnClickListener);
        musiclist.setOnClickListener(ViewOnClickListener);
        set.setOnClickListener(ViewOnClickListener);
        mprogress.setOnSeekBarChangeListener(new SeekBarChangeListener());

        //歌词控件
        lrcView = (LrcView) findViewById(R.id.lrcShowView);
        lrcView.setOnClickListener(ViewOnClickListener);
        //歌词容器
        LinearLayout linear=(LinearLayout)findViewById(R.id.linear);
        linear.setOnClickListener(ViewOnClickListener);

    }



    /**
     * 控件点击事件
     *
     * @author wwj
     *
     */
    private class ViewOnclickListener implements View.OnClickListener {
        //Intent intent = new Intent();

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.playStyle:
                    musicBinder.changeMode();
                    break;

                case R.id.left:
                    //lrcView.setVisibility(View.GONE);//美化界面防止歌词闪出
                    musicBinder.toPrevious();
                    break;

                case R.id.play:
                    if(musicBinder.isPlaying()){
                        musicBinder.stopPlay();
                        MusicPlayerActivity.play.setImageDrawable(getResources().getDrawable(R.drawable.start));
                    }else{
                        musicBinder.startPlay(currentMusic,currentPosition);
                    }
                    break;

                case R.id.next:
                    musicBinder.toNext();
                    break;

                case R.id.musiclist:
                    Intent intent = new Intent();
                    intent.setClass(MusicPlayerActivity.this, MusicListActivity.class);
                    //intent.putExtra(MusicListActivity.MUSIC_LENGTH, currentMax);
                    intent.putExtra(MusicListActivity.CURRENT_MUSIC, currentMusic);
                    //intent.putExtra(MusicListActivity.CURRENT_POSITION, currentPosition);
                    startActivity(intent);
                    break;

                case R.id.set:
                    openOptionsMenu();
                    break;

                case R.id.lrcShowView:
                    if(musicBinder.isPlaying())
                        lrcView.setVisibility(View.INVISIBLE);
                    break;
                case R.id.linear:
                    if(lrcView.getVisibility()==View.INVISIBLE||lrcView.getVisibility()==View.GONE)
                        lrcView.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }


    /**
     * 实现监听Seekbar的类
     *
     * @author wwj
     *
     */


    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if(fromUser){
                musicBinder.changeProgress(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }

    }


    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return mGestureDetector.onTouchEvent(event);
    }


    /**
     * 屏幕监听事件
     */

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }
    private int verticalMinDistance = 40;
    private int minVelocity         = 0;

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // TODO Auto-generated method stub
        if (e1.getX() - e2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {

            Intent intent = new Intent(MusicPlayerActivity.this, MusicListActivity.class);
            startActivity(intent);
            finish ();

        }
        else if (e2.getX() - e1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {

            //切换Activity
            Intent intent = new Intent(MusicPlayerActivity.this, MusicListActivity.class);
            startActivity(intent);
            finish ();
        }

        return false;

    }

/*
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev);
        // scroll.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

*/



    /**
     * 震动换歌曲
     */

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        //当传感器精度改变时回调该方法，Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {

        int sensorType = event.sensor.getType();
        //values[0]:X轴，values[1]：Y轴，values[2]：Z轴
        float[] values = event.values;
        if (sensorType == Sensor.TYPE_ACCELEROMETER)
        {
            if ((Math.abs(values[0]) > 17 || Math.abs(values[1]) > 17 || Math
                    .abs(values[2]) > 17))
            {
                //摇动手机后，再伴随震动提示~~
                vibrator.vibrate(500);
                musicBinder.toNext();
            }

        }
    }



    private void registerReceiver(){
        progressReceiver = new ProgressReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.ACTION_UPDATE_PROGRESS);
        intentFilter.addAction(MusicService.ACTION_UPDATE_DURATION);
        intentFilter.addAction(MusicService.ACTION_UPDATE_CURRENT_MUSIC);
        registerReceiver(progressReceiver, intentFilter);
    }

    class ProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case MusicService.ACTION_UPDATE_PROGRESS:
                    int progress = intent.getIntExtra(MusicService.ACTION_UPDATE_PROGRESS, 0);
                    if (progress > 0) {
                        currentPosition = progress; // Remember the current position
                        mprogress.setProgress(progress / 1000);
                        currentTime.setText(FormatHelper.formatDuration(progress));
                    }
                    break;
                case MusicService.ACTION_UPDATE_CURRENT_MUSIC:
                    //Retrive the current music and get the title to show on top of the screen.
                    currentMusic = intent.getIntExtra(MusicService.ACTION_UPDATE_CURRENT_MUSIC, 0);
                    musicName.setText(musicList.get(currentMusic).getTitle());
                    artist.setText(musicList.get(currentMusic).getArtist());
                    break;
                case MusicService.ACTION_UPDATE_DURATION:
                    //Receive the duration and show under the progress bar
                    //Why do this ? because from the ContentResolver, the duration is zero.
                    int currentMax = intent.getIntExtra(MusicService.ACTION_UPDATE_DURATION, 0);
                   // int max = currentMax / 1000;
                    allTime.setText(FormatHelper.formatDuration(musicList.get(currentMusic).getDuration()));
                    mprogress.setMax(currentMax / 1000);
                    break;
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
        else if(id == R.id.quit){
            Intent intent = new Intent(MusicPlayerActivity.this, MusicService.class);
            stopService(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

}
