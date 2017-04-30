package com.aurailus.caninemusic;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class AlbumAdapter extends BaseAdapter {
    private ArrayList<Album> albums;
    private LayoutInflater albumInf;

    @Override
    public int getCount() {
        return albums.size();
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

        ConstraintLayout albumLay =(ConstraintLayout)albumInf.inflate(R.layout.album, parent, false);
        TextView songView = (TextView)albumLay.findViewById(R.id.album_title);
        ImageView albumArtView = (ImageView)albumLay.findViewById(R.id.album_art);

        Album curAlbum = albums.get(ind);
        songView.setText(curAlbum.getTitle());

        if (!curAlbum.getEmpty()) {
            Drawable img = curAlbum.getImage();
            albumArtView.setImageDrawable(img);
        }

        albumLay.setTag(curAlbum.getId());
        return albumLay;
    }

    public AlbumAdapter(Context c, ArrayList<Album> albums) {
        this.albums = albums;
        albumInf = LayoutInflater.from(c);
    }
}
