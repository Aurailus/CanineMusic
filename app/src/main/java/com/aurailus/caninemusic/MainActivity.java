package com.aurailus.caninemusic;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.aurailus.caninemusic.MusicService.*;

public class MainActivity extends AppCompatActivity
        implements MediaPlayerControl, NavigationView.OnNavigationItemSelectedListener  {

    private ArrayList<Song> songList;
    private ArrayList<Song> playList;
    private ArrayList<Album> albumList;
    private ListView songView;
    private GridView albumView;
    private ViewFlipper flipper;
    private MusicService musicSrv;
    private Intent playIntent;
    private BottomNavigationView bottomNav;
    private boolean musicBound = false;
    private boolean paused = false;
    private boolean playbackPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
            return;
            }
        }

        songView = (ListView)findViewById(R.id.song_list);
        flipper = (ViewFlipper)findViewById(R.id.flipper);
        bottomNav = (BottomNavigationView)findViewById(R.id.navigation);
        songList = new ArrayList<>();

        getSongs();
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().toLowerCase().compareTo(b.getTitle().toLowerCase());
            }
        });

        playList = new ArrayList<>();
        playList.addAll(songList);

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        albumView = (GridView)findViewById(R.id.album_list);
        albumList = new ArrayList<>();
        getAlbums();
        Collections.sort(albumList, new Comparator<Album>() {
            public int compare(Album a, Album b) {
                return a.getTitle().toLowerCase().compareTo(b.getTitle().toLowerCase());
            }
        });
        AlbumAdapter albumAdt = new AlbumAdapter(this, albumList);
        albumView.setAdapter(albumAdt);

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectSection(item);
                return true;
            }
        });

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    void selectSection(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.nav_album):
                flipper.setDisplayedChild(0);
                break;
            case (R.id.nav_playlist):
                flipper.setDisplayedChild(1);
                break;
            case (R.id.nav_track):
                flipper.setDisplayedChild(2);
                break;
            case (R.id.nav_artist):
                flipper.setDisplayedChild(3);
                break;
            case (R.id.nav_genre):
                flipper.setDisplayedChild(4);
                break;
        }
    }

    //Set the musicBound variable
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            musicSrv = binder.getService();
            musicSrv.setList(playList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy() {
        //finish();
        //stopService(playIntent);
        //musicSrv = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    public void getSongs() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int idList = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int titleList = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int artistList = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            int albumList = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM_ID);
            int durationList = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DURATION);

            boolean next = true;
            while (next) {
                long thisId = musicCursor.getLong(idList);
                String thisTitle = musicCursor.getString(titleList);
                String thisArtist = musicCursor.getString(artistList);
                String albumId = musicCursor.getString(albumList);
                long thisDuration = musicCursor.getLong(durationList);

                songList.add(new Song(thisId, thisTitle, thisArtist, albumId, thisDuration));
                next = musicCursor.moveToNext();
            }
            musicCursor.close();
        }
    }

    public void getAlbums() {
        ContentResolver albumResolver = getContentResolver();
        Uri albumUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[]columns = {android.provider.MediaStore.Audio.Albums._ID, android.provider.MediaStore.Audio.Albums.ALBUM_ID,
                android.provider.MediaStore.Audio.Albums.ALBUM, android.provider.MediaStore.Audio.Albums.ARTIST};

        Cursor albumCursor = albumResolver.query(albumUri, columns, null, null, null);

        if (albumCursor != null && albumCursor.moveToFirst()) {
            int idList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums._ID);
            int titleList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM);
            int albumIdList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_ID);
            int artistList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ARTIST);

            boolean next = true;
            while (next) {
                String realId = albumCursor.getString(idList);
                String albumId = albumCursor.getString(albumIdList);
                String thisTitle = albumCursor.getString(titleList);
                String thisArtist = albumCursor.getString(artistList);

                boolean exists = false;
                for (Album album : albumList) {
                    if (album.getId().equals(albumId)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) albumList.add(new Album(albumId, thisTitle, thisArtist, this.getBaseContext()));
                next = albumCursor.moveToNext();
            }
            albumCursor.close();
        }
    }

    public void chooseSong(View view) {
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        if (playbackPaused) {
            playbackPaused = false;
        }
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }

    public void chooseAlbum(View view) {
        if (playbackPaused) {
            playbackPaused = false;
        }

        playList.clear();
        System.out.println(view.getTag());
        for(Song song : songList) {
            if (song.getAlbumId().equals(view.getTag())) {
                playList.add(song);
            }
        }
        musicSrv.setList(playList);
        musicSrv.setSong(0);
        musicSrv.playSong();
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_player:
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                startActivity(intent);
                break;
            case R.id.action_shuffle:
                musicSrv.setShuffle(!musicSrv.getShuffle());
                break;
            case R.id.action_jumble:
                break;
            case R.id.action_close:
            default:
                stopService(playIntent);
                musicSrv = null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //Media Control Events
    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void pause() {
        musicSrv.pausePlayer();
        playbackPaused = true;
    }

    @Override
    public int getDuration() {
        if (musicSrv != null && musicBound && musicSrv.isPlaying()) {
            return musicSrv.getLength();
        }
        else return 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
        }
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null && musicBound && musicSrv.isPlaying()) {
            return musicSrv.getTime();
        }
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        return musicSrv != null && musicBound && musicSrv.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private void playNext() {
        musicSrv.playNext();
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    private void playPrev() {
        musicSrv.playPrev();
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case (R.id.nav_id3):
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                startActivity(intent);
                break;
            case (R.id.nav_settings):
                //open settings
                break;
            case (R.id.nav_ringtone):
                //open ringtone editor
                break;
            case (R.id.nav_import):
                //import files
                break;
            case (R.id.nav_share):
                //send files
                break;
            case (R.id.nav_rate):
                String appPck = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPck)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPck)));
                }
                break;
            case (R.id.nav_about):
                //go to about page
                break;
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
