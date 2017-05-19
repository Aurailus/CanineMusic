package com.aurailus.caninemusic;

import java.io.Serializable;
import java.util.Locale;

class Song implements Serializable {
    private final long id;
    private final long duration;
    private final String title;
    private final String artist;
    private final String albumId;
    private final String humanLength;

    Song(long id, String title, String artist, String albumId, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.albumId = albumId;
        this.duration = duration;

        int x = (int)this.duration/1000;
        int seconds = x % 60;
        x /= 60;
        int minutes = x % 60;
        this.humanLength = minutes + ":" + String.format(Locale.CANADA, "%02d", seconds);
    }

    /*Getters and setters*/
    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumId() {
        return albumId;
    }

    public String getHumanLength() {
        return humanLength;
    }
}
