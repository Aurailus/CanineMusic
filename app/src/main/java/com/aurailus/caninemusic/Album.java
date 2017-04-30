package com.aurailus.caninemusic;

import android.support.v4.graphics.drawable.RoundedBitmapDrawable;

public class Album {
    private long id;
    private RoundedBitmapDrawable albumArt;

    public Album(long id) {
        this.id = id;
    }

    /*Getters and setters*/
    public long getId() {
        return id;
    }

}
