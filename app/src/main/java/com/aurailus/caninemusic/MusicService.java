package com.aurailus.caninemusic;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private static final int NOTIFY_ID = 1;
    private MediaPlayer player;
    private ArrayList<Song> songs;
    private int ind;
    private IBinder musicBind;
    private String songTitle, songArtist;
    private boolean shuffle;
    private Random rand;
    private Handler h;
    private Runnable playerStart;
    private Runnable updateSeekbar;
    private boolean prepared;
    private Intent notIntent;
    private RemoteViews view, bigView;
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        shuffle = false;
        ind = 0;
        prepared = false;
        musicBind = new MusicBinder();
        player = new MediaPlayer();
        rand = new Random();

        initMusicPlayer();

        h = new Handler();
        playerStart = new Runnable(){
            public void run(){
                player.start();
                prepared = true;
            }
        };
        updateSeekbar = new Runnable(){
            public void run(){
                setSeekbar();
                h.postDelayed(this, 1000);
            }
        };

        notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
        h.removeCallbacks(updateSeekbar);
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        player.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer player) {
        int delay = 250;
        h.removeCallbacks(updateSeekbar);
        h.postDelayed(playerStart, delay);
        h.postDelayed(updateSeekbar, 250);

        Intent intent = new Intent("musicPrepared");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void setSeekbar() {
        if (notification != null) {
            view.setProgressBar(R.id.noti_seekbar, Math.round(getLength()), Math.round(getTime()), false);
            bigView.setProgressBar(R.id.noti_seekbar, Math.round(getLength()), Math.round(getTime()), false);
            Notification.Builder builder = new Notification.Builder(this);
            PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentIntent(pendInt)
                    .setContentText(songTitle)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker(songTitle)
                    .setContentTitle("Playing " + songTitle);

            if (Build.VERSION.SDK_INT < 24) {
                //noinspection deprecation
                builder.setContent(view);
            }
            else {
                builder.setCustomContentView(view);
                builder.setCustomBigContentView(bigView);
            }
            if (Build.VERSION.SDK_INT >= 16) {
                builder.setPriority(Notification.PRIORITY_MAX);
            }

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NOTIFY_ID, builder.build());
        }
    }

    public void setList(ArrayList<Song> songs) {
        this.songs = songs;
    }

    private void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        System.out.println("focus");
    }

    class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void updateNotification(Bitmap albumArt) {
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this);

        view = new RemoteViews(this.getPackageName(), R.layout.notification);
        bigView = new RemoteViews(this.getPackageName(), R.layout.notification_big);

        view.setImageViewResource(R.id.noti_prev_button, R.drawable.ic_noti_prev); //Previous button
        view.setImageViewResource(R.id.noti_pause_button, R.drawable.ic_noti_pause); //Pause button
        view.setImageViewResource(R.id.noti_next_button, R.drawable.ic_noti_next); //Next button

        view.setTextViewText(R.id.noti_title, songTitle); //Title
        view.setTextViewText(R.id.noti_artist, songArtist); //Artist

        bigView.setImageViewResource(R.id.noti_prev_button, R.drawable.ic_noti_prev); //Previous button
        bigView.setImageViewResource(R.id.noti_pause_button, R.drawable.ic_noti_pause); //Pause button
        bigView.setImageViewResource(R.id.noti_next_button, R.drawable.ic_noti_next); //Next button

        bigView.setTextViewText(R.id.noti_title, songTitle); //Title
        bigView.setTextViewText(R.id.noti_artist, songArtist); //Artist

        if (albumArt != null) {
            view.setImageViewBitmap(R.id.noti_album_art, albumArt);
            bigView.setImageViewBitmap(R.id.noti_album_art, albumArt);
        }
        else {
            view.setImageViewResource(R.id.noti_album_art, R.drawable.ic_album_unknown);
            bigView.setImageViewResource(R.id.noti_album_art, R.drawable.ic_album_unknown);
        }

        builder.setContentIntent(pendInt)
                .setContentText(songTitle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(songTitle)
                .setContentTitle("Playing " + songTitle);

        if (Build.VERSION.SDK_INT < 24) {
            //noinspection deprecation
            builder.setContent(view);
        }
        else {
            builder.setCustomContentView(view);
            builder.setCustomBigContentView(bigView);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            builder.setPriority(Notification.PRIORITY_MAX);
            notification = builder.build();
        }
        else {
            notification = builder.getNotification();
        }
        startForeground(NOTIFY_ID, notification);
    }

    public void playSong() {
        player.reset();
        prepared = false;
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
        h.removeCallbacks(playerStart);
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
    public ArrayList<Song> getList() {
        return songs;
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }
    public boolean isPrepared() {
        return prepared;
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
