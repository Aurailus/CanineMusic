package com.aurailus.caninemusic;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.aurailus.caninemusic.MusicService.*;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String STATE_PLAYLIST = "playList";
    private static final String STATE_VIEW = "curView";
    private ArrayList<Song> songList;
    private ArrayList<Song> playList;
    private ArrayList<Album> albumList;
    private ViewSwitcher albumSwitcher;
    private PageView currentView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean albumIsGrid = true;
    private boolean musicBound = false;
    private short permReqCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*Begin Create Event*/

        //Basic stuffs
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request Permission at Runtime because Android Sucks
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return;
            }
        }

        //Initialize ArrayLists
        songList = new ArrayList<>();
        playList = new ArrayList<>();
        findSongs();

        //Get Songs
        Collections.sort(songList, new Comparator<Song>() {
            public int compare(Song a, Song b) {
                return a.getTitle().toLowerCase().compareTo(b.getTitle().toLowerCase());
            }
        });

        //Initialize Albums
        albumList = new ArrayList<>();
        findAlbums();
        Collections.sort(albumList, new Comparator<Album>() {
            public int compare(Album a, Album b) {
                return a.getTitle().toLowerCase().compareTo(b.getTitle().toLowerCase());
            }
        });

        initializePlaylist(savedInstanceState);
        if (savedInstanceState == null) {
            currentView = PageView.ALBUMS;
        }
        else {
            currentView = PageView.atPosition(savedInstanceState.getInt(STATE_VIEW));
            ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
            flipper.setDisplayedChild(currentView.getPosition());
        }

        //Connect to MusicService
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }

        //Receive Broadcasts for Toolbar Song Details
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("musicPrepared"));

        /*View functions*/
        ListView songView = (ListView)findViewById(R.id.song_list);
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        ExpandableHeightGridView albumGridView = (ExpandableHeightGridView) findViewById(R.id.album_grid);
        AlbumAdapter albumAdt = new AlbumAdapter(this, albumList, false);
        albumGridView.setAdapter(albumAdt);
        albumGridView.expand();
        //albumGridView.setFocusable(false);

        ExpandableHeightListView albumListView = (ExpandableHeightListView) findViewById(R.id.album_list);
        albumAdt = new AlbumAdapter(this, albumList, true);
        albumListView.setAdapter(albumAdt);
        albumListView.expand();
        //albumListView.setFocusable(false);

        albumSwitcher = (ViewSwitcher)findViewById(R.id.album_switcher);

        //Navigation
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        BottomNavigationView bottomNav = (BottomNavigationView) findViewById(R.id.navigation);
        BottomNavigationViewHelper.disableShiftMode(bottomNav);

        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectSection(item);
                return true;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
        case 1:
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                if (permReqCount > 0) {
                    finish();
                    System.exit(0);
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.alert_permission_title).setMessage(R.string.alert_permission_content).setCancelable(true).setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                                    permReqCount++;
                                }
                            }
                        });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initializePlaylist(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            /*Returning from orientation change or extended close time*/
            playList = (ArrayList)savedInstanceState.getSerializable(STATE_PLAYLIST);
        }
        else {
            /*Starting app for the first time*/
            playList.addAll(songList);
        }
    }

    //Set the musicBound variable
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            musicSrv = binder.getService();
            musicSrv.setList(playList);
            musicBound = true;

            if (musicSrv.isPlaying()) {
                setToolbarContent();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    private void selectSection(MenuItem item) {
        ViewFlipper flipper = (ViewFlipper)findViewById(R.id.flipper);
        switch (item.getItemId()) {
            case (R.id.nav_album):
                currentView = PageView.ALBUMS;
                break;
            case (R.id.nav_playlist):
                currentView = PageView.PLAYLISTS;
                break;
            case (R.id.nav_track):
                currentView = PageView.TRACKS;
                break;
            case (R.id.nav_artist):
                currentView = PageView.ARTISTS;
                break;
            case (R.id.nav_genre):
                currentView = PageView.GENRES;
                break;
        }
        flipper.setDisplayedChild(currentView.getPosition());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(outState);
        // Save our own state now
        outState.putSerializable(STATE_PLAYLIST, playList);
        outState.putSerializable(STATE_VIEW, currentView);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setToolbarContent();
        }
    };

    private void setToolbarContent() {
        TextView appTitle = (TextView)findViewById(R.id.app_title);
        ConstraintLayout songDetails = (ConstraintLayout)findViewById(R.id.playing_details);

        //Hide the app title and start displaying song info
        if (appTitle.getVisibility() == View.VISIBLE) {
            appTitle.setVisibility(View.INVISIBLE);
            songDetails.setVisibility(View.VISIBLE);
        }

        TextView mainTitle = (TextView)findViewById(R.id.current_title);
        TextView mainArtist = (TextView)findViewById(R.id.current_artist);

        mainTitle.setText(musicSrv.getTitle());
        mainArtist.setText(musicSrv.getArtist());

        ImageView albumView = (ImageView)findViewById(R.id.current_albumart);
        String albumId = musicSrv.getAlbumId();

        Bitmap roundedBmp;
        Drawable albumArt = ImageHelper.findAlbumArtById(albumId, this.getBaseContext());
        if (albumArt != null) {
            albumView.setImageDrawable(albumArt);

            //Rounded Bitmap for Notification
            roundedBmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(roundedBmp);
            albumArt.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            albumArt.draw(canvas);
        }
        else {
            albumView.setImageResource(R.drawable.ic_album_unknown);
            roundedBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_album_unknown);
        }

        musicSrv.updateNotification(roundedBmp);

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        if (isFinishing()) {
            stopService(playIntent);
            unbindService(musicConnection);
            musicSrv = null;
        }

        super.onDestroy();
    }

    private void findSongs() {
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

    private void findAlbums() {
        ContentResolver albumResolver = getContentResolver();
        Uri albumUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[]columns = {android.provider.MediaStore.Audio.Albums._ID, android.provider.MediaStore.Audio.Albums.ALBUM_ID,
                android.provider.MediaStore.Audio.Albums.ALBUM, android.provider.MediaStore.Audio.Albums.ARTIST};

        Cursor albumCursor = albumResolver.query(albumUri, columns, null, null, null);

        if (albumCursor != null && albumCursor.moveToFirst()) {
            int titleList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM);
            int albumIdList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ALBUM_ID);
            int artistList = albumCursor.getColumnIndex(android.provider.MediaStore.Audio.Albums.ARTIST);

            boolean next = true;
            while (next) {
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

    //TODO: Find out why this method cant play FLAC files but the ones in MusicService can
    public void chooseSong(View view) {
        playList.clear();
        playList.addAll(songList);
        musicSrv.setList(playList);
        musicSrv.setSong(Integer.parseInt(view.getTag(R.id.index_id).toString()));
        musicSrv.playSong();

        openPlayer();
    }

    public void chooseAlbum(View view) {
        if (musicBound) {
            playList.clear();
            System.out.println(view.getTag(R.id.index_id));
            for (Song song : songList) {
                if (song.getAlbumId().equals(view.getTag())) {
                    playList.add(song);
                }
            }
            musicSrv.setList(playList);
            musicSrv.setSong((int) Math.floor(Math.random() * playList.size()));
            musicSrv.playSong();

            openPlayer();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            this.moveTaskToBack(true);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case (R.id.add_pin):
                //TODO: Repurpose for adding stuff to nav list
                /*NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
                MenuItem it = navigationView.getMenu().findItem(R.id.pinned_items);
                it.getSubMenu().add(0, 0, 0, "C418").setIcon(R.drawable.ic_jumble);*/
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alert_pinned_title).setMessage(R.string.alert_pinned_content).setCancelable(true).setPositiveButton("Ok",null);
                AlertDialog dialog = builder.create();
                dialog.show();
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

    @SuppressWarnings("UnusedParameters")
    public void switchAlbumView(View view) {
        if (albumIsGrid) {
            ScrollView s = (ScrollView)findViewById(R.id.grid_scrollview);
            s.fullScroll(ScrollView.FOCUS_UP);
            s = (ScrollView)findViewById(R.id.list_scrollview);
            s.fullScroll(ScrollView.FOCUS_UP);
            albumSwitcher.setDisplayedChild(1);
            albumIsGrid = false;
        }
        else {
            ScrollView s = (ScrollView)findViewById(R.id.grid_scrollview);
            s.fullScroll(ScrollView.FOCUS_UP);
            s = (ScrollView)findViewById(R.id.list_scrollview);
            s.fullScroll(ScrollView.FOCUS_UP);
            albumSwitcher.setDisplayedChild(0);
            albumIsGrid = true;
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void openDrawer(View view) {
        DrawerLayout nav = (DrawerLayout)findViewById(R.id.drawer_layout);
        nav.openDrawer(Gravity.START);
    }

    @SuppressWarnings("UnusedParameters")
    public void shuffleAll(View view) {
        if (musicBound) {
            musicSrv.setShuffle(true);
            playList.clear();
            playList.addAll(songList);
            musicSrv.setList(playList);
            musicSrv.setSong((int) Math.floor(Math.random() * playList.size()));
            musicSrv.playSong();

            openPlayer();
        }
    }

    private void openPlayer() {
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }

    @SuppressWarnings("UnusedParameters")
    public void openPlayer(View view) {
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }
}
