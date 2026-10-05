package com.spatial.sound8dplayer;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public class Spatial8DEngine {

    public enum AudioMode {
        OFF, MODE_3D, MODE_8D, MODE_16D
    }

    public interface OnSpatialUpdateListener {
        void onSpatialUpdate(float angle, float leftVol, float rightVol);
    }

    private PresetReverb presetReverb;
    private BassBoost bassBoost;
    private Equalizer equalizer;
    private SpatialAudioProcessor spatialAudioProcessor;

    private AudioMode currentMode = AudioMode.MODE_8D;
    private boolean isRunning = false;
    private float angle = 0.0f;
    private float baseSpeed = 0.045f;
    private float speedMultiplier = 1.0f;

    private short bassStrength = 600;
    private short reverbPreset = PresetReverb.PRESET_LARGEHALL;

    private OnSpatialUpdateListener updateListener;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private HandlerThread bgThread;
    private Handler bgHandler;
    private final Runnable panRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                applySpatialPanning();
                if (bgHandler != null) {
                    bgHandler.postDelayed(this, 25);
                }
            }
        }
    };

    private int attachedSessionId = -1;

    public Spatial8DEngine() {
        bgThread = new HandlerThread("Spatial8DOrbitThread", android.os.Process.THREAD_PRIORITY_AUDIO);
        bgThread.start();
        bgHandler = new Handler(bgThread.getLooper());
    }

    public void setSpatialAudioProcessor(SpatialAudioProcessor processor) {
        this.spatialAudioProcessor = processor;
    }

    public void setOnSpatialUpdateListener(OnSpatialUpdateListener listener) {
        this.updateListener = listener;
    }

    public void attachAudioSession(int sessionId) {
        if (sessionId <= 0) return;
        if (attachedSessionId == sessionId && equalizer != null && bassBoost != null && presetReverb != null) {
            return;
        }
        attachedSessionId = sessionId;

        try {
            try {
                if (presetReverb != null) presetReverb.release();
                presetReverb = new PresetReverb(0, sessionId);
                presetReverb.setPreset(reverbPreset);
                presetReverb.setEnabled(currentMode != AudioMode.OFF);
            } catch (Exception ignored) {}

            try {
                if (bassBoost != null) bassBoost.release();
                bassBoost = new BassBoost(0, sessionId);
                bassBoost.setStrength(bassStrength);
                bassBoost.setEnabled(true);
            } catch (Exception ignored) {}

            try {
                if (equalizer != null) equalizer.release();
                equalizer = new Equalizer(0, sessionId);
                equalizer.setEnabled(true);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public void start() {
        isRunning = true;
        if (bgHandler != null) {
            bgHandler.removeCallbacks(panRunnable);
            bgHandler.post(panRunnable);
        }
    }

    public void pause() {
        isRunning = false;
        if (bgHandler != null) {
            bgHandler.removeCallbacks(panRunnable);
        }
    }

    public void setMode(AudioMode mode) {
        this.currentMode = mode;
        switch (mode) {
            case OFF:
                baseSpeed = 0f;
                if (presetReverb != null) {
                    try { presetReverb.setEnabled(false); } catch (Exception ignored) {}
                }
                if (spatialAudioProcessor != null) {
                    spatialAudioProcessor.setSpatialState(AudioMode.OFF, 0f, 0f, 1.0f);
                }
                break;
            case MODE_3D:
                baseSpeed = 0.040f;
                if (presetReverb != null) {
                    try {
                        presetReverb.setPreset(PresetReverb.PRESET_MEDIUMHALL);
                        presetReverb.setEnabled(true);
                    } catch (Exception ignored) {}
                }
                break;
            case MODE_8D:
                baseSpeed = 0.045f;
                if (presetReverb != null) {
                    try {
                        presetReverb.setPreset(PresetReverb.PRESET_LARGEHALL);
                        presetReverb.setEnabled(true);
                    } catch (Exception ignored) {}
                }
                break;
            case MODE_16D:
                baseSpeed = 0.085f;
                if (presetReverb != null) {
                    try {
                        presetReverb.setPreset(PresetReverb.PRESET_LARGEROOM);
                        presetReverb.setEnabled(true);
                    } catch (Exception ignored) {}
                }
                break;
        }

        if (updateListener != null) {
            final float curAngle = angle;
            uiHandler.post(() -> {
                if (updateListener != null) updateListener.onSpatialUpdate(curAngle, 1.0f, 1.0f);
            });
        }
    }

    public AudioMode getMode() {
        return currentMode;
    }

    public void setSpeedMultiplier(float multiplier) {
        this.speedMultiplier = Math.max(0.2f, Math.min(3.0f, multiplier));
    }

    public float getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void setBassStrength(int strength) {
        short target = (short) Math.max(0, Math.min(1000, strength));
        if (this.bassStrength == target && bassBoost != null) return;
        this.bassStrength = target;
        if (bassBoost != null) {
            try {
                bassBoost.setStrength(this.bassStrength);
                bassBoost.setEnabled(this.bassStrength > 0);
            } catch (Exception ignored) {}
        }
    }

    public int getBassStrength() {
        return bassStrength;
    }

    public void setReverbPreset(short preset) {
        if (this.reverbPreset == preset && presetReverb != null) return;
        this.reverbPreset = preset;
        if (presetReverb != null) {
            try {
                presetReverb.setPreset(preset);
                presetReverb.setEnabled(currentMode != AudioMode.OFF);
            } catch (Exception ignored) {}
        }
    }

    public short getReverbPreset() {
        return reverbPreset;
    }

    public Equalizer getEqualizer() {
        return equalizer;
    }

    private void applySpatialPanning() {
        if (currentMode == AudioMode.OFF) {
            if (spatialAudioProcessor != null) {
                spatialAudioProcessor.setSpatialState(AudioMode.OFF, 0f, 0f, 1.0f);
            }
            if (updateListener != null) {
                uiHandler.post(() -> {
                    if (updateListener != null) updateListener.onSpatialUpdate(0f, 1.0f, 1.0f);
                });
            }
            return;
        }

        angle += (baseSpeed * speedMultiplier);
        if (angle > (2 * Math.PI)) {
            angle = (float) (angle - (2 * Math.PI));
        }

        float azimuth = angle;
        float elevation = 0.0f;
        float radius = 1.0f;
        float leftVol;
        float rightVol;
        float visualAngle = angle;

        if (currentMode == AudioMode.MODE_3D) {
            azimuth = (float) Math.sin(angle);
            elevation = 0.0f;
            radius = 1.2f;
            visualAngle = (float) (Math.PI * 0.5f - azimuth * (Math.PI * 0.44f));
            float norm = (azimuth + 1.0f) * 0.5f;
            leftVol = 0.06f + 0.94f * (1.0f - norm);
            rightVol = 0.06f + 0.94f * norm;
        } else if (currentMode == AudioMode.MODE_8D) {
            elevation = (float) (Math.sin(azimuth) * 0.15f);
            radius = 1.0f;
            visualAngle = angle;
            float sinVal = (float) Math.sin(azimuth);
            leftVol = 0.08f + 0.92f * ((1.0f - sinVal) / 2.0f);
            rightVol = 0.08f + 0.92f * ((1.0f + sinVal) / 2.0f);
        } else {
            elevation = (float) (Math.sin(azimuth * 2.0) * (Math.PI / 3.0));
            radius = 1.0f + 0.25f * (float) Math.cos(azimuth * 3.0);
            visualAngle = angle;
            float sinVal = (float) Math.sin(azimuth);
            leftVol = 0.06f + 0.94f * ((1.0f - sinVal * (float) Math.cos(elevation)) / 2.0f);
            rightVol = 0.06f + 0.94f * ((1.0f + sinVal * (float) Math.cos(elevation)) / 2.0f);
        }

        if (spatialAudioProcessor != null) {
            spatialAudioProcessor.setSpatialState(currentMode, azimuth, elevation, radius);
        }

        if (updateListener != null) {
            final float curAngle = visualAngle;
            final float l = leftVol;
            final float r = rightVol;
            uiHandler.post(() -> {
                if (updateListener != null) updateListener.onSpatialUpdate(curAngle, l, r);
            });
        }
    }

    public void release() {
        isRunning = false;
        if (bgHandler != null) {
            bgHandler.removeCallbacks(panRunnable);
        }
        if (bgThread != null) {
            bgThread.quitSafely();
            bgThread = null;
            bgHandler = null;
        }
        if (presetReverb != null) {
            try { presetReverb.release(); } catch (Exception ignored) {}
        }
        if (bassBoost != null) {
            try { bassBoost.release(); } catch (Exception ignored) {}
        }
        if (equalizer != null) {
            try { equalizer.release(); } catch (Exception ignored) {}
        }
    }
}
