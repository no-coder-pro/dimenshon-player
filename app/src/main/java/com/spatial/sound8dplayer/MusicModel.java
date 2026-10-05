package com.spatial.sound8dplayer;

import android.net.Uri;
import java.net.URLDecoder;
import java.util.Objects;

public class MusicModel {
    private final long id;
    private String title;
    private String artist;
    private String album;
    private final String duration;
    private final long durationMs;
    private final long size;
    private final long dateAdded;
    private final String dataPath;
    private final Uri uri;

    private String normalizedSearchKey = "";
    private String normalizedSearchKeyNoSpace = "";

    public MusicModel(long id, String title, String artist, String duration, Uri uri) {
        this(id, title, artist, "Unknown Album", duration, 0L, 0L, 0L, "", uri);
    }

    public MusicModel(long id, String title, String artist, String duration, long durationMs, long size, long dateAdded, String dataPath, Uri uri) {
        this(id, title, artist, "Unknown Album", duration, durationMs, size, dateAdded, dataPath, uri);
    }

    public MusicModel(long id, String title, String artist, String album, String duration, long durationMs, long size, long dateAdded, String dataPath, Uri uri) {
        this.id = id;
        this.title = decodeText(title, "Unknown Track");
        this.artist = decodeText(artist, "Unknown Artist");
        this.album = (album != null && !album.trim().isEmpty() && !album.equalsIgnoreCase("<unknown>")) ? album.trim() : "Unknown Album";
        this.duration = duration != null ? duration : "0:00";
        this.durationMs = durationMs;
        this.size = size;
        this.dateAdded = dateAdded;
        this.dataPath = dataPath != null ? dataPath : "";
        this.uri = uri;
        updateSearchKeys();
    }

    public void updateSearchKeys() {
        StringBuilder raw = new StringBuilder(128);
        if (title != null) raw.append(title).append(' ');
        if (artist != null) raw.append(artist).append(' ');
        if (album != null) raw.append(album).append(' ');
        if (dataPath != null) raw.append(dataPath);

        String text = raw.toString();
        if (text.contains("%")) {
            try {
                text = URLDecoder.decode(text, "UTF-8");
            } catch (Exception ignored) {}
        }

        StringBuilder clean = new StringBuilder(text.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                clean.append(Character.toLowerCase(c));
                lastWasSpace = false;
            } else {
                if (!lastWasSpace) {
                    clean.append(' ');
                    lastWasSpace = true;
                }
            }
        }
        this.normalizedSearchKey = clean.toString().trim();
        this.normalizedSearchKeyNoSpace = this.normalizedSearchKey.replace(" ", "");
    }

    public String getNormalizedSearchKey() {
        return normalizedSearchKey;
    }

    public String getNormalizedSearchKeyNoSpace() {
        return normalizedSearchKeyNoSpace;
    }

    private static String decodeText(String text, String defaultText) {
        if (text == null || text.trim().isEmpty() || text.equalsIgnoreCase("<unknown>")) {
            return defaultText;
        }
        try {
            return URLDecoder.decode(text, "UTF-8").trim();
        } catch (Exception e) {
            return text.trim();
        }
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
            updateSearchKeys();
        }
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        if (artist != null && !artist.trim().isEmpty()) {
            this.artist = artist.trim();
            updateSearchKeys();
        }
    }

    public String getAlbum() {
        return album != null ? album : "Unknown Album";
    }

    public void setAlbum(String album) {
        if (album != null && !album.trim().isEmpty()) {
            this.album = album.trim();
            updateSearchKeys();
        }
    }

    public String getDuration() {
        return duration;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getSize() {
        return size;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public String getDataPath() {
        return dataPath;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicModel that = (MusicModel) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
