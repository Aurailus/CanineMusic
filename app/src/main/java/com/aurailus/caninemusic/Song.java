package com.aurailus.caninemusic;

import java.util.Locale;

public class Song {
    private long id, duration;
    private String title, artist, albumId, humanLength;

    public Song(long id, String title, String artist, String albumId, long duration) {
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

    public long getDuration() {
        return duration;
    }

    public String getHumanLength() {
        return humanLength;
    }
}
