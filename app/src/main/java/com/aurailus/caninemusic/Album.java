package com.aurailus.caninemusic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.aurailus.caninemusic.PaletteGrabber.Palette;

class Album {
    private String id;
    private String title;
    private String artist;
    private Bitmap albumArt;

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
        Drawable albumDraw = ImageHelper.roundAlbumDrawable(ImageHelper.findAlbumArtById(id, context));

        if (albumDraw != null) {
            albumArt = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(albumArt);
            albumDraw.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            albumDraw.draw(canvas);
        }
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
    public Bitmap getImage() {
        return albumArt;
    }
}
