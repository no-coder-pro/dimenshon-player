package com.spatial.sound8dplayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SongDownloadHelper {

    private static final String CHANNEL_ID = "song_downloads_channel";
    private static final String CHANNEL_NAME = "Song Downloads";
    private static final ExecutorService downloadExecutor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface OnDownloadCompletedListener {
        void onDownloadCompleted(String songTitle, String filePath);
    }

    private static OnDownloadCompletedListener globalListener;

    public static void setGlobalDownloadListener(OnDownloadCompletedListener listener) {
        globalListener = listener;
    }

    public static void startDownload(Context context, String downloadUrl, String suggestedFilename,
                                     String songTitle, String artist, String source) {
        if (context == null || downloadUrl == null || downloadUrl.trim().isEmpty()) {
            return;
        }

        final Context appContext = context.getApplicationContext();
        final String cleanName = sanitizeFilename(suggestedFilename);
        final String displayTitle = (songTitle != null && !songTitle.isEmpty()) ? songTitle : cleanName;
        final String displayArtist = (artist != null && !artist.isEmpty()) ? artist : "Music Download";
        final int notificationId = (int) (System.currentTimeMillis() % 100000);

        createNotificationChannel(appContext);

        Toast.makeText(appContext, "⚡ Download started: " + displayTitle, Toast.LENGTH_SHORT).show();

        NotificationManager notificationManager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Downloading: " + displayTitle)
                .setContentText("0% • " + displayArtist)
                .setProgress(100, 0, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, notifBuilder.build());
        }

        downloadExecutor.execute(() -> {
            HttpURLConnection conn = null;
            InputStream is = null;
            BufferedInputStream bis = null;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;
            File tempFile = null;
            File finalFile = null;

            try {
                File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                if (!musicDir.exists()) {
                    musicDir.mkdirs();
                }

                String targetFilename = cleanName;
                if (!targetFilename.toLowerCase().endsWith(".mp3")) {
                    targetFilename += ".mp3";
                }

                finalFile = new File(musicDir, targetFilename);
                if (finalFile.exists()) {
                    targetFilename = System.currentTimeMillis() + "_" + targetFilename;
                    finalFile = new File(musicDir, targetFilename);
                }

                tempFile = new File(musicDir, targetFilename + ".downloading");

                URL url = new URL(downloadUrl.trim());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.connect();

                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
                    String redirectUrl = conn.getHeaderField("Location");
                    if (redirectUrl != null) {
                        conn.disconnect();
                        url = new URL(redirectUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(30000);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                        conn.setRequestProperty("Accept", "*/*");
                        conn.connect();
                    }
                }

                long totalBytes = conn.getContentLengthLong();
                is = conn.getInputStream();
                bis = new BufferedInputStream(is, 64 * 1024);
                fos = new FileOutputStream(tempFile);
                bos = new BufferedOutputStream(fos, 64 * 1024);

                byte[] buffer = new byte[64 * 1024];
                int bytesRead;
                long downloadedBytes = 0;
                long lastProgressTime = 0;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    long now = System.currentTimeMillis();
                    if (now - lastProgressTime > 400 && totalBytes > 0) {
                        lastProgressTime = now;
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        if (notificationManager != null) {
                            notifBuilder.setProgress(100, progress, false);
                            notifBuilder.setContentText(progress + "% • " + (downloadedBytes / (1024 * 1024)) + "MB / " + (totalBytes / (1024 * 1024)) + "MB");
                            notificationManager.notify(notificationId, notifBuilder.build());
                        }
                    }
                }

                bos.flush();
                fos.flush();

                tempFile.renameTo(finalFile);
                final String finalPath = finalFile.getAbsolutePath();

                MediaScannerConnection.scanFile(appContext, new String[]{finalPath}, new String[]{"audio/mpeg"}, (path, uri) -> {
                    mainHandler.post(() -> {
                        if (globalListener != null) {
                            globalListener.onDownloadCompleted(displayTitle, path);
                        }
                    });
                });

                if (notificationManager != null) {
                    NotificationCompat.Builder completeNotif = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_download)
                            .setContentTitle("✅ Downloaded: " + displayTitle)
                            .setContentText("Saved to Music folder")
                            .setAutoCancel(true)
                            .setOngoing(false);
                    notificationManager.notify(notificationId, completeNotif.build());
                }

                mainHandler.post(() -> Toast.makeText(appContext, "✅ Download complete: " + displayTitle, Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }

                if (notificationManager != null) {
                    NotificationCompat.Builder errorNotif = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_download)
                            .setContentTitle("❌ Download failed")
                            .setContentText(displayTitle + ": " + e.getMessage())
                            .setAutoCancel(true)
                            .setOngoing(false);
                    notificationManager.notify(notificationId, errorNotif.build());
                }

                mainHandler.post(() -> Toast.makeText(appContext, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try { if (bos != null) bos.close(); } catch (Exception ignored) {}
                try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                try { if (bis != null) bis.close(); } catch (Exception ignored) {}
                try { if (is != null) is.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Shows download progress for songs");
                channel.enableVibration(false);
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }
        }
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.trim().isEmpty()) return "song_" + System.currentTimeMillis() + ".mp3";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
