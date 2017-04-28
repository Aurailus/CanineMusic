package com.aurailus.caninemusic;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    private ArrayList<SongArt> albumart;
    private LayoutInflater songInf;

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int arg0) {
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        return 0;
    }

    @Override
    public View getView(int ind, View convertView, ViewGroup parent) {

        ConstraintLayout songLay =(ConstraintLayout)songInf.inflate(R.layout.song, parent, false);
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        ImageView albumArtView = (ImageView)songLay.findViewById(R.id.album_art);
        TextView songDuration = (TextView)songLay.findViewById(R.id.song_duration);

        Song curSong = songs.get(ind);
        songView.setText(curSong.getTitle());
        artistView.setText(curSong.getArtist());
        songDuration.setText(curSong.getHumanLength());

        for(int i = 0; i < albumart.size(); i++) {
            SongArt curAlbum = albumart.get(i);
            if (curAlbum.getId().equals(curSong.getAlbumId())) {
                if (!curAlbum.getEmpty()) {
                    Drawable img = curAlbum.getImage();
                    albumArtView.setImageDrawable(img);
                    break;
                }
            }
        }

        songLay.setTag(ind);
        return songLay;
    }

    public SongAdapter(Context c, ArrayList<Song> songs) {
        this.songs = songs;
        albumart = new ArrayList<>();

        for(int i = 0; i < songs.size(); i++) {

            Song curSong = songs.get(i);
            boolean exists = false;
            for(int j = 0; j < albumart.size(); j++) {
                SongArt curAlbum = albumart.get(j);
                if (curSong.getAlbumId().equals(curAlbum.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                albumart.add(new SongArt(curSong.getAlbumId(), c));
            }
        }

        songInf = LayoutInflater.from(c);
    }
}
