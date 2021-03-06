package com.aurailus.caninemusic;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

class SongAdapter extends BaseAdapter {
    private final ArrayList<Song> songs;
    private final ArrayList<Album> albumart;
    private final LayoutInflater songInf;

    private static class ViewHolder {
        TextView songView;
        TextView artistView;
        TextView songDuration;
        ImageView albumArtView;
    }

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

        ViewHolder holder;

        if (convertView == null) {
            convertView = songInf.inflate(R.layout.item_song, parent, false);
            holder = new ViewHolder();

            holder.songView = (TextView)convertView.findViewById(R.id.song_title);
            holder.artistView = (TextView)convertView.findViewById(R.id.song_artist);
            holder.albumArtView = (ImageView)convertView.findViewById(R.id.album_art);
            holder.songDuration = (TextView)convertView.findViewById(R.id.song_duration);

            convertView.setTag(R.id.holder_id, holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag(R.id.holder_id);
        }

        Song curSong = songs.get(ind);
        holder.songView.setText(curSong.getTitle());
        holder.artistView.setText(curSong.getArtist());
        holder.songDuration.setText(curSong.getHumanLength());

        for(Album curAlbum : albumart) {
            if (curAlbum.getId().equals(curSong.getAlbumId())) {
                if (curAlbum.getImage() != null) {

                    Bitmap img = curAlbum.getImage();
                    holder.albumArtView.setImageBitmap(img);
                    break;
                }
                else {
                    holder.albumArtView.setImageResource(R.drawable.ic_album_unknown);
                }
            }
        }

        convertView.setTag(R.id.index_id, ind);
        return convertView;
    }

    SongAdapter(Context c, ArrayList<Song> songs) {
        this.songs = songs;
        albumart = new ArrayList<>();

        for(int i = 0; i < songs.size(); i++) {

            Song curSong = songs.get(i);
            boolean exists = false;
            for(int j = 0; j < albumart.size(); j++) {
                Album curAlbum = albumart.get(j);
                if (curSong.getAlbumId().equals(curAlbum.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                albumart.add(new Album(curSong.getAlbumId(), c));
            }
        }

        songInf = LayoutInflater.from(c);
    }
}
