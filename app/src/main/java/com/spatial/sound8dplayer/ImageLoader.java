package com.spatial.sound8dplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {

    private static ImageLoader instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private ImageLoader() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = Math.max(1024 * 8, maxMemory / 8);
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executorService = Executors.newFixedThreadPool(6);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ImageLoader getInstance() {
        if (instance == null) {
            instance = new ImageLoader();
        }
        return instance;
    }

    public void displayImage(String imageUrl, ImageView imageView, int placeholderResId) {
        if (imageView == null) return;

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            if (placeholderResId != 0) {
                imageView.setImageResource(placeholderResId);
                imageView.setColorFilter(0xFF6B7280);
            }
            return;
        }

        final String finalUrl = imageUrl.trim();
        imageView.setTag(finalUrl);

        Bitmap cached = memoryCache.get(finalUrl);
        if (cached != null) {
            imageView.setImageTintList(null);
            imageView.setColorFilter(null);
            imageView.setImageBitmap(cached);
            return;
        }

        if (placeholderResId != 0) {
            imageView.setImageResource(placeholderResId);
            imageView.setColorFilter(0xFF6B7280);
        }

        executorService.execute(() -> {
            Bitmap bmp = downloadBitmap(finalUrl);
            if (bmp != null) {
                memoryCache.put(finalUrl, bmp);
                mainHandler.post(() -> {
                    String currentTag = (String) imageView.getTag();
                    if (finalUrl.equals(currentTag)) {
                        imageView.setImageTintList(null);
                        imageView.setColorFilter(null);
                        imageView.setImageBitmap(bmp);
                    }
                });
            }
        });
    }

    private Bitmap downloadBitmap(String urlStr) {
        HttpURLConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
            conn.connect();

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null) {
                    conn.disconnect();
                    return downloadBitmap(newUrl);
                }
            }

            if (code == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                byte[] data = baos.toByteArray();
                if (data.length > 0) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    return BitmapFactory.decodeByteArray(data, 0, data.length, options);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
            try { if (baos != null) baos.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
        return null;
    }
}
