package com.spatial.sound8dplayer;

import android.app.AlertDialog;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlayerActivity extends AppCompatActivity implements AudioPlaybackManager.PlaybackCallback, AudioPlaybackManager.SleepTimerCallback {

    private SpatialVisualizerView playerVisualizer;
    private TextView tvPlayerTitle, tvPlayerArtist, tvPlayerCurrentTime, tvPlayerTotalDuration;
    private TextView tvPlayerPlaybackSpeedValue, tvPlayerPitchValue;
    private TextView tvPlayerSpeedValue, tvPlayerBassValue;
    private Button btnModeOff, btnMode3D, btnMode8D, btnMode16D;
    private Button btnPresetNormal, btnPresetSlowed, btnPresetNightcore;
    private ImageButton btnCollapsePlayer, btnResetEffects, btnEqualizer, btnSleepTimer;
    private ImageButton btnPlayerPrev, btnPlayerPlayPause, btnPlayerNext;
    private ImageButton btnLoopMode, btnPlayerFavorite;
    private SeekBar sbPlayerProgress, sbPlayerPlaybackSpeed, sbPlayerPitch, sbPlayerSpeed, sbPlayerBass;
    private Spinner spPlayerReverb;

    private LinearLayout bottomSheetQueue;
    private BottomSheetBehavior<LinearLayout> queueBehavior;
    private LinearLayout llQueueHeader;
    private TextView tvQueueBadge;
    private ImageView ivQueueChevron;
    private RecyclerView rvPlayerQueue;
    private QueueAdapter queueAdapter;

    private AudioPlaybackManager audioManager;
    private FavoritesManager favoritesManager;
    private boolean isUserSeeking = false;
    private AlertDialog timerDialog;
    private TextView tvTimerStatusInDialog;

    private final short[] REVERB_PRESET_VALUES = {
            PresetReverb.PRESET_LARGEHALL,
            PresetReverb.PRESET_MEDIUMHALL,
            PresetReverb.PRESET_LARGEROOM,
            PresetReverb.PRESET_MEDIUMROOM,
            PresetReverb.PRESET_PLATE,
            PresetReverb.PRESET_NONE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        audioManager = AudioPlaybackManager.getInstance();
        favoritesManager = FavoritesManager.getInstance(this);

        initViews();
        setupReverbSpinner();
        setupVisualizer();
        setupQueueBottomSheet();
        setupListeners();

        updateUIFromState();
    }

    private void initViews() {
        playerVisualizer = findViewById(R.id.playerVisualizer);
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvPlayerArtist = findViewById(R.id.tvPlayerArtist);
        tvPlayerCurrentTime = findViewById(R.id.tvPlayerCurrentTime);
        tvPlayerTotalDuration = findViewById(R.id.tvPlayerTotalDuration);
        tvPlayerPlaybackSpeedValue = findViewById(R.id.tvPlayerPlaybackSpeedValue);
        tvPlayerPitchValue = findViewById(R.id.tvPlayerPitchValue);
        tvPlayerSpeedValue = findViewById(R.id.tvPlayerSpeedValue);
        tvPlayerBassValue = findViewById(R.id.tvPlayerBassValue);

        btnModeOff = findViewById(R.id.btnModeOff);
        btnMode3D = findViewById(R.id.btnMode3D);
        btnMode8D = findViewById(R.id.btnMode8D);
        btnMode16D = findViewById(R.id.btnMode16D);

        btnPresetNormal = findViewById(R.id.btnPresetNormal);
        btnPresetSlowed = findViewById(R.id.btnPresetSlowed);
        btnPresetNightcore = findViewById(R.id.btnPresetNightcore);

        btnCollapsePlayer = findViewById(R.id.btnCollapsePlayer);
        btnResetEffects = findViewById(R.id.btnResetEffects);
        btnEqualizer = findViewById(R.id.btnEqualizer);
        btnSleepTimer = findViewById(R.id.btnSleepTimer);
        btnPlayerPrev = findViewById(R.id.btnPlayerPrev);
        btnPlayerPlayPause = findViewById(R.id.btnPlayerPlayPause);
        btnPlayerNext = findViewById(R.id.btnPlayerNext);
        btnLoopMode = findViewById(R.id.btnLoopMode);
        btnPlayerFavorite = findViewById(R.id.btnPlayerFavorite);

        sbPlayerProgress = findViewById(R.id.sbPlayerProgress);
        sbPlayerPlaybackSpeed = findViewById(R.id.sbPlayerPlaybackSpeed);
        sbPlayerPitch = findViewById(R.id.sbPlayerPitch);
        sbPlayerSpeed = findViewById(R.id.sbPlayerSpeed);
        sbPlayerBass = findViewById(R.id.sbPlayerBass);
        spPlayerReverb = findViewById(R.id.spPlayerReverb);

        bottomSheetQueue = findViewById(R.id.bottomSheetQueue);
        llQueueHeader = findViewById(R.id.llQueueHeader);
        tvQueueBadge = findViewById(R.id.tvQueueBadge);
        ivQueueChevron = findViewById(R.id.ivQueueChevron);
        rvPlayerQueue = findViewById(R.id.rvPlayerQueue);

        tvPlayerTitle.setSelected(true);
    }

    private void setupQueueBottomSheet() {
        if (bottomSheetQueue == null) return;
        queueBehavior = BottomSheetBehavior.from(bottomSheetQueue);

        List<MusicModel> playlist = audioManager.getPlaylist();
        rvPlayerQueue.setLayoutManager(new LinearLayoutManager(this));
        queueAdapter = new QueueAdapter(playlist, (song, position) -> {
            audioManager.playSong(this, position);
        });
        rvPlayerQueue.setAdapter(queueAdapter);
        queueAdapter.setCurrentPlayingIndex(audioManager.getCurrentIndex());

        if (playlist != null) {
            tvQueueBadge.setText(playlist.size() + " Songs");
        }

        llQueueHeader.setOnClickListener(v -> {
            if (queueBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                queueBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                queueBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                scrollToCurrentPlaying();
            }
        });

        queueBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    if (ivQueueChevron != null) ivQueueChevron.setImageResource(R.drawable.ic_chevron_down);
                    scrollToCurrentPlaying();
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (ivQueueChevron != null) ivQueueChevron.setImageResource(R.drawable.ic_chevron_up);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    private void scrollToCurrentPlaying() {
        int idx = audioManager.getCurrentIndex();
        if (idx >= 0 && rvPlayerQueue != null && rvPlayerQueue.getLayoutManager() != null) {
            int scrollTarget = Math.max(0, idx - 1);
            ((LinearLayoutManager) rvPlayerQueue.getLayoutManager()).scrollToPositionWithOffset(scrollTarget, 0);
        }
    }

    private void setupVisualizer() {
        audioManager.getSpatialEngine().setOnSpatialUpdateListener((angle, leftVol, rightVol) -> {
            boolean isPlaying = audioManager.isPlaying();
            playerVisualizer.updateAngle(angle, isPlaying, audioManager.getSpatialEngine().getMode());
        });
    }

    private void setupListeners() {
        btnCollapsePlayer.setOnClickListener(v -> finish());

        btnPresetNormal.setOnClickListener(v -> {
            audioManager.applyPreset(AudioPlaybackManager.AudioPreset.NORMAL);
            updateUIFromState();
            Toast.makeText(this, "8D Normal Audio Activated", Toast.LENGTH_SHORT).show();
        });

        btnPresetSlowed.setOnClickListener(v -> {
            audioManager.applyPreset(AudioPlaybackManager.AudioPreset.SLOWED_REVERB);
            updateUIFromState();
            Toast.makeText(this, "🌊 Slowed + Reverb 8D Activated", Toast.LENGTH_SHORT).show();
        });

        btnPresetNightcore.setOnClickListener(v -> {
            audioManager.applyPreset(AudioPlaybackManager.AudioPreset.NIGHTCORE);
            updateUIFromState();
            Toast.makeText(this, "⚡ Nightcore 8D Activated", Toast.LENGTH_SHORT).show();
        });

        btnModeOff.setOnClickListener(v -> audioManager.setSpatialMode(Spatial8DEngine.AudioMode.OFF));
        btnMode3D.setOnClickListener(v -> audioManager.setSpatialMode(Spatial8DEngine.AudioMode.MODE_3D));
        btnMode8D.setOnClickListener(v -> audioManager.setSpatialMode(Spatial8DEngine.AudioMode.MODE_8D));
        btnMode16D.setOnClickListener(v -> audioManager.setSpatialMode(Spatial8DEngine.AudioMode.MODE_16D));

        sbPlayerPlaybackSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = 0.5f + (progress / 20.0f);
                tvPlayerPlaybackSpeedValue.setText(String.format(Locale.US, "%.2fx", speed));
                if (fromUser) {
                    audioManager.setPlaybackSpeed(speed);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbPlayerPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = 0.5f + (progress / 20.0f);
                if (pitch < 0.95f) {
                    tvPlayerPitchValue.setText(String.format(Locale.US, "%.2fx (Deep 🎙️)", pitch));
                } else if (pitch > 1.05f) {
                    tvPlayerPitchValue.setText(String.format(Locale.US, "%.2fx (High ⚡)", pitch));
                } else {
                    tvPlayerPitchValue.setText("1.00x (Natural)");
                }
                if (fromUser) {
                    audioManager.setPlaybackPitch(pitch);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbPlayerSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float mult = Math.max(0.2f, progress / 10.0f);
                tvPlayerSpeedValue.setText(String.format(Locale.US, "%.1fx", mult));
                if (fromUser) {
                    audioManager.setOrbitSpeedMultiplier(mult);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sbPlayerBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int percent = progress / 10;
                tvPlayerBassValue.setText(percent + "%");
                if (fromUser) {
                    audioManager.setBassStrength(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPlayerPlayPause.setOnClickListener(v -> audioManager.togglePlayPause(this));
        btnPlayerPrev.setOnClickListener(v -> audioManager.playPrev(this));
        btnPlayerNext.setOnClickListener(v -> audioManager.playNext(this));
        btnLoopMode.setOnClickListener(v -> audioManager.toggleLoopMode());

        btnPlayerFavorite.setOnClickListener(v -> {
            MusicModel song = audioManager.getCurrentSong();
            if (song != null) {
                boolean isFav = favoritesManager.toggleFavorite(song.getId());
                updateFavoriteUI();
                Toast.makeText(this, isFav ? "Added to Favorites ⭐" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
            }
        });

        btnResetEffects.setOnClickListener(v -> {
            audioManager.resetAllEffects();
            updateUIFromState();
            Toast.makeText(this, "Audio effects reset to default", Toast.LENGTH_SHORT).show();
        });

        btnEqualizer.setOnClickListener(v -> showEqualizerDialog());
        btnSleepTimer.setOnClickListener(v -> showSleepTimerDialog());

        sbPlayerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvPlayerCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                audioManager.seekTo(seekBar.getProgress());
                isUserSeeking = false;
            }
        });
    }

    private void setupReverbSpinner() {
        String[] presets = {
                "Large Hall (Concert Hall)",
                "Medium Hall (Auditorium)",
                "Large Room (Studio)",
                "Medium Room (Acoustic)",
                "Plate Reverb (Bright)",
                "Off / Dry"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_spinner_selected, presets) {
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ContextCompat.getColor(PlayerActivity.this, R.color.text_primary));
                    view.setBackgroundColor(ContextCompat.getColor(PlayerActivity.this, R.color.bg_card));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spPlayerReverb.setAdapter(adapter);

        spPlayerReverb.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                audioManager.setReverbPreset(REVERB_PRESET_VALUES[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateUIFromState() {
        MusicModel song = audioManager.getCurrentSong();
        if (song != null) {
            tvPlayerTitle.setText(song.getTitle());
            tvPlayerArtist.setText(song.getArtist());
        }

        btnPlayerPlayPause.setImageResource(audioManager.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

        int duration = audioManager.getDuration();
        int current = audioManager.getCurrentPosition();
        sbPlayerProgress.setMax(duration);
        sbPlayerProgress.setProgress(current);
        tvPlayerTotalDuration.setText(formatTime(duration));
        tvPlayerCurrentTime.setText(formatTime(current));

        float curSpeed = audioManager.getPlaybackSpeed();
        int speedProgress = (int) Math.round((curSpeed - 0.5f) * 20.0f);
        sbPlayerPlaybackSpeed.setProgress(Math.max(0, Math.min(30, speedProgress)));
        tvPlayerPlaybackSpeedValue.setText(String.format(Locale.US, "%.2fx", curSpeed));

        float curPitch = audioManager.getPlaybackPitch();
        int pitchProgress = (int) Math.round((curPitch - 0.5f) * 20.0f);
        sbPlayerPitch.setProgress(Math.max(0, Math.min(30, pitchProgress)));
        if (curPitch < 0.95f) {
            tvPlayerPitchValue.setText(String.format(Locale.US, "%.2fx (Deep 🎙️)", curPitch));
        } else if (curPitch > 1.05f) {
            tvPlayerPitchValue.setText(String.format(Locale.US, "%.2fx (High ⚡)", curPitch));
        } else {
            tvPlayerPitchValue.setText("1.00x (Natural)");
        }

        int orbitProgress = (int) (audioManager.getOrbitSpeedMultiplier() * 10);
        sbPlayerSpeed.setProgress(orbitProgress);
        tvPlayerSpeedValue.setText(String.format(Locale.US, "%.1fx", audioManager.getOrbitSpeedMultiplier()));

        int bass = audioManager.getBassStrength();
        sbPlayerBass.setProgress(bass);
        tvPlayerBassValue.setText((bass / 10) + "%");

        short curReverb = audioManager.getReverbPreset();
        for (int i = 0; i < REVERB_PRESET_VALUES.length; i++) {
            if (REVERB_PRESET_VALUES[i] == curReverb) {
                spPlayerReverb.setSelection(i);
                break;
            }
        }

        updateModeUI(audioManager.getSpatialEngine().getMode());
        updatePresetButtonsUI(audioManager.getCurrentPreset());
        updateLoopModeUI(audioManager.getLoopMode());
        updateFavoriteUI();

        if (queueAdapter != null) {
            queueAdapter.setCurrentPlayingIndex(audioManager.getCurrentIndex());
        }
    }

    private void updateFavoriteUI() {
        MusicModel song = audioManager.getCurrentSong();
        if (song != null) {
            boolean isFav = favoritesManager.isFavorite(song.getId());
            btnPlayerFavorite.setImageResource(isFav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            ImageViewCompat.setImageTintList(btnPlayerFavorite,
                    android.content.res.ColorStateList.valueOf(isFav ? android.graphics.Color.parseColor("#FFD700") : ContextCompat.getColor(this, R.color.text_secondary)));
        }
    }

    private void updateLoopModeUI(AudioPlaybackManager.LoopMode loopMode) {
        if (loopMode == AudioPlaybackManager.LoopMode.REPEAT_ALL) {
            btnLoopMode.setImageResource(R.drawable.ic_repeat);
            btnLoopMode.setColorFilter(ContextCompat.getColor(this, R.color.secondary));
        } else if (loopMode == AudioPlaybackManager.LoopMode.REPEAT_ONE) {
            btnLoopMode.setImageResource(R.drawable.ic_repeat_one);
            btnLoopMode.setColorFilter(ContextCompat.getColor(this, R.color.accent_pink));
        } else {
            btnLoopMode.setImageResource(R.drawable.ic_shuffle);
            btnLoopMode.setColorFilter(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void updatePresetButtonsUI(AudioPlaybackManager.AudioPreset preset) {
        int activeBg = ContextCompat.getColor(this, R.color.primary);
        int inactiveBg = ContextCompat.getColor(this, R.color.bg_card_light);
        int textActive = ContextCompat.getColor(this, R.color.text_primary);
        int textInactive = ContextCompat.getColor(this, R.color.text_secondary);

        btnPresetNormal.setBackgroundColor(preset == AudioPlaybackManager.AudioPreset.NORMAL ? activeBg : inactiveBg);
        btnPresetNormal.setTextColor(preset == AudioPlaybackManager.AudioPreset.NORMAL ? textActive : textInactive);

        btnPresetSlowed.setBackgroundColor(preset == AudioPlaybackManager.AudioPreset.SLOWED_REVERB ? activeBg : inactiveBg);
        btnPresetSlowed.setTextColor(preset == AudioPlaybackManager.AudioPreset.SLOWED_REVERB ? textActive : textInactive);

        btnPresetNightcore.setBackgroundColor(preset == AudioPlaybackManager.AudioPreset.NIGHTCORE ? activeBg : inactiveBg);
        btnPresetNightcore.setTextColor(preset == AudioPlaybackManager.AudioPreset.NIGHTCORE ? textActive : textInactive);
    }

    private void updateModeUI(Spatial8DEngine.AudioMode mode) {
        int activeBg = ContextCompat.getColor(this, R.color.primary);
        int inactiveBg = ContextCompat.getColor(this, R.color.bg_card_light);
        int textActive = ContextCompat.getColor(this, R.color.text_primary);
        int textInactive = ContextCompat.getColor(this, R.color.text_secondary);

        btnModeOff.setBackgroundColor(mode == Spatial8DEngine.AudioMode.OFF ? activeBg : inactiveBg);
        btnModeOff.setTextColor(mode == Spatial8DEngine.AudioMode.OFF ? textActive : textInactive);

        btnMode3D.setBackgroundColor(mode == Spatial8DEngine.AudioMode.MODE_3D ? activeBg : inactiveBg);
        btnMode3D.setTextColor(mode == Spatial8DEngine.AudioMode.MODE_3D ? textActive : textInactive);

        btnMode8D.setBackgroundColor(mode == Spatial8DEngine.AudioMode.MODE_8D ? activeBg : inactiveBg);
        btnMode8D.setTextColor(mode == Spatial8DEngine.AudioMode.MODE_8D ? textActive : textInactive);

        btnMode16D.setBackgroundColor(mode == Spatial8DEngine.AudioMode.MODE_16D ? activeBg : inactiveBg);
        btnMode16D.setTextColor(mode == Spatial8DEngine.AudioMode.MODE_16D ? textActive : textInactive);
    }

    private void showSleepTimerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sleep_timer, null);
        tvTimerStatusInDialog = dialogView.findViewById(R.id.tvTimerStatus);
        TextView tvMinutesValue = dialogView.findViewById(R.id.tvMinutesValue);
        SeekBar sbTimerMinutes = dialogView.findViewById(R.id.sbTimerMinutes);
        Button btnMinus = dialogView.findViewById(R.id.btnTimerMinus);
        Button btnPlus = dialogView.findViewById(R.id.btnTimerPlus);
        Button btn15 = dialogView.findViewById(R.id.btnTimer15);
        Button btn30 = dialogView.findViewById(R.id.btnTimer30);
        Button btn45 = dialogView.findViewById(R.id.btnTimer45);
        Button btn60 = dialogView.findViewById(R.id.btnTimer60);
        Button btnStartTimer = dialogView.findViewById(R.id.btnStartTimer);
        Button btnTurnOff = dialogView.findViewById(R.id.btnTurnOffTimer);
        Button btnClose = dialogView.findViewById(R.id.btnCloseTimerDialog);

        final int[] selectedMinutes = {30};
        long rem = audioManager.getSleepTimerRemainingMs();
        if (rem > 0) {
            tvTimerStatusInDialog.setText("Active timer: " + (rem / 60000) + " minutes remaining");
            selectedMinutes[0] = (int) (rem / 60000);
        } else {
            tvTimerStatusInDialog.setText("Select duration to turn off audio:");
        }

        tvMinutesValue.setText(selectedMinutes[0] + " MIN");
        sbTimerMinutes.setProgress(selectedMinutes[0]);

        sbTimerMinutes.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedMinutes[0] = Math.max(1, progress);
                tvMinutesValue.setText(selectedMinutes[0] + " MIN");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnMinus.setOnClickListener(v -> {
            if (selectedMinutes[0] > 5) {
                selectedMinutes[0] -= 5;
            } else if (selectedMinutes[0] > 1) {
                selectedMinutes[0]--;
            }
            sbTimerMinutes.setProgress(selectedMinutes[0]);
            tvMinutesValue.setText(selectedMinutes[0] + " MIN");
        });

        btnPlus.setOnClickListener(v -> {
            if (selectedMinutes[0] < 120) {
                selectedMinutes[0] += 5;
                if (selectedMinutes[0] > 120) selectedMinutes[0] = 120;
            }
            sbTimerMinutes.setProgress(selectedMinutes[0]);
            tvMinutesValue.setText(selectedMinutes[0] + " MIN");
        });

        btn15.setOnClickListener(v -> { selectedMinutes[0] = 15; sbTimerMinutes.setProgress(15); tvMinutesValue.setText("15 MIN"); });
        btn30.setOnClickListener(v -> { selectedMinutes[0] = 30; sbTimerMinutes.setProgress(30); tvMinutesValue.setText("30 MIN"); });
        btn45.setOnClickListener(v -> { selectedMinutes[0] = 45; sbTimerMinutes.setProgress(45); tvMinutesValue.setText("45 MIN"); });
        btn60.setOnClickListener(v -> { selectedMinutes[0] = 60; sbTimerMinutes.setProgress(60); tvMinutesValue.setText("60 MIN"); });

        timerDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (timerDialog.getWindow() != null) {
            timerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnStartTimer.setOnClickListener(v -> {
            audioManager.startSleepTimer(selectedMinutes[0]);
            Toast.makeText(this, "Sleep timer set for " + selectedMinutes[0] + " minutes", Toast.LENGTH_SHORT).show();
            timerDialog.dismiss();
        });

        btnTurnOff.setOnClickListener(v -> {
            audioManager.stopSleepTimer();
            Toast.makeText(this, "Sleep timer disabled", Toast.LENGTH_SHORT).show();
            if (timerDialog != null) timerDialog.dismiss();
        });

        btnClose.setOnClickListener(v -> timerDialog.dismiss());
        timerDialog.show();
    }

    private void updateTimerStatusText(long remainingMs) {
        if (tvTimerStatusInDialog != null) {
            if (remainingMs > 0) {
                long minutes = remainingMs / 60000;
                long seconds = (remainingMs % 60000) / 1000;
                tvTimerStatusInDialog.setText(String.format(Locale.US, "Active timer: %02d:%02d remaining", minutes, seconds));
            } else {
                tvTimerStatusInDialog.setText("Select duration to turn off audio:");
            }
        }
    }

    private void showEqualizerDialog() {
        Equalizer eq = audioManager.getSpatialEngine().getEqualizer();
        if (eq == null) {
            Toast.makeText(this, "Equalizer not ready yet. Please play music first.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_equalizer, null);
        Spinner spPresets = dialogView.findViewById(R.id.spEqPresets);
        LinearLayout llBands = dialogView.findViewById(R.id.llEqBandsContainer);
        Button btnResetEq = dialogView.findViewById(R.id.btnResetEq);
        Button btnClose = dialogView.findViewById(R.id.btnCloseEq);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        short numPresets = eq.getNumberOfPresets();
        List<String> presetNames = new ArrayList<>();
        for (short i = 0; i < numPresets; i++) {
            presetNames.add(eq.getPresetName(i));
        }

        ArrayAdapter<String> eqAdapter = new ArrayAdapter<String>(this, R.layout.item_spinner_selected, presetNames) {
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ContextCompat.getColor(PlayerActivity.this, R.color.text_primary));
                    view.setBackgroundColor(ContextCompat.getColor(PlayerActivity.this, R.color.bg_card));
                }
                return view;
            }
        };
        eqAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spPresets.setAdapter(eqAdapter);
        try {
            spPresets.setSelection(eq.getCurrentPreset());
        } catch (Exception ignored) {}

        spPresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    eq.usePreset((short) position);
                } catch (Exception ignored) {}
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        String[] bandDescriptions = {
                "Sub-Bass (Kick / Sub)",
                "Bass (Warmth / Groove)",
                "Midrange (Vocals / Lead)",
                "High-Mid (Clarity / Presence)",
                "Treble (Air / Crispiness)"
        };

        short numBands = eq.getNumberOfBands();
        final short minEqLevel = eq.getBandLevelRange()[0];
        final short maxEqLevel = eq.getBandLevelRange()[1];
        final List<SeekBar> bandSeekBars = new ArrayList<>();

        for (short i = 0; i < numBands; i++) {
            final short bandIndex = i;
            int centerFreq = eq.getCenterFreq(bandIndex) / 1000;
            String freqStr = centerFreq >= 1000 ? (centerFreq / 1000) + " kHz" : centerFreq + " Hz";
            String desc = (i < bandDescriptions.length) ? bandDescriptions[i] : "Band " + (i + 1);

            LinearLayout bandRow = new LinearLayout(this);
            bandRow.setOrientation(LinearLayout.VERTICAL);
            bandRow.setPadding(0, 6, 0, 4);

            LinearLayout textRow = new LinearLayout(this);
            textRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvBandName = new TextView(this);
            tvBandName.setText(freqStr + " • " + desc);
            tvBandName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tvBandName.setTextSize(12f);
            tvBandName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvGain = new TextView(this);
            short curLevel = eq.getBandLevel(bandIndex);
            tvGain.setText((curLevel / 100) + " dB");
            tvGain.setTextColor(ContextCompat.getColor(this, R.color.secondary));
            tvGain.setTextSize(12f);

            textRow.addView(tvBandName);
            textRow.addView(tvGain);

            SeekBar sbBand = new SeekBar(this);
            sbBand.setMax(maxEqLevel - minEqLevel);
            sbBand.setProgress(curLevel - minEqLevel);
            sbBand.setProgressTintList(ContextCompat.getColorStateList(this, R.color.secondary));
            sbBand.setThumbTintList(ContextCompat.getColorStateList(this, R.color.secondary));
            bandSeekBars.add(sbBand);

            sbBand.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    short newLevel = (short) (progress + minEqLevel);
                    tvGain.setText((newLevel / 100) + " dB");
                    if (fromUser) {
                        eq.setBandLevel(bandIndex, newLevel);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            bandRow.addView(textRow);
            bandRow.addView(sbBand);
            llBands.addView(bandRow);
        }

        btnResetEq.setOnClickListener(v -> {
            try {
                for (short i = 0; i < numBands; i++) {
                    eq.setBandLevel(i, (short) 0);
                    if (i < bandSeekBars.size()) {
                        bandSeekBars.get(i).setProgress(0 - minEqLevel);
                    }
                }
                Toast.makeText(this, "Equalizer reset to Flat (0 dB)", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String formatTime(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / (1000 * 60)) % 60;
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    @Override
    public void onSongChanged(MusicModel song, int index) {
        if (song != null) {
            tvPlayerTitle.setText(song.getTitle());
            tvPlayerArtist.setText(song.getArtist());
            updateFavoriteUI();
            if (queueAdapter != null) {
                queueAdapter.setCurrentPlayingIndex(index);
                scrollToCurrentPlaying();
            }
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        btnPlayerPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    public void onProgressUpdate(int currentPos, int totalDuration) {
        if (!isUserSeeking) {
            sbPlayerProgress.setMax(totalDuration);
            sbPlayerProgress.setProgress(currentPos);
            tvPlayerCurrentTime.setText(formatTime(currentPos));
            tvPlayerTotalDuration.setText(formatTime(totalDuration));
        }
    }

    @Override
    public void onSpatialModeChanged(Spatial8DEngine.AudioMode mode) {
        updateModeUI(mode);
    }

    @Override
    public void onLoopModeChanged(AudioPlaybackManager.LoopMode mode) {
        updateLoopModeUI(mode);
    }

    @Override
    public void onTimerTick(long millisUntilFinished) {
        updateTimerStatusText(millisUntilFinished);
    }

    @Override
    public void onTimerFinished() {
        updateTimerStatusText(0);
        btnPlayerPlayPause.setImageResource(R.drawable.ic_play);
    }

    @Override
    public void onBackPressed() {
        if (queueBehavior != null && queueBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            queueBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        audioManager.addCallback(this);
        audioManager.addTimerCallback(this);
        updateUIFromState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        audioManager.removeCallback(this);
        audioManager.removeTimerCallback(this);
    }
}
