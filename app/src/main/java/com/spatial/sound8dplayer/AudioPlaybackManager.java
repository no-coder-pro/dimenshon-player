package com.spatial.sound8dplayer;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@UnstableApi
public class AudioPlaybackManager {

    private static AudioPlaybackManager instance;

    public enum AudioPreset {
        NORMAL, SLOWED_REVERB, NIGHTCORE
    }

    public enum LoopMode {
        REPEAT_ALL, REPEAT_ONE, SHUFFLE
    }

    public interface PlaybackCallback {
        void onSongChanged(MusicModel song, int index);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressUpdate(int currentPos, int totalDuration);
        void onSpatialModeChanged(Spatial8DEngine.AudioMode mode);
        void onLoopModeChanged(LoopMode mode);
    }

    public interface SleepTimerCallback {
        void onTimerTick(long millisUntilFinished);
        void onTimerFinished();
    }

    private ExoPlayer exoPlayer;
    private final SpatialAudioProcessor spatialAudioProcessor = new SpatialAudioProcessor();
    private final Spatial8DEngine spatialEngine = new Spatial8DEngine();

    private final List<MusicModel> playlist = new ArrayList<>();
    private final List<PlaybackCallback> callbacks = new ArrayList<>();
    private final List<SleepTimerCallback> timerCallbacks = new ArrayList<>();

    private int currentIndex = -1;
    private LoopMode currentLoopMode = LoopMode.REPEAT_ALL;
    private final Random random = new Random();
    private boolean isServiceStarted = false;

    public static final float DEFAULT_SPEED = 1.0f;
    public static final float DEFAULT_PITCH = 1.0f;
    public static final int DEFAULT_BASS = 600;
    public static final short DEFAULT_REVERB = PresetReverb.PRESET_LARGEHALL;
    public static final Spatial8DEngine.AudioMode DEFAULT_MODE = Spatial8DEngine.AudioMode.MODE_8D;

    private float currentSpeed = DEFAULT_SPEED;
    private float currentPitch = DEFAULT_PITCH;
    private float currentOrbitSpeed = DEFAULT_SPEED;
    private int currentBass = DEFAULT_BASS;
    private short currentReverb = DEFAULT_REVERB;
    private AudioPreset currentPreset = AudioPreset.NORMAL;

    private CountDownTimer sleepTimer;
    private long sleepTimerRemainingMs = 0;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (exoPlayer != null) {
                try {
                    if (exoPlayer.isPlaying()) {
                        int pos = (int) exoPlayer.getCurrentPosition();
                        if (pos < 0) pos = 0;
                        int dur = getDuration();
                        for (PlaybackCallback cb : callbacks) {
                            cb.onProgressUpdate(pos, dur);
                        }
                    }
                } catch (Exception ignored) {}
            }
            progressHandler.postDelayed(this, 350);
        }
    };

    private AudioPlaybackManager() {
        spatialEngine.setSpatialAudioProcessor(spatialAudioProcessor);
        progressHandler.post(progressRunnable);
    }

    public static synchronized AudioPlaybackManager getInstance() {
        if (instance == null) {
            instance = new AudioPlaybackManager();
        }
        return instance;
    }

    private void ensureExoPlayer(Context context) {
        if (exoPlayer != null) return;

        Context appContext = context.getApplicationContext();

        DefaultAudioSink audioSink = new DefaultAudioSink.Builder(appContext)
                .setAudioProcessors(new AudioProcessor[]{spatialAudioProcessor})
                .build();

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(appContext) {
            @Override
            protected AudioSink buildAudioSink(Context context, boolean enableFloatOutput, boolean enableAudioTrackPlaybackParams) {
                return audioSink;
            }
        };

        androidx.media3.extractor.DefaultExtractorsFactory extractorsFactory = new androidx.media3.extractor.DefaultExtractorsFactory()
                .setMp3ExtractorFlags(
                        androidx.media3.extractor.mp3.Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING
                        | androidx.media3.extractor.mp3.Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING
                );

        androidx.media3.datasource.DefaultHttpDataSource.Factory httpDataSourceFactory = new androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Spatial8DPlayer/1.0")
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true);

        androidx.media3.datasource.DefaultDataSource.Factory dataSourceFactory =
                new androidx.media3.datasource.DefaultDataSource.Factory(appContext, httpDataSourceFactory);

        androidx.media3.exoplayer.source.DefaultMediaSourceFactory mediaSourceFactory =
                new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory);

        exoPlayer = new ExoPlayer.Builder(appContext, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    spatialEngine.start();
                } else {
                    spatialEngine.pause();
                }
                notifyPlaybackStateChanged(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    handleSongCompletion(appContext);
                }
            }
        });
    }

    public void addCallback(PlaybackCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }

    public void removeCallback(PlaybackCallback callback) {
        callbacks.remove(callback);
    }

    public void addTimerCallback(SleepTimerCallback callback) {
        if (!timerCallbacks.contains(callback)) {
            timerCallbacks.add(callback);
        }
    }

    public void removeTimerCallback(SleepTimerCallback callback) {
        timerCallbacks.remove(callback);
    }

    public Spatial8DEngine getSpatialEngine() {
        return spatialEngine;
    }

    public void setPlaylist(List<MusicModel> songs) {
        this.playlist.clear();
        this.playlist.addAll(songs);
    }

    public List<MusicModel> getPlaylist() {
        return playlist;
    }

    public MusicModel getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isPlaying() {
        try {
            return exoPlayer != null && exoPlayer.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public int getCurrentPosition() {
        try {
            return exoPlayer != null ? (int) exoPlayer.getCurrentPosition() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public int getDuration() {
        try {
            long dur = exoPlayer != null ? exoPlayer.getDuration() : 0;
            if (dur > 0 && dur != androidx.media3.common.C.TIME_UNSET) {
                return (int) dur;
            }
            MusicModel current = getCurrentSong();
            if (current != null && current.getDurationMs() > 0) {
                return (int) current.getDurationMs();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void playSong(Context context, int index) {
        if (playlist.isEmpty() || index < 0 || index >= playlist.size()) return;

        ensureExoPlayer(context);

        currentIndex = index;
        MusicModel song = playlist.get(index);

        try {
            MediaItem mediaItem = MediaItem.fromUri(song.getUri());
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();

            PlaybackParameters params = new PlaybackParameters(currentSpeed, currentPitch);
            exoPlayer.setPlaybackParameters(params);

            exoPlayer.play();

            int sessionId = exoPlayer.getAudioSessionId();
            spatialEngine.attachAudioSession(sessionId);
            spatialEngine.setMode(spatialEngine.getMode());
            spatialEngine.setSpeedMultiplier(currentOrbitSpeed);
            spatialEngine.setBassStrength(currentBass);
            spatialEngine.setReverbPreset(currentReverb);
            spatialEngine.start();

            notifySongChanged(song, index);
            startMusicServiceSafely(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playOnlineTrack(Context context, OnlineSongModel onlineSong, String streamUrl) {
        if (onlineSong == null || streamUrl == null) return;

        ensureExoPlayer(context);

        long durMs = onlineSong.getDurationSec() > 0 ? (onlineSong.getDurationSec() * 1000L) : 0L;
        if (durMs <= 0 && onlineSong.getDurationStr() != null && !onlineSong.getDurationStr().isEmpty()) {
            durMs = OnlineMusicApiService.parseDurationStringToSec(onlineSong.getDurationStr()) * 1000L;
        }

        MusicModel onlineModel = new MusicModel(
                Math.abs(onlineSong.getId().hashCode()),
                onlineSong.getTitle(),
                onlineSong.getArtist(),
                onlineSong.isSpotify() ? "Spotify" : "YouTube",
                onlineSong.getDurationStr(),
                durMs,
                0L,
                System.currentTimeMillis(),
                onlineSong.getUrl(),
                android.net.Uri.parse(streamUrl)
        );

        int foundIdx = -1;
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).getId() == onlineModel.getId()) {
                foundIdx = i;
                break;
            }
        }
        if (foundIdx == -1) {
            playlist.add(0, onlineModel);
            currentIndex = 0;
        } else {
            currentIndex = foundIdx;
        }

        playSong(context, currentIndex);
    }

    private void handleSongCompletion(Context context) {
        if (playlist.isEmpty()) return;

        if (currentLoopMode == LoopMode.REPEAT_ONE) {
            playSong(context, currentIndex);
        } else if (currentLoopMode == LoopMode.SHUFFLE) {
            if (playlist.size() > 1) {
                int next;
                do {
                    next = random.nextInt(playlist.size());
                } while (next == currentIndex);
                playSong(context, next);
            } else {
                playSong(context, 0);
            }
        } else {
            playNext(context);
        }
    }

    public void togglePlayPause(Context context) {
        ensureExoPlayer(context);

        if (exoPlayer == null) return;

        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
            spatialEngine.pause();
            notifyPlaybackStateChanged(false);
        } else {
            if (currentIndex == -1 && !playlist.isEmpty()) {
                playSong(context, 0);
            } else {
                exoPlayer.play();
                spatialEngine.start();
                notifyPlaybackStateChanged(true);
            }
        }

        startMusicServiceSafely(context);
    }

    private void startMusicServiceSafely(Context context) {
        try {
            Intent serviceIntent = new Intent(context, MusicService.class);
            if (!isServiceStarted) {
                isServiceStarted = true;
                ContextCompat.startForegroundService(context, serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception ignored) {}
    }

    public void pausePlayback() {
        try {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                exoPlayer.pause();
                if (spatialEngine != null) {
                    spatialEngine.pause();
                }
                notifyPlaybackStateChanged(false);
            }
        } catch (Exception ignored) {}
    }

    public void stopPlayback() {
        try {
            if (exoPlayer != null) {
                exoPlayer.stop();
            }
            if (spatialEngine != null) {
                spatialEngine.pause();
            }
        } catch (Exception ignored) {}
        currentIndex = -1;
        notifyPlaybackStateChanged(false);
        notifySongChanged(null, -1);
    }

    public void playNext(Context context) {
        if (playlist.isEmpty()) return;
        if (currentLoopMode == LoopMode.SHUFFLE && playlist.size() > 1) {
            int next;
            do {
                next = random.nextInt(playlist.size());
            } while (next == currentIndex);
            playSong(context, next);
        } else {
            int nextIndex = (currentIndex + 1) % playlist.size();
            playSong(context, nextIndex);
        }
    }

    public void playPrev(Context context) {
        if (playlist.isEmpty()) return;
        int prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        playSong(context, prevIndex);
    }

    public void toggleLoopMode() {
        if (currentLoopMode == LoopMode.REPEAT_ALL) {
            currentLoopMode = LoopMode.REPEAT_ONE;
        } else if (currentLoopMode == LoopMode.REPEAT_ONE) {
            currentLoopMode = LoopMode.SHUFFLE;
        } else {
            currentLoopMode = LoopMode.REPEAT_ALL;
        }
        for (PlaybackCallback cb : callbacks) {
            cb.onLoopModeChanged(currentLoopMode);
        }
    }

    public LoopMode getLoopMode() {
        return currentLoopMode;
    }

    public void seekTo(int progress) {
        if (exoPlayer != null) {
            try {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    exoPlayer.seekTo(progress);
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            if (exoPlayer != null) exoPlayer.seekTo(progress);
                        } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}
        }
    }

    public void setSpatialMode(Spatial8DEngine.AudioMode mode) {
        spatialEngine.setMode(mode);
        for (PlaybackCallback cb : callbacks) {
            cb.onSpatialModeChanged(mode);
        }
    }

    public void setOrbitSpeedMultiplier(float speed) {
        this.currentOrbitSpeed = speed;
        spatialEngine.setSpeedMultiplier(speed);
    }

    public float getOrbitSpeedMultiplier() {
        return currentOrbitSpeed;
    }

    public void setBassStrength(int bass) {
        this.currentBass = bass;
        spatialEngine.setBassStrength(bass);
    }

    public int getBassStrength() {
        return currentBass;
    }

    public void setReverbPreset(short reverb) {
        this.currentReverb = reverb;
        spatialEngine.setReverbPreset(reverb);
    }

    public short getReverbPreset() {
        return currentReverb;
    }

    public void setPlaybackSpeed(float speed) {
        this.currentSpeed = Math.max(0.4f, Math.min(2.5f, speed));
        updatePlaybackParameters();
    }

    public float getPlaybackSpeed() {
        return currentSpeed;
    }

    public void setPlaybackPitch(float pitch) {
        this.currentPitch = Math.max(0.4f, Math.min(2.5f, pitch));
        updatePlaybackParameters();
    }

    public float getPlaybackPitch() {
        return currentPitch;
    }

    private void updatePlaybackParameters() {
        if (exoPlayer != null) {
            try {
                PlaybackParameters params = new PlaybackParameters(currentSpeed, currentPitch);
                exoPlayer.setPlaybackParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void applyPreset(AudioPreset preset) {
        this.currentPreset = preset;
        switch (preset) {
            case NORMAL:
                currentSpeed = 1.0f;
                currentPitch = 1.0f;
                currentOrbitSpeed = 1.0f;
                currentBass = 600;
                currentReverb = PresetReverb.PRESET_LARGEHALL;
                setSpatialMode(Spatial8DEngine.AudioMode.MODE_8D);
                break;
            case SLOWED_REVERB:
                currentSpeed = 0.82f;
                currentPitch = 0.82f;
                currentOrbitSpeed = 0.6f;
                currentBass = 850;
                currentReverb = PresetReverb.PRESET_LARGEHALL;
                setSpatialMode(Spatial8DEngine.AudioMode.MODE_8D);
                break;
            case NIGHTCORE:
                currentSpeed = 1.25f;
                currentPitch = 1.25f;
                currentOrbitSpeed = 1.5f;
                currentBass = 500;
                currentReverb = PresetReverb.PRESET_LARGEROOM;
                setSpatialMode(Spatial8DEngine.AudioMode.MODE_16D);
                break;
        }

        updatePlaybackParameters();
        setOrbitSpeedMultiplier(currentOrbitSpeed);
        setBassStrength(currentBass);
        setReverbPreset(currentReverb);
    }

    public AudioPreset getCurrentPreset() {
        return currentPreset;
    }

    public void startSleepTimer(int minutes) {
        stopSleepTimer();
        if (minutes <= 0) return;

        long durationMs = minutes * 60 * 1000L;
        sleepTimerRemainingMs = durationMs;

        sleepTimer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                sleepTimerRemainingMs = millisUntilFinished;
                for (SleepTimerCallback cb : timerCallbacks) {
                    cb.onTimerTick(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                sleepTimerRemainingMs = 0;
                if (exoPlayer != null && exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                    spatialEngine.pause();
                    notifyPlaybackStateChanged(false);
                }
                for (SleepTimerCallback cb : timerCallbacks) {
                    cb.onTimerFinished();
                }
            }
        }.start();
    }

    public void stopSleepTimer() {
        if (sleepTimer != null) {
            sleepTimer.cancel();
            sleepTimer = null;
        }
        sleepTimerRemainingMs = 0;
        for (SleepTimerCallback cb : timerCallbacks) {
            cb.onTimerFinished();
        }
    }

    public long getSleepTimerRemainingMs() {
        return sleepTimerRemainingMs;
    }

    public void resetAllEffects() {
        applyPreset(AudioPreset.NORMAL);
    }

    private void notifySongChanged(MusicModel song, int index) {
        for (PlaybackCallback cb : callbacks) {
            cb.onSongChanged(song, index);
        }
    }

    private void notifyPlaybackStateChanged(boolean isPlaying) {
        for (PlaybackCallback cb : callbacks) {
            cb.onPlaybackStateChanged(isPlaying);
        }
    }
}
