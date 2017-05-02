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
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.aurailus.caninemusic.MusicService.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener  {

    private ArrayList<Song> songList;
    private ArrayList<Song> playList;
    private ArrayList<Album> albumList;
    private ListView songView;
    private GridView albumGridView;
    private ListView albumListView;
    private ViewSwitcher albumSwitcher;
    private ViewFlipper flipper;
    private MusicService musicSrv;
    private Intent playIntent;
    private BottomNavigationView bottomNav;
    private boolean playbackPaused = false;
    private boolean albumIsGrid = true;
    private boolean musicBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        albumGridView = (GridView)findViewById(R.id.album_grid);
        albumList = new ArrayList<>();
        getAlbums();
        Collections.sort(albumList, new Comparator<Album>() {
            public int compare(Album a, Album b) {
                return a.getTitle().toLowerCase().compareTo(b.getTitle().toLowerCase());
            }
        });
        AlbumAdapter albumAdt = new AlbumAdapter(this, albumList, false);
        albumGridView.setAdapter(albumAdt);

        albumListView = (ListView)findViewById(R.id.album_list);
        albumAdt = new AlbumAdapter(this, albumList, true);
        albumListView.setAdapter(albumAdt);

        albumSwitcher = (ViewSwitcher)findViewById(R.id.album_switcher);

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
            //TODO: Find out if this is needed or not
            //int idList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums._ID);
            int titleList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM);
            int albumIdList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_ID);
            int artistList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ARTIST);

            boolean next = true;
            while (next) {
                //String realId = albumCursor.getString(idList);
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
        openPlayer();
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
        musicSrv.setSong((int)Math.floor(Math.random()*playList.size()));
        musicSrv.playSong();
        openPlayer();
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
                //open id3 editor
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

    public void switchAlbumView(View view) {
        if (albumIsGrid) {
            albumListView.setSelection(0);
            albumSwitcher.setDisplayedChild(1);
            TextView v = (TextView)findViewById(R.id.album_view_mode);
            v.setText(R.string.list_mode);
            albumIsGrid = false;
        }
        else {
            albumGridView.setSelection(0);
            TextView v = (TextView)findViewById(R.id.album_view_mode);
            v.setText(R.string.grid_mode);
            albumSwitcher.setDisplayedChild(0);
            albumIsGrid = true;
        }
    }

    public void openDrawer(View view) {
        DrawerLayout nav = (DrawerLayout)findViewById(R.id.drawer_layout);
        nav.openDrawer(Gravity.LEFT);
    }

    public void shuffleAll(View view) {
        musicSrv.setShuffle(true);
        playList.addAll(songList);
        musicSrv.setList(playList);
        musicSrv.setSong((int)Math.floor(Math.random()*songList.size()));
        musicSrv.playSong();
        openPlayer();
    }

    public void openPlayer() {
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }
    public void openPlayer(View view) {
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }
}
