package com.aurailus.caninemusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private SeekBar seek;
    private TextView titleView;
    private TextView artistView;
    private TextView lengthView;
    private TextView timeView;
    private ImageView albumView;
    private ImageButton playPauseButton;
    private boolean seekInteracting = false;
    private MusicService musicSrv;
    private boolean musicBound = false;
    private Intent playIntent;
    private Handler h;
    private Runnable r;
    private int updateDelay;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            h.removeCallbacks(r);
            System.out.println("destroy");
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
            stopService(playIntent);
            unbindService(musicConnection);
            musicSrv = null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_player);

        seek = (SeekBar) findViewById(R.id.song_seekbar);
        titleView = (TextView) findViewById(R.id.song_title);
        artistView = (TextView) findViewById(R.id.song_artist);
        timeView = (TextView) findViewById(R.id.current_time);
        lengthView = (TextView) findViewById(R.id.song_duration);
        albumView = (ImageView) findViewById(R.id.album_cover);
        playPauseButton = (ImageButton) findViewById(R.id.button_playpause);

        seek.setOnSeekBarChangeListener(this);

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("musicPrepared"));

        h = new Handler();
        r = new Runnable(){
            public void run(){
                updatePlayer();
                h.postDelayed(this, updateDelay);
            }
        };
    }


   private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            musicSrv = binder.getService();
            musicBound = true;

            initPlayer();
            updatePlayer();

            if (!musicSrv.isPlaying()) {
                if (musicSrv.isPrepared()) {
                    playPauseButton.setBackgroundResource(R.drawable.ic_playcircle);
                }
            }

            updateDelay = 250;
            h.postDelayed(r, updateDelay);
        }

    @Override
    public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initPlayer();
        }
    };

    private void initPlayer() {
        seek.setMax(Math.round(musicSrv.getLength()));

        int x = musicSrv.getLength() / 1000;
        int seconds = x % 60;
        x /= 60;
        int minutes = x % 60;

        lengthView.setText(minutes + ":" + String.format(Locale.CANADA, "%02d", seconds));

        titleView.setText(musicSrv.getTitle());
        artistView.setText(musicSrv.getArtist());

        String albumId = musicSrv.getAlbumId();
        ContentResolver albumResolver = getContentResolver();
        Cursor albumCursor = albumResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,  //Location to grab from
                new String[] {MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},  //Columns to grab
                MediaStore.Audio.Albums._ID+ "=?",                                              //Selection filter... question marks substitute 4th row args
                new String[] {String.valueOf(albumId)},                                         //Args for filter
                null);

        if (albumCursor != null && albumCursor.moveToFirst()) {
            String albumString = albumCursor.getString(albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_ART));

            if (albumString != null) {
                File file = new File(albumString);

                if (file.exists()) {
                    Bitmap albumArt = Bitmap.createBitmap(BitmapFactory.decodeFile(albumString));
                    albumView.setImageBitmap(albumArt);
                }
            }

            albumCursor.close();
        }
    }
    private void updatePlayer() {
        if (musicBound) {
            if (!seekInteracting) {
                seek.setProgress(Math.round(musicSrv.getTime()));

                int x = musicSrv.getTime() / 1000;
                int seconds = x % 60;
                x /= 60;
                int minutes = x % 60;

                timeView.setText(minutes + ":" + String.format(Locale.CANADA, "%02d", seconds));
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
        if (seekInteracting && fromUser) {
            int x = seek.getProgress() / 1000;
            int seconds = x % 60;
            x /= 60;
            int minutes = x % 60;

            timeView.setText(minutes + ":" + String.format(Locale.CANADA, "%02d", seconds));
        }
    }

    public void togglePlaying(View view) {
        if (musicSrv.isPlaying()) {
            musicSrv.pausePlayer();
            view.setBackgroundResource(R.drawable.ic_playcircle);
        }
        else {
            musicSrv.go();
            view.setBackgroundResource(R.drawable.ic_pausecircle);
        }
    }

    public void nextSong(View view) {
        musicSrv.playNext();
        playPauseButton.setBackgroundResource(R.drawable.ic_pausecircle);
    }

    public void previousSong(View view) {
        if (musicSrv.getTime() > 3000) {
            musicSrv.seek(0);
        }
        else {
            musicSrv.playPrev();
            playPauseButton.setBackgroundResource(R.drawable.ic_pausecircle);
        }
    }

    public void back(View view) {
        this.finish();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {
        seekInteracting = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seek) {
        musicSrv.seek(seek.getProgress());
        seekInteracting = false;
    }
}
