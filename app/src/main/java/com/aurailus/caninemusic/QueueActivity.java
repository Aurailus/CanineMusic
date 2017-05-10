package com.aurailus.caninemusic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class QueueActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
    }

    public void back(View view) {
        this.finish();
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

}
