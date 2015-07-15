package com.dack.musicplayer_dack;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.gesture.GestureOverlayView;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.List;
import com.dack.musicplayer_dack.MusicLoader.MusicInfo;
import com.dack.musicplayer_dack.MusicService.MusicBinder;



public class MusicListActivity extends ActionBarActivity implements View.OnTouchListener,GestureDetector.OnGestureListener {

    //public static final String MUSIC_LENGTH = "com.example.nature.DetailActivity.MUSIC_LENGTH";
    //public static final String CURRENT_POSITION = "com.dack.musicplayer_dack.CURRENT_POSITION";
    public static final String CURRENT_MUSIC = "com.dack.musicplayer_dack.MusicListActivity.CURRENT_MUSIC";

    GestureDetector mGestureDetector;

    public static ListView lvSongs;

    public List<MusicInfo> musicList;

    private int currentMusic;
    private int currentPosition;

    private ProgressReceiver progressReceiver;

    //1.在Activity中我们通过BindService来获得一个Binder,然后就可以通过Binder来跟Service交互
    private MusicBinder musicBinder;

    //2.创建一个ServiceConnection 对象，在其onServiceConnected方法中返回我们的Binder
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicBinder = (MusicBinder) service;
        }
    };
    //3.接下来就是调用bindService了，将serviceConnection作为参数传进去
    private void connectToMusicService(){
        Intent intent = new Intent(MusicListActivity.this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*******************************沉浸式通知栏***********************************
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        //******************************************************************************/

        MusicLoader musicLoader = MusicLoader.instance(getContentResolver());//获取音乐
        musicList = musicLoader.getMusicList();
        setContentView(R.layout.activity_musiclist);

        connectToMusicService();                                //连接服务
        initComponents();                                        //初始化

        mGestureDetector = new GestureDetector((GestureDetector.OnGestureListener)this);
        LinearLayout ll = (LinearLayout) findViewById(R.id.list);
        ll.setOnTouchListener(this);
        ll.setLongClickable(true);

    }




    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev);
        // scroll.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
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
    private int verticalMinDistance = 30;
    private int minVelocity         = 0;

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // TODO Auto-generated method stub
        if (e1.getX() - e2.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {

            Intent intent = new Intent(MusicListActivity.this, MusicPlayerActivity.class);
            startActivity(intent);
            finish();
        }
        else if (e2.getX() - e1.getX() > verticalMinDistance && Math.abs(velocityX) > minVelocity) {

            //切换Activity
            Intent intent = new Intent(MusicListActivity.this, MusicPlayerActivity.class);
            startActivity(intent);
            finish();
        }

        return false;

    }

    @Override
    public void onResume(){
        super.onResume();
        initReceiver();

    }
    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(progressReceiver);
    }
    @Override
    public void onStop(){
        super.onStop();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if(musicBinder != null){
            unbindService(serviceConnection);
        }
    }

    void initComponents(){
        currentMusic = getIntent().getIntExtra(CURRENT_MUSIC,0);

        MusicListAdapter adapter = new MusicListAdapter(this,musicList);
        lvSongs = (ListView) findViewById(R.id.lvSongs);
        lvSongs.setAdapter(adapter);
        lvSongs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                if(currentMusic == position){
                    if(musicBinder.isPlaying()){
                        musicBinder.stopPlay();
                    }
                    else
                        musicBinder.startPlay(position, currentPosition);
                }
                else{
                    currentMusic = position;
                    musicBinder.startPlay(position, 0);
                }
            }
        });

    }

    private void initReceiver(){
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
            if(MusicService.ACTION_UPDATE_PROGRESS.equals(action)){
                int progress = intent.getIntExtra(MusicService.ACTION_UPDATE_PROGRESS, currentPosition);
                if(progress > 0){
                    currentPosition = progress; // Remember the current position
                }
            }else if(MusicService.ACTION_UPDATE_CURRENT_MUSIC.equals(action)){
                //Retrieve the current music and get the title to show on top of the screen.
                currentMusic = intent.getIntExtra(MusicService.ACTION_UPDATE_CURRENT_MUSIC, 0);
            }

        }

    }



}
