package com.dack.musicplayer_dack;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dack.musicplayer_dack.MusicLoader.MusicInfo;
import java.util.List;


public class MusicListAdapter extends BaseAdapter {
    private Context context;
    private List<MusicInfo> musicList;

    public MusicListAdapter(Context context, List<MusicLoader.MusicInfo> musicList) {
        this.context = context;
        this.musicList=musicList;
    }

    @Override
    public int getCount() {
        return musicList.size();
    }

    @Override
    public Object getItem(int position) {
        return musicList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return musicList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.music_item, null);
            ImageView pImageView = (ImageView) convertView.findViewById(R.id.albumPhoto);
            TextView pTitle = (TextView) convertView.findViewById(R.id.title);
            TextView pDuration = (TextView) convertView.findViewById(R.id.duration);
            TextView pArtist = (TextView) convertView.findViewById(R.id.artist);
            holder = new ViewHolder(pImageView, pTitle, pDuration, pArtist);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }


        //1.是否载入默认 2.是否小图
        Bitmap bm = PictureLoader.getArtwork(context, musicList.get(position).getId(), musicList.get(position).getAlbumId(),true,false);
        if(bm == null) {
            holder.imageView.setImageResource(R.drawable.audio);
        } else {
            holder.imageView.setImageBitmap(bm);
        }

        holder.title.setText(musicList.get(position).getTitle());
        holder.duration.setText(FormatHelper.formatDuration(musicList.get(position).getDuration()));
        holder.artist.setText(musicList.get(position).getArtist());

        return convertView;
    }
    class ViewHolder{
        public ViewHolder(ImageView pImageView, TextView pTitle, TextView pDuration, TextView pArtist){
            imageView = pImageView;
            title = pTitle;
            duration = pDuration;
            artist = pArtist;
        }

        public ImageView imageView;
        public TextView title;
        public TextView duration;
        public TextView artist;
    }

}


