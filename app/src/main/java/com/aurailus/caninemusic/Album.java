package com.aurailus.caninemusic;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;

import java.io.File;

public class Album {
    private String id, title, artist;
    private RoundedBitmapDrawable albumArt;
    private boolean empty = false;

    public Album(String id, String title, String artist, Context context) {
        this.id = id;
        this.title = title;
        this.artist = artist;

        getAlbumArt(context);
    }
    public Album(String id, Context context) {
        this.id = id;

        getAlbumArt(context);
    }

    public void getAlbumArt(Context context) {
        ContentResolver albumResolver = context.getContentResolver();
        Cursor albumCursor = albumResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,  //Location to grab from
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},  //Columns to grab
                MediaStore.Audio.Albums._ID+ "=?",                                              //Selection filter... question marks substitute 4th row args
                new String[] {String.valueOf(id)},                                              //Args for filter
                null);

        if (albumCursor.moveToFirst()) {
            String albumString = albumCursor.getString(albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_ART));
            File file = new File(albumString);

            if (file.exists()) {

                Bitmap albumBmp = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(albumString), 128, 128, false);
                albumArt = RoundedBitmapDrawableFactory.create(null, albumBmp);

                albumArt.setCornerRadius(1000.0f);
                albumArt.setAntiAlias(true);
            }
            else empty = true;

        }
        else empty = true;
    }

    /*Getters and setters*/
    public String getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getArtist() {
        return artist;
    }
    public Drawable getImage() {
        return albumArt;
    }
    public boolean getEmpty() { return empty; }
}
