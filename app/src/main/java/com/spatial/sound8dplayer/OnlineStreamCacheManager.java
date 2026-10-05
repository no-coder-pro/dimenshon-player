package com.spatial.sound8dplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineStreamCacheManager {

    private static OnlineStreamCacheManager instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ConcurrentHashMap<String, Boolean> activeDownloads = new ConcurrentHashMap<>();

    private static final long MAX_CACHE_SIZE_BYTES = 120 * 1024 * 1024;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Spatial8DPlayer/1.0";

    public interface CacheCallback {
        void onSuccess(File cachedAudioFile);
        void onError(String errorMessage);
    }

    private OnlineStreamCacheManager() {}

    public static synchronized OnlineStreamCacheManager getInstance() {
        if (instance == null) {
            instance = new OnlineStreamCacheManager();
        }
        return instance;
    }

    private File getCacheFolder(Context context) {
        File folder = new File(context.getCacheDir(), "stream_cache");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private String sanitizeFilename(String id) {
        if (id == null) return "stream_" + System.currentTimeMillis();
        return id.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    public void getCachedAudio(Context context, String songId, String streamUrl, CacheCallback callback) {
        if (streamUrl == null || streamUrl.trim().isEmpty()) {
            mainHandler.post(() -> callback.onError("Invalid stream URL"));
            return;
        }

        File cacheDir = getCacheFolder(context);
        String cleanId = sanitizeFilename(songId);
        File targetFile = new File(cacheDir, cleanId + ".mp3");

        if (targetFile.exists() && targetFile.length() > 50 * 1024) {
            mainHandler.post(() -> callback.onSuccess(targetFile));
            return;
        }

        executor.execute(() -> {
            HttpURLConnection conn = null;
            InputStream in = null;
            FileOutputStream out = null;
            File tempFile = new File(cacheDir, cleanId + ".tmp");

            try {
                activeDownloads.put(songId, true);

                URL url = new URL(streamUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(25000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Accept", "*/*");
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    String newUrl = conn.getHeaderField("Location");
                    if (newUrl != null && !newUrl.isEmpty()) {
                        conn.disconnect();
                        url = new URL(newUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(25000);
                        conn.setReadTimeout(30000);
                        conn.setRequestProperty("User-Agent", USER_AGENT);
                        conn.connect();
                        responseCode = conn.getResponseCode();
                    }
                }

                if (responseCode < 200 || responseCode >= 300) {
                    throw new Exception("HTTP " + responseCode + " while caching stream");
                }

                in = conn.getInputStream();
                out = new FileOutputStream(tempFile);

                byte[] buffer = new byte[16 * 1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.flush();
                out.close();
                out = null;
                in.close();
                in = null;

                if (tempFile.length() > 10 * 1024) {
                    if (targetFile.exists()) targetFile.delete();
                    tempFile.renameTo(targetFile);

                    trimCacheIfNeeded(cacheDir);

                    mainHandler.post(() -> callback.onSuccess(targetFile));
                } else {
                    throw new Exception("Downloaded file is too small or invalid");
                }

            } catch (Exception e) {
                if (tempFile.exists()) tempFile.delete();
                mainHandler.post(() -> callback.onError("Stream caching failed: " + e.getMessage()));
            } finally {
                activeDownloads.remove(songId);
                if (out != null) try { out.close(); } catch (Exception ignored) {}
                if (in != null) try { in.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void trimCacheIfNeeded(File cacheDir) {
        try {
            File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(".mp3"));
            if (files == null) return;

            long totalSize = 0;
            for (File f : files) totalSize += f.length();

            if (totalSize > MAX_CACHE_SIZE_BYTES) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (File f : files) {
                    if (totalSize <= MAX_CACHE_SIZE_BYTES * 0.7) break;
                    long len = f.length();
                    if (f.delete()) {
                        totalSize -= len;
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
