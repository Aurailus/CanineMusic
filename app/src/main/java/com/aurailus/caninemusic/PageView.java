package com.aurailus.caninemusic;

enum PageView {
    ALBUMS(0), PLAYLISTS(1), TRACKS(2), ARTISTS(3), GENRES(4);

    private final int position;

    PageView(final int position) {
        this.position = position;
    }

    public int getPosition() {
        return this.position;
    }

    public static PageView atPosition(int pos) {
        for (PageView cur : PageView.values()) {
            if (cur.getPosition() == pos) return cur;
        }
        return null;
    }
}
