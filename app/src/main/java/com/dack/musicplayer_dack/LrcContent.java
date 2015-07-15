package com.dack.musicplayer_dack;

/**
 * Created by Dack on 2015/4/12.
 * 歌词实体类
 */

public class LrcContent {
    private String lrcStr;	//歌词内容
    private int lrcTime;	//歌词当前时间
    public String getLrcStr() {
        return lrcStr;
    }
    public void setLrcStr(String lrcStr) {
        this.lrcStr = lrcStr;
    }
    public int getLrcTime() {
        return lrcTime;
    }
    public void setLrcTime(int lrcTime) {
        this.lrcTime = lrcTime;
    }
}

