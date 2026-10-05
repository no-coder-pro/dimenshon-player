package com.spatial.sound8dplayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {
    private static final String PREFS_NAME = "aura_favorites_prefs";
    private static final String KEY_FAVORITES = "fav_song_ids";

    private static FavoritesManager instance;
    private final SharedPreferences prefs;
    private final Set<Long> favoriteIds = new HashSet<>();

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context.getApplicationContext());
        }
        return instance;
    }

    public FavoritesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        reloadFavorites();
    }

    public synchronized void reloadFavorites() {
        Set<String> saved = prefs.getStringSet(KEY_FAVORITES, null);
        favoriteIds.clear();
        if (saved != null) {
            for (String s : saved) {
                try {
                    favoriteIds.add(Long.parseLong(s));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public synchronized boolean isFavorite(long songId) {
        return favoriteIds.contains(songId);
    }

    public synchronized boolean toggleFavorite(long songId) {
        boolean isFavNow;
        if (favoriteIds.contains(songId)) {
            favoriteIds.remove(songId);
            isFavNow = false;
        } else {
            favoriteIds.add(songId);
            isFavNow = true;
        }
        saveFavorites();
        return isFavNow;
    }

    public synchronized void removeFavorite(long songId) {
        if (favoriteIds.contains(songId)) {
            favoriteIds.remove(songId);
            saveFavorites();
        }
    }

    private void saveFavorites() {
        Set<String> stringSet = new HashSet<>(favoriteIds.size());
        for (Long id : favoriteIds) {
            stringSet.add(String.valueOf(id));
        }
        prefs.edit().putStringSet(KEY_FAVORITES, stringSet).apply();
    }

    public synchronized Set<String> getFavoriteIds() {
        Set<String> stringSet = new HashSet<>(favoriteIds.size());
        for (Long id : favoriteIds) {
            stringSet.add(String.valueOf(id));
        }
        return stringSet;
    }
}
