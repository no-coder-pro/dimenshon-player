package com.spatial.sound8dplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class SongMetadataManager {
    private static final String PREF_NAME = "aura_song_metadata_prefs";
    private static final String KEY_TITLE = "custom_title_";
    private static final String KEY_ARTIST = "custom_artist_";
    private static final String KEY_ALBUM = "custom_album_";

    private final SharedPreferences prefs;

    public SongMetadataManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveMetadata(long songId, String title, String artist, String album) {
        SharedPreferences.Editor editor = prefs.edit();
        if (title != null && !title.trim().isEmpty()) {
            editor.putString(KEY_TITLE + songId, title.trim());
        }
        if (artist != null && !artist.trim().isEmpty()) {
            editor.putString(KEY_ARTIST + songId, artist.trim());
        }
        if (album != null && !album.trim().isEmpty()) {
            editor.putString(KEY_ALBUM + songId, album.trim());
        }
        editor.apply();
    }

    public String getTitle(long songId, String defaultTitle) {
        return prefs.getString(KEY_TITLE + songId, defaultTitle);
    }

    public String getArtist(long songId, String defaultArtist) {
        return prefs.getString(KEY_ARTIST + songId, defaultArtist);
    }

    public String getAlbum(long songId, String defaultAlbum) {
        return prefs.getString(KEY_ALBUM + songId, defaultAlbum);
    }

    public void applySavedMetadata(MusicModel song) {
        if (song == null) return;
        long id = song.getId();
        String savedTitle = prefs.getString(KEY_TITLE + id, null);
        if (savedTitle != null) {
            song.setTitle(savedTitle);
        }
        String savedArtist = prefs.getString(KEY_ARTIST + id, null);
        if (savedArtist != null) {
            song.setArtist(savedArtist);
        }
        String savedAlbum = prefs.getString(KEY_ALBUM + id, null);
        if (savedAlbum != null) {
            song.setAlbum(savedAlbum);
        }
    }
}
