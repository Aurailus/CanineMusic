package com.aurailus.caninemusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aurailus.caninemusic.PaletteGrabber.Palette;

import java.util.Locale;

import static java.lang.Math.abs;

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

    Bitmap albumArt = BitmapFactory.decodeFile(ImageHelper.findAlbumPathById(albumId, getApplicationContext()));
    Palette colorPalette = new Palette(albumArt);
    short[] colVals = colorPalette.getColour();
    short[] oriVals = colVals;

    System.out.println("Set color");
    if (colVals[0] != -1) {
        short attp = 0;
        while ((abs(colVals[0] - colVals[1]) < 30) &&
                (abs(colVals[0] - colVals[2]) < 30) &&
                (abs(colVals[1] - colVals[2]) < 30) && attp < 5) {
            colVals = colorPalette.getColour();
            attp++;
            System.out.println("Color next attempt");
        }
        if (attp == 5) {
            colVals = oriVals;
        }

        int r = (Math.max(colVals[0] - 20, 0) << 16) & 0x00FF0000;
        int g = (Math.max(colVals[1] - 20, 0) << 8) & 0x0000FF00;
        int b = (Math.max(colVals[2] - 20, 0)) & 0x000000FF;

        int themeColor = 0xFF000000 | r | g | b;
        findViewById(R.id.player_actionbar).setBackgroundColor(themeColor);
    }
    else {
        findViewById(R.id.player_actionbar).setBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimary));
    }
    if (findEuclideanDist(colVals[0], colVals[1], colVals[2], 0, 0, 0) > 340) {
        titleView.setTextColor(0xff000000);
        artistView.setTextColor(0xff000000);
        ((ImageView)findViewById(R.id.back_button)).setColorFilter(0xff000000);
        ((ImageView)findViewById(R.id.queue_button)).setColorFilter(0xff000000);
        findViewById(R.id.back_button).setBackgroundResource(R.drawable.ripple_oval);
        findViewById(R.id.queue_button).setBackgroundResource(R.drawable.ripple_oval);
    }
    else {
        titleView.setTextColor(0xffffffff);
        artistView.setTextColor(0xffffffff);
        ((ImageView)findViewById(R.id.back_button)).setColorFilter(0xffffffff);
        ((ImageView)findViewById(R.id.queue_button)).setColorFilter(0xffffffff);
        findViewById(R.id.back_button).setBackgroundResource(R.drawable.ripple_oval_light);
        findViewById(R.id.queue_button).setBackgroundResource(R.drawable.ripple_oval_light);
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

    public void openQueue(View view) {
        Intent intent = new Intent(PlayerActivity.this, QueueActivity.class);
        intent.putExtra(STATE_QUEUE, musicSrv.getList());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
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

    double findEuclideanDist(int x1, int y1, int z1, int x2, int y2, int z2) {
        double sum = 0.0;
        sum += Math.pow(x1 - x2, 2.0);
        sum += Math.pow(y1 - y2, 2.0);
        sum += Math.pow(z1 - z2, 2.0);
        return Math.sqrt(sum);
    }
}
