package com.aurailus.caninemusic;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.content.Context;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;

public class SongArt {
    private String albumId;
    private RoundedBitmapDrawable albumArt;
    private boolean empty = false;

    public SongArt(String albumId, Context context) {
        this.albumId = albumId;

        getAlbumArt(context);
    }

    public void getAlbumArt(Context context) {
        ContentResolver albumResolver = context.getContentResolver();
        Cursor albumCursor = albumResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,  //Location to grab from
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},  //Columns to grab
                MediaStore.Audio.Albums._ID+ "=?",                                              //Selection filter... question marks substitute 4th row args
                new String[] {String.valueOf(albumId)},                                         //Args for filter
                null);

        if (albumCursor.moveToFirst()) {
            String albumString = albumCursor.getString(albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_ART));

            Bitmap albumBmp = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(albumString), 128, 128, false);
            albumArt = RoundedBitmapDrawableFactory.create(null, albumBmp);

            albumArt.setCornerRadius(1000.0f);
            albumArt.setAntiAlias(true);

        }
        else empty = true;
    }

    public String getId() {
        return albumId;
    }

    public Drawable getImage() {
        return albumArt;
    }

    public boolean getEmpty() { return empty; }
}
