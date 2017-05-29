package com.aurailus.caninemusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class PlayerActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String STATE_QUEUE = "com.aurailus.caninemusic.QUEUE";
    private SeekBar seek;
    private TextView titleView;
    private TextView artistView;
    private TextView lengthView;
    private TextView timeView;
    private ImageView albumView;
    private ImageView playPauseButton;
    private boolean seekInteracting = false;
    private MusicService musicSrv;
    private boolean musicBound = false;
    private Intent playIntent;
    private Handler h;
    private Runnable r;
    private int updateDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.player_toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        seek = (SeekBar) findViewById(R.id.song_seekbar);
        titleView = (TextView) findViewById(R.id.song_title);
        artistView = (TextView) findViewById(R.id.song_artist);
        timeView = (TextView) findViewById(R.id.current_time);
        lengthView = (TextView) findViewById(R.id.song_duration);
        albumView = (ImageView) findViewById(R.id.album_cover);
        playPauseButton = (ImageView) findViewById(R.id.button_playpause);

        seek.setOnSeekBarChangeListener(this);

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("musicPrepared"));
        LocalBroadcastManager.getInstance(this).registerReceiver(pMessageReciever, new IntentFilter("playingState"));

        h = new Handler();
        r = new Runnable(){
            public void run(){
                updatePlayer();
                h.postDelayed(this, updateDelay);
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            h.removeCallbacks(r);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
            stopService(playIntent);
            unbindService(musicConnection);
            musicSrv = null;
        }
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
                    playPauseButton.setImageResource(R.drawable.ic_playcircle);
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

    private BroadcastReceiver pMessageReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean playing = intent.getBooleanExtra("State", false);
            if (playing) {
                playPauseButton.setImageResource(R.drawable.ic_pausecircle);
            }
            else {
                playPauseButton.setImageResource(R.drawable.ic_playcircle);
            }
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
        Drawable img = ImageHelper.findAlbumArtById(albumId, getApplicationContext());
        if (img != null) {
            albumView.setImageDrawable(img);
        }
        else {
            albumView.setImageResource(R.drawable.ic_album);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case (R.id.action_settings):
                break;
            case (R.id.action_queue):
                Intent intent = new Intent(PlayerActivity.this, QueueActivity.class);
                intent.putExtra(STATE_QUEUE, musicSrv.getList());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
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
            ((ImageView)view).setImageResource(R.drawable.ic_playcircle);
        }
        else {
            musicSrv.goCheckFocus();
            ((ImageView)view).setImageResource(R.drawable.ic_pausecircle);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void nextSong(View view) {
        musicSrv.playNext();
        playPauseButton.setImageResource(R.drawable.ic_pausecircle);
    }

    @SuppressWarnings("UnusedParameters")
    public void previousSong(View view) {
        if (musicSrv.getTime() > 3000) {
            musicSrv.seek(0);
        }
        else {
            musicSrv.playPrev();
            playPauseButton.setImageResource(R.drawable.ic_pausecircle);
        }
    }

    @Override
    public void onBackPressed() {
        back(null);
    }

    @SuppressWarnings("UnusedParameters")
    public void back(View view) {
        this.finish();
        overridePendingTransition(R.anim.slide_up_in, R.anim.slide_up_out);
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
