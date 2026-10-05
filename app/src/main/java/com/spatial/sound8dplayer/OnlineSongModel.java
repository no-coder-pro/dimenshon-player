package com.spatial.sound8dplayer;

import java.util.Locale;
import java.util.Objects;

public class OnlineSongModel {
    private final String id;
    private final String title;
    private final String artist;
    private final String album;
    private final String url;
    private final String thumbnailUrl;
    private final String durationStr;
    private final long durationSec;
    private final String source;
    private final String views;
    private final String published;

    public OnlineSongModel(String id, String title, String artist, String album,
                           String url, String thumbnailUrl, String durationStr,
                           long durationSec, String source, String views, String published) {
        this.id = id != null ? id : "";
        this.title = title != null ? title : "Unknown Title";
        this.artist = artist != null ? artist : "Unknown Artist";
        this.album = album != null ? album : "";
        this.url = url != null ? url : "";
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
        this.durationStr = durationStr != null ? durationStr : "";
        this.durationSec = durationSec;
        this.source = source != null ? source : "youtube";
        this.views = views != null ? views : "";
        this.published = published != null ? published : "";
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getUrl() { return url; }
    public String getThumbnailUrl() { return thumbnailUrl; }

    public String getDurationStr() {
        if (durationStr != null && !durationStr.isEmpty()) return durationStr;
        if (durationSec > 0) {
            long min = durationSec / 60;
            long sec = durationSec % 60;
            return String.format(Locale.US, "%d:%02d", min, sec);
        }
        return "";
    }

    public long getDurationSec() { return durationSec; }
    public String getSource() { return source; }
    public String getViews() { return views; }
    public String getPublished() { return published; }
    public boolean isSpotify() { return "spotify".equalsIgnoreCase(source); }
    public boolean isYouTube() { return "youtube".equalsIgnoreCase(source); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnlineSongModel that = (OnlineSongModel) o;
        if (!id.isEmpty() && id.equals(that.id)) return true;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.isEmpty() ? url : id);
    }
}
