package com.aurailus.caninemusic;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;

class Album {
    private String id;
    private String title;
    private String artist;
    private RoundedBitmapDrawable albumArt;

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

    private void getAlbumArt(Context context) {
        albumArt = (RoundedBitmapDrawable)ImageHelper.findAlbumArtById(id, context);
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
}
