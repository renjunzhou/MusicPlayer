package com.dack.musicplayer_dack;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Dack on 2015/4/18.
 * 音乐服务
 */

public class MusicService extends Service {

    private static final String TAG = "MUSIC_SERVICE";

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private List<MusicLoader.MusicInfo> musicList;//存放musicList对象的集合
    private Binder natureBinder = new MusicBinder();

    private int currentMusic;   // 记录当前正在播放的音乐
    private int currentPosition;//当前播放进度
    private int duration;		//播放长度

    private int currentMode = 3; //default sequence playing

    public static final String[] MODE_DESC = {"Single Loop", "List Loop", "Random", "Sequence"};

    //循环模式
    public static final int MODE_ONE_LOOP = 0;
    public static final int MODE_ALL_LOOP = 1;
    public static final int MODE_RANDOM = 2;
    public static final int MODE_SEQUENCE = 3;


    private static final int updateProgress = 1;
    private static final int updateCurrentMusic = 2;
    private static final int updateDuration = 3;

    public static final String ACTION_UPDATE_PROGRESS = "com.dack.musicplayer_dack.UPDATE_PROGRESS";
    public static final String ACTION_UPDATE_DURATION = "com.dack.musicplayer_dack.UPDATE_DURATION";
    public static final String ACTION_UPDATE_CURRENT_MUSIC = "com.dack.musicplayer_dack.UPDATE_CURRENT_MUSIC";




    //************************歌词***********************
    private LrcProcess mLrcProcess; //歌词处理
    private List<LrcContent> lrcList = new ArrayList<LrcContent>(); //存放歌词列表对象
    private int index = 0;          //歌词检索值
    public static final String SHOW_LRC = "com.dack.musicplayer_dack.SHOW_LRC";			//通知显示歌词

    //****************************************************

    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case updateProgress:
                    toUpdateProgress();
                    break;
                case updateDuration:
                    toUpdateDuration();
                    break;
                case updateCurrentMusic:
                    toUpdateCurrentMusic();
                    break;
            }
        }
    };

    /**
     * 通过发送广播的方式通知列表界面
     * Activity中会注册相应的广播过滤器来接收Service中发送出去的消息
     */


    private void toUpdateProgress(){
        if(mediaPlayer != null && isPlaying){
            int progress = mediaPlayer.getCurrentPosition();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_PROGRESS);
            intent.putExtra(ACTION_UPDATE_PROGRESS,progress);
            sendBroadcast(intent);
            handler.sendEmptyMessageDelayed(updateProgress, 1000);
        }
    }

    private void toUpdateDuration(){
        if(mediaPlayer != null){
            int duration = mediaPlayer.getDuration();
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATE_DURATION);
            intent.putExtra(ACTION_UPDATE_DURATION,duration);
            sendBroadcast(intent);
        }
    }

    private void toUpdateCurrentMusic(){
        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATE_CURRENT_MUSIC);
        intent.putExtra(ACTION_UPDATE_CURRENT_MUSIC,currentMusic);
        sendBroadcast(intent);
    }

    /**
     * 0.service建立和销毁时调用的函数
     */
    public void onCreate(){
        initMediaPlayer();
        musicList = MusicLoader.instance(getContentResolver()).getMusicList();
        Log.v(TAG, "OnCreate");
        super.onCreate();

        Intent intent = new Intent(this, MusicPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification.Builder(this)
                .setTicker("Nature")
                .setContentTitle("Playing")
                .setContentText(musicList.get(currentMusic).getTitle())
                .setContentIntent(pendingIntent)
                .getNotification();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(1, notification);

    }

    public void onDestroy(){
        if(mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    /**
     * 0.1.initialize the MediaPlayer
     */
    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
                mediaPlayer.seekTo(currentPosition);
                Log.v(TAG, "[OnPreparedListener] Start at " + currentMusic + " in mode " + currentMode + ", currentPosition : " + currentPosition);
                handler.sendEmptyMessage(updateDuration);
            }
        });

        /**
         * 播放器自身逻辑控制
         * 一首播放完成后继续播放
         */

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(isPlaying){
                    Log.v(TAG, "[OnCompletionListener] On Completion at " + currentMusic);
                    switch (currentMode) {
                        case MODE_ONE_LOOP:
                            Log.v(TAG, "[Mode] currentMode = MODE_ONE_LOOP.");
                            mediaPlayer.start();
                            break;
                        case MODE_ALL_LOOP:
                            Log.v(TAG, "[Mode] currentMode = MODE_ALL_LOOP.");
                            play((currentMusic + 1) % musicList.size(), 0);
                            break;
                        case MODE_RANDOM:
                            Log.v(TAG, "[Mode] currentMode = MODE_RANDOM.");
                            play(getRandomPosition(), 0);
                            break;
                        case MODE_SEQUENCE:
                            Log.v(TAG, "[Mode] currentMode = MODE_SEQUENCE.");
                            if(currentMusic < musicList.size() - 1){
                                playNext();
                            }
                            break;
                        default:
                            Log.v(TAG, "No Mode selected! How could that be ?");
                            break;
                    }
                    Log.v(TAG, "[OnCompletionListener] Going to play at " + currentMusic);
                }
            }
        });
    }

    /**
     *1.播放控制部分
     */

    private void setCurrentMusic(int pCurrentMusic){
        currentMusic = pCurrentMusic;
        handler.sendEmptyMessage(updateCurrentMusic);
    }

    private int getRandomPosition(){
        return (int)(Math.random() * (musicList.size() - 1));
    }

    private void play(int currentMusic, int pCurrentPosition) {
        currentPosition = pCurrentPosition;
        setCurrentMusic(currentMusic);
        MusicPlayerActivity.play.setImageDrawable(getResources().getDrawable(R.drawable.pause));
        initLrc();
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(musicList.get(currentMusic).getUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v(TAG, "[Play] Start Preparing at " + currentMusic);


        mediaPlayer.prepareAsync();
        handler.sendEmptyMessage(updateProgress);

        isPlaying = true;

    }

    private void stop(){
        mediaPlayer.stop();
        isPlaying = false;
    }

    private void playNext(){
        switch(currentMode){
            case MODE_ONE_LOOP:
                play(currentMusic, 0);
                break;
            case MODE_ALL_LOOP:
                if(currentMusic + 1 == musicList.size()){
                    play(0,0);
                }else{
                    play(currentMusic + 1, 0);
                }
                break;
            case MODE_SEQUENCE:
                if(currentMusic + 1 == musicList.size()){
                    Toast.makeText(this, "No more song.", Toast.LENGTH_SHORT).show();
                }else{
                    play(currentMusic + 1, 0);
                }
                break;
            case MODE_RANDOM:
                play(getRandomPosition(), 0);
                break;
        }
    }

    private void playPrevious(){
        switch(currentMode){
            case MODE_ONE_LOOP:
                play(currentMusic, 0);
                break;
            case MODE_ALL_LOOP:
                if(currentMusic - 1 < 0){
                    play(musicList.size() - 1, 0);
                }else{
                    play(currentMusic - 1, 0);
                }
                break;
            case MODE_SEQUENCE:
                if(currentMusic - 1 < 0){
                    Toast.makeText(this, "No previous song.", Toast.LENGTH_SHORT).show();
                }else{
                    play(currentMusic - 1, 0);
                }
                break;
            case MODE_RANDOM:
                play(getRandomPosition(), 0);
                break;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return natureBinder;
    }


    /**
     * 2.
     * service接受控制的逻辑部分
     * 要响应 Activity 中按钮的控制
     *  bindService的方法来将Activity 跟 Service 绑定在一起
     *  通过绑定，从Service返回了一个继承于Binder的类给Activity，而通过这个继承类，可以在里面定义一些交互的回调函数，那么Activity就可以通过这些方法来随时告诉Service要做些啥事了
     */

    class MusicBinder extends Binder{

        public void startPlay(int currentMusic, int currentPosition){
            play(currentMusic,currentPosition);
        }

        public void stopPlay(){
            stop();
        }

        public void toNext(){
            playNext();
        }

        public void toPrevious(){
            playPrevious();
        }

        /**
         * MODE_ONE_LOOP = 1;
         * MODE_ALL_LOOP = 2;
         * MODE_RANDOM = 3;
         * MODE_SEQUENCE = 4;
         */
        public void changeMode(){
            currentMode = (currentMode + 1) % 4;
            Log.v(TAG, "[NatureBinder] changeMode : " + currentMode);
            Toast.makeText(MusicService.this, MODE_DESC[currentMode], Toast.LENGTH_SHORT).show();
        }

        /**
         * return the current mode
         * MODE_ONE_LOOP = 1;
         * MODE_ALL_LOOP = 2;
         * MODE_RANDOM = 3;
         * MODE_SEQUENCE = 4;
         */
        public int getCurrentMode(){
            return currentMode;
        }

        /**
         * The service is playing the music
         */
        public boolean isPlaying(){
            return isPlaying;
        }

        /**
         * Notify Activities to update the current music and duration when current activity changes.
         * 要告诉调用者，当前播哪首歌了，歌多长啊
         */
        public void notifyActivity(){
            toUpdateCurrentMusic();
            toUpdateDuration();
        }

        /**
         * Seekbar changes
         * 有人拖动Seekbar了，要告诉service去改变播放的位置
         */
        public void changeProgress(int progress){
            if(mediaPlayer != null){
                currentPosition = progress * 1000;
                if(isPlaying){
                    mediaPlayer.seekTo(currentPosition);
                }else{
                    play(currentMusic, currentPosition);
                }
            }
        }
    }



    /**
     * 初始化歌词配置
     */
    public void initLrc(){
        mLrcProcess = new LrcProcess();
        //读取歌词文件
        mLrcProcess.readLRC(musicList.get(currentMusic).getUrl());

        //传回处理后的歌词文件
        lrcList = mLrcProcess.getLrcList();
        MusicPlayerActivity.lrcView.setmLrcList(lrcList);
        //切换带动画显示歌词
        MusicPlayerActivity.lrcView.setAnimation(AnimationUtils.loadAnimation(MusicService.this, R.anim.alpha_z));
        handler.post(mRunnable);
    }
    Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            MusicPlayerActivity.lrcView.setIndex(lrcIndex());
            MusicPlayerActivity.lrcView.invalidate();
            handler.postDelayed(mRunnable, 100);
        }
    };

    /**
     * 根据时间获取歌词显示的索引值
     */
    public int lrcIndex() {
        if(mediaPlayer.isPlaying()) {
            currentPosition = mediaPlayer.getCurrentPosition();
            duration = mediaPlayer.getDuration();
        }
        if(currentPosition < duration) {
            for (int i = 0; i < lrcList.size(); i++) {
                if (i < lrcList.size() - 1) {
                    if (currentPosition < lrcList.get(i).getLrcTime() && i == 0) {
                        index = i;
                    }
                    if (currentPosition > lrcList.get(i).getLrcTime()
                            && currentPosition < lrcList.get(i + 1).getLrcTime()) {
                        index = i;
                    }
                }
                if (i == lrcList.size() - 1
                        && currentPosition > lrcList.get(i).getLrcTime()) {
                    index = i;
                }
            }
        }
        return index;
    }

}
