package com.spatial.sound8dplayer;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnlineMusicApiService {

    private static final String YT_BASE_URL = "https://yt.islamraisul796.workers.dev";
    private static final String SPOTIFY_BASE_URL = "https://spotify.islamraisul796.workers.dev";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Spatial8DPlayer/1.0";

    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public static class DownloadResult {
        public String title;
        public String artist;
        public String filename;
        public String downloadUrl;
        public String thumbnailUrl;
        public String quality;
        public String source;
        public long durationSec;

        public DownloadResult(String title, String artist, String filename, String downloadUrl,
                              String thumbnailUrl, String quality, String source, long durationSec) {
            this.title = title;
            this.artist = artist;
            this.filename = filename;
            this.downloadUrl = downloadUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.quality = quality;
            this.source = source;
            this.durationSec = durationSec;
        }
    }

    public OnlineMusicApiService() {
        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static String extractUrlFromText(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        Pattern pattern = Pattern.compile("(https?://\\S+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[)>}\\]\"']+$", "");
        }
        return null;
    }

    public static String detectUrlSource(String url) {
        if (url == null) return null;
        String lower = url.toLowerCase();
        if (lower.contains("spotify.com") || lower.contains("spotify.link")) {
            return "spotify";
        }
        if (lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("music.youtube.com")) {
            return "youtube";
        }
        return null;
    }

    public void fetchUrlMetadata(String url, ApiCallback<OnlineSongModel> callback) {
        executorService.execute(() -> {
            try {
                String source = detectUrlSource(url);
                if (source == null) source = "youtube";

                String encodedQ = URLEncoder.encode(url, StandardCharsets.UTF_8.name());

                if ("spotify".equalsIgnoreCase(source)) {
                    String endpoint = SPOTIFY_BASE_URL + "/api/search?q=" + encodedQ;
                    String jsonStr = fetchHttpString(endpoint);
                    JSONObject root = new JSONObject(jsonStr);

                    String id = root.optString("id", "");
                    String title = root.optString("title", "");
                    String artist = root.optString("artist", "Spotify Artist");
                    String album = root.optString("album", "");
                    String trackUrl = root.optString("url", url);
                    String thumbnail = root.optString("thumbnail", "");
                    long duration = root.optLong("duration", 0);

                    if (title.isEmpty()) {
                        JSONArray results = root.optJSONArray("results");
                        if (results != null && results.length() > 0) {
                            JSONObject obj = results.getJSONObject(0);
                            id = obj.optString("id", "");
                            title = obj.optString("title", "Spotify Track");
                            artist = obj.optString("artist", "Spotify Artist");
                            album = obj.optString("album", "");
                            trackUrl = obj.optString("url", url);
                            thumbnail = obj.optString("thumbnail", "");
                            duration = obj.optLong("duration", 0);
                        }
                    }

                    if (title.isEmpty()) title = "Spotify Track";

                    OnlineSongModel song = new OnlineSongModel(
                            id, title, artist, album, trackUrl, thumbnail,
                            "", duration, "spotify", "", ""
                    );
                    mainHandler.post(() -> callback.onSuccess(song));

                } else {
                    String endpoint = YT_BASE_URL + "/api/search?q=" + encodedQ + "&limit=1";
                    String jsonStr = fetchHttpString(endpoint);
                    JSONObject root = new JSONObject(jsonStr);

                    JSONArray results = root.optJSONArray("results");
                    if (results != null && results.length() > 0) {
                        JSONObject obj = results.getJSONObject(0);
                        String id = obj.optString("id", "");
                        String title = obj.optString("title", "YouTube Audio");
                        String channel = obj.optString("channel", "YouTube");
                        String trackUrl = obj.optString("url", url);
                        String thumbnail = obj.optString("thumbnail", "");
                        String durationStr = obj.optString("duration", "");

                        OnlineSongModel song = new OnlineSongModel(
                                id, title, channel, "", trackUrl, thumbnail,
                                durationStr, 0, "youtube", "", ""
                        );
                        mainHandler.post(() -> callback.onSuccess(song));
                    } else {
                        String infoEndpoint = YT_BASE_URL + "/api/info?url=" + encodedQ;
                        String infoJson = fetchHttpString(infoEndpoint);
                        JSONObject infoObj = new JSONObject(infoJson);
                        String id = infoObj.optString("id", "");
                        String title = infoObj.optString("title", "YouTube Audio");
                        String channel = infoObj.optString("channel", "YouTube");
                        String thumbnail = infoObj.optString("thumbnail", "");

                        OnlineSongModel song = new OnlineSongModel(
                                id, title, channel, "", url, thumbnail,
                                "", 0, "youtube", "", ""
                        );
                        mainHandler.post(() -> callback.onSuccess(song));
                    }
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Failed to fetch track info: " + e.getMessage()));
            }
        });
    }

    public static long parseDurationStringToSec(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) return 0;
        try {
            String[] parts = durationStr.trim().split(":");
            if (parts.length == 2) {
                long min = Long.parseLong(parts[0].trim());
                long sec = Long.parseLong(parts[1].trim());
                return min * 60 + sec;
            } else if (parts.length == 3) {
                long hr = Long.parseLong(parts[0].trim());
                long min = Long.parseLong(parts[1].trim());
                long sec = Long.parseLong(parts[2].trim());
                return hr * 3600 + min * 60 + sec;
            } else if (parts.length == 1) {
                return Long.parseLong(parts[0].trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public void searchYoutube(String query, int limit, ApiCallback<List<OnlineSongModel>> callback) {
        executorService.execute(() -> {
            try {
                String encodedQ = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                String endpoint = YT_BASE_URL + "/api/search?q=" + encodedQ + "&limit=" + limit;
                String jsonStr = fetchHttpString(endpoint);
                JSONObject root = new JSONObject(jsonStr);

                List<OnlineSongModel> results = new ArrayList<>();
                JSONArray items = root.optJSONArray("results");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject obj = items.getJSONObject(i);
                        String id = obj.optString("id", "");
                        String title = obj.optString("title", "Unknown");
                        String channel = obj.optString("channel", "YouTube");
                        String itemUrl = obj.optString("url", "https://www.youtube.com/watch?v=" + id);
                        String thumbnail = obj.optString("thumbnail", "");
                        String duration = obj.optString("duration", "");
                        long durationSec = parseDurationStringToSec(duration);
                        String views = obj.optString("views", "");
                        String published = obj.optString("published", "");

                        results.add(new OnlineSongModel(
                                id, title, channel, "", itemUrl, thumbnail,
                                duration, durationSec, "youtube", views, published
                        ));
                    }
                }

                mainHandler.post(() -> callback.onSuccess(results));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("YouTube Search failed: " + e.getMessage()));
            }
        });
    }

    public void searchSpotify(String query, int limit, ApiCallback<List<OnlineSongModel>> callback) {
        executorService.execute(() -> {
            try {
                String encodedQ = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                String endpoint = SPOTIFY_BASE_URL + "/api/search?q=" + encodedQ + "&limit=" + limit;
                String jsonStr = fetchHttpString(endpoint);
                JSONObject root = new JSONObject(jsonStr);

                List<OnlineSongModel> results = new ArrayList<>();
                JSONArray items = root.optJSONArray("results");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject obj = items.getJSONObject(i);
                        String id = obj.optString("id", "");
                        String title = obj.optString("title", "Unknown");
                        String artist = obj.optString("artist", "Spotify Artist");
                        String album = obj.optString("album", "");
                        String itemUrl = obj.optString("url", "https://open.spotify.com/track/" + id);
                        String thumbnail = obj.optString("thumbnail", "");
                        long durationSec = obj.optLong("duration", 0);

                        results.add(new OnlineSongModel(
                                id, title, artist, album, itemUrl, thumbnail,
                                "", durationSec, "spotify", "", ""
                        ));
                    }
                }

                mainHandler.post(() -> callback.onSuccess(results));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Spotify Search failed: " + e.getMessage()));
            }
        });
    }

    public void fetchYoutubeDownload(String urlOrId, String quality, ApiCallback<DownloadResult> callback) {
        executorService.execute(() -> {
            try {
                String q = (quality != null && !quality.isEmpty()) ? quality : "128";
                String encodedUrl = URLEncoder.encode(urlOrId, StandardCharsets.UTF_8.name());
                String endpoint = YT_BASE_URL + "/api/download?url=" + encodedUrl + "&format=mp3&quality=" + q;

                String jsonStr = fetchHttpString(endpoint);
                JSONObject root = new JSONObject(jsonStr);

                String tunnelUrl = root.optString("tunnel_url", "");
                if (tunnelUrl.isEmpty()) {
                    String err = root.optString("error", "Failed to obtain download URL from server");
                    throw new Exception(err);
                }

                String title = root.optString("title", "YouTube Audio");
                String filename = root.optString("filename", title + ".mp3");
                long duration = root.optLong("duration", 0);
                String videoId = root.optString("video_id", "");
                String thumbnail = videoId.isEmpty() ? "" : "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";

                DownloadResult result = new DownloadResult(
                        title, "YouTube Audio", filename, tunnelUrl,
                        thumbnail, q, "youtube", duration
                );

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("YouTube Download resolution error: " + e.getMessage()));
            }
        });
    }

    public void fetchSpotifyDownload(String spotifyUrl, ApiCallback<DownloadResult> callback) {
        fetchSpotifyDownload(spotifyUrl, null, callback);
    }

    public void fetchSpotifyDownload(String spotifyUrl, String fallbackQuery, ApiCallback<DownloadResult> callback) {
        executorService.execute(() -> {
            try {
                String encodedUrl = URLEncoder.encode(spotifyUrl, StandardCharsets.UTF_8.name());
                String endpoint = SPOTIFY_BASE_URL + "/api/download?url=" + encodedUrl;

                try {
                    String jsonStr = fetchHttpString(endpoint);
                    JSONObject root = new JSONObject(jsonStr);

                    String downloadLink = root.optString("download_link", "");
                    if (!downloadLink.isEmpty()) {
                        String title = root.optString("title", "Spotify Track");
                        String artist = root.optString("artist", "Spotify Artist");
                        String filename = root.optString("filename", title + " - " + artist + ".mp3");
                        String thumbnail = root.optString("thumbnail", "");
                        long duration = root.optLong("duration", 0);

                        DownloadResult result = new DownloadResult(
                                title, artist, filename, downloadLink,
                                thumbnail, "320", "spotify", duration
                        );

                        mainHandler.post(() -> callback.onSuccess(result));
                        return;
                    }
                } catch (Exception ignored) {}

                String queryToUse = (fallbackQuery != null && !fallbackQuery.trim().isEmpty()) ? fallbackQuery : null;
                if (queryToUse == null) {
                    try {
                        String metaEndpoint = SPOTIFY_BASE_URL + "/api/search?q=" + encodedUrl;
                        String metaJson = fetchHttpString(metaEndpoint);
                        JSONObject metaRoot = new JSONObject(metaJson);
                        String t = metaRoot.optString("title", "");
                        String a = metaRoot.optString("artist", "");
                        if (!t.isEmpty()) {
                            queryToUse = t + " " + a;
                        }
                    } catch (Exception ignored) {}
                }

                if (queryToUse != null && !queryToUse.trim().isEmpty()) {
                    fetchYoutubeDownload(queryToUse, "128", callback);
                } else {
                    throw new Exception("Spotify server is temporarily busy, please retry in a moment.");
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Spotify resolution error: " + e.getMessage()));
            }
        });
    }

    private String fetchHttpString(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(35000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code + ": " + sb.toString());
            }

            return sb.toString();
        } finally {
            if (reader != null) try { reader.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }
}
