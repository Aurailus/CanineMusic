package com.aurailus.caninemusic;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int ind;
    private final IBinder musicBind = new MusicBinder();
    private String songTitle, songArtist;
    private static final int NOTIFY_ID = 1;
    private boolean shuffle = true;
    private Random rand;
    private Runnable playerStart;
    private Handler h;

    @Override
    public void onCreate() {
        super.onCreate();
        ind = 0;
        player = new MediaPlayer();
        rand = new Random();

        initMusicPlayer();

        h = new Handler();
        playerStart = new Runnable(){
            public void run(){
                System.out.println("Playback started");
                player.start();
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        playNext();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        player.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        h.removeCallbacks(playerStart);
        int delay = 250;
        h.postDelayed(playerStart, delay);

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle);
        Notification not = builder.build();
        startForeground(NOTIFY_ID, not);

        Intent intent = new Intent("musicPrepared");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        System.out.println("BROADCAST SENT");
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        player.reset();
        Song playSong = songs.get(ind);
        songTitle = playSong.getTitle();
        songArtist = playSong.getArtist();
        long curSong = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, curSong);

        try {
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public void playPrev() {
        ind--;
        if (ind < 0) ind = songs.size() - 1;
        playSong();
    }

    public void playNext() {
        if (shuffle) {
            ind = rand.nextInt(songs.size());
        }
        else {
            ind++;
            if (ind >= songs.size()) ind = 0;
        }
        playSong();
    }

    public void setSong(int songInd) {
        ind = songInd;
    }

    public int getTime() {
        return player.getCurrentPosition();
    }
    public int getLength() {
        return player.getDuration();
    }
    public String getTitle() { return songTitle; }
    public String getArtist() { return songArtist; }
    public String getAlbumId() { return songs.get(ind).getAlbumId(); }
    public boolean getShuffle() {
        return shuffle;
    }
    public MediaPlayer getPlayer() {
        return player;
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }
    public void pausePlayer() {
        player.pause();
    }
    public void seek(int pos) {
        player.seekTo(pos);
    }
    public void go() {
        player.start();
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }
}
