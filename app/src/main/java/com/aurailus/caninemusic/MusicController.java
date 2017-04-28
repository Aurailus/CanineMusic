package com.aurailus.caninemusic;

import android.content.Context;
import android.view.View;
import android.widget.MediaController;

public class MusicController extends MediaController {

    public MusicController(Context c) {
        super(c);
    }
    public void hide() {
        //prevent popup from hiding
    }
}
