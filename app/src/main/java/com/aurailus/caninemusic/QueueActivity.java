package com.aurailus.caninemusic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

public class QueueActivity extends AppCompatActivity {

    private static final String STATE_QUEUE = "com.aurailus.caninemusic.QUEUE";
    private ArrayList<Song> queue;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        queue = (ArrayList<Song>)getIntent().getSerializableExtra(STATE_QUEUE);
        if (queue != null) {
            /*View functions*/
            ListView songView = (ListView)findViewById(R.id.song_list);
            SongAdapter songAdt = new SongAdapter(this, queue);
            songView.setAdapter(songAdt);
        }
    }

    @Override
    public void onBackPressed() {
        back(null);
    }

    @SuppressWarnings("UnusedParameters")
    public void back(View view) {
        this.finish();
        overridePendingTransition(R.anim.slide_fleft_small, R.anim.slide_tleft_large);
    }
}
