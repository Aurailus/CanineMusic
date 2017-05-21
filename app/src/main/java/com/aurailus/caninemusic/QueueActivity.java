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

    @Override
    public void onBackPressed() {
        back(null);
    }

    @SuppressWarnings("UnusedParameters")
    public void back(View view) {
        this.finish();
        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
    }
}
