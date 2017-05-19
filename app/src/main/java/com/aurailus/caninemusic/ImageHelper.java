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

class ImageHelper {
    public static Drawable findAlbumArtById(String id, Context context) {
        Drawable albumArt;

        ContentResolver albumResolver = context.getContentResolver();
        Cursor albumCursor = albumResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,  //Location to grab from
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},   //Columns to grab
                MediaStore.Audio.Albums._ID + "=?",                                             //Selection filter... question marks substitute 4th row args
                new String[]{id},                                                               //Args for filter
                null);

        if (albumCursor != null) {
            if (albumCursor.moveToFirst()) {
                String albumString = albumCursor.getString(albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));

                if (albumString != null) {
                    File file = new File(albumString);

                    if (file.exists()) {

                        Bitmap albumBmp = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(albumString), 128, 128, false);
                        RoundedBitmapDrawable roundedArt = RoundedBitmapDrawableFactory.create(null, albumBmp);

                        roundedArt.setCornerRadius(1000.0f);
                        roundedArt.setAntiAlias(true);

                        albumArt = roundedArt;
                    } else return null;
                } else return null;
            } else return null;

            albumCursor.close();
        } else return null;

        return albumArt;
    }
}
