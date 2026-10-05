package com.spatial.sound8dplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class MusicService extends Service implements AudioPlaybackManager.PlaybackCallback {

    public static final String CHANNEL_ID = "aura_8d_media_channel";
    public static final int NOTIFICATION_ID = 8801;

    public static final String ACTION_TOGGLE = "com.spatial.sound8dplayer.TOGGLE";
    public static final String ACTION_NEXT = "com.spatial.sound8dplayer.NEXT";
    public static final String ACTION_PREV = "com.spatial.sound8dplayer.PREV";
    public static final String ACTION_STOP = "com.spatial.sound8dplayer.STOP";

    private AudioPlaybackManager audioManager;
    private NotificationManager notificationManager;
    private MediaSessionCompat mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = AudioPlaybackManager.getInstance();
        audioManager.addCallback(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();
        initMediaSession();
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "Aura8DMediaSession");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                audioManager.togglePlayPause(MusicService.this);
            }

            @Override
            public void onPause() {
                audioManager.togglePlayPause(MusicService.this);
            }

            @Override
            public void onSkipToNext() {
                audioManager.playNext(MusicService.this);
            }

            @Override
            public void onSkipToPrevious() {
                audioManager.playPrev(MusicService.this);
            }

            @Override
            public void onStop() {
                stopForeground(true);
                stopSelf();
            }
        });
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_TOGGLE:
                    audioManager.togglePlayPause(this);
                    break;
                case ACTION_NEXT:
                    audioManager.playNext(this);
                    break;
                case ACTION_PREV:
                    audioManager.playPrev(this);
                    break;
                case ACTION_STOP:
                    stopForeground(true);
                    stopSelf();
                    return START_NOT_STICKY;
            }
        }

        updateNotification();
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Aura 8D Media Controls",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Audio playback controls and lock-screen widget");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void updateNotification() {
        MusicModel song = audioManager.getCurrentSong();

        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpen = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        if (song == null) {
            NotificationCompat.Builder idleBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_headphone)
                    .setContentTitle("Dimen Spatial Audio")
                    .setContentText("Ready for playback")
                    .setContentIntent(pendingOpen)
                    .setOngoing(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            startForeground(NOTIFICATION_ID, idleBuilder.build());
            return;
        }

        boolean isPlaying = audioManager.isPlaying();

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(
                        isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        audioManager.getCurrentPosition(),
                        1.0f
                );
        mediaSession.setPlaybackState(stateBuilder.build());

        MediaMetadataCompat.Builder metaBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, audioManager.getDuration());
        mediaSession.setMetadata(metaBuilder.build());

        PendingIntent pPrev = createServiceAction(ACTION_PREV, 1);
        PendingIntent pToggle = createServiceAction(ACTION_TOGGLE, 2);
        PendingIntent pNext = createServiceAction(ACTION_NEXT, 3);

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(createServiceAction(ACTION_STOP, 5));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_headphone)
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setContentIntent(pendingOpen)
                .setOngoing(isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(mediaStyle)
                .addAction(R.drawable.ic_prev, "Previous", pPrev)
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, isPlaying ? "Pause" : "Play", pToggle)
                .addAction(R.drawable.ic_next, "Next", pNext);

        Notification notification = builder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private PendingIntent createServiceAction(String action, int reqCode) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(
                this, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
    }

    @Override
    public void onSongChanged(MusicModel song, int index) {
        updateNotification();
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updateNotification();
    }

    @Override
    public void onProgressUpdate(int currentPos, int totalDuration) {}

    @Override
    public void onSpatialModeChanged(Spatial8DEngine.AudioMode mode) {
        updateNotification();
    }

    @Override
    public void onLoopModeChanged(AudioPlaybackManager.LoopMode mode) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        audioManager.removeCallback(this);
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
