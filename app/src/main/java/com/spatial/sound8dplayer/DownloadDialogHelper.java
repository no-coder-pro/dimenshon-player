package com.spatial.sound8dplayer;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadDialogHelper {

    private final Context context;
    private final OnlineMusicApiService apiService;
    private final ImageLoader imageLoader;
    private final AudioPlaybackManager audioManager;
    private String selectedQuality = "128";

    public DownloadDialogHelper(Context context) {
        this.context = context;
        this.apiService = new OnlineMusicApiService();
        this.imageLoader = ImageLoader.getInstance();
        this.audioManager = AudioPlaybackManager.getInstance();
    }

    public void showDownloadDialog(OnlineSongModel song) {
        if (song == null) return;
        showDownloadDialog(song.getUrl(), song.getTitle(), song.getArtist(), song.getThumbnailUrl(), song.getSource(), song.getDurationStr());
    }

    public void showDownloadDialogForUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        String source = OnlineMusicApiService.detectUrlSource(url);
        if (source == null) source = "youtube";

        String defaultTitle = "Loading Track Info...";
        String defaultArtist = source.equalsIgnoreCase("spotify") ? "Spotify Track" : "YouTube Audio";
        showDownloadDialog(url, defaultTitle, defaultArtist, "", source, "");
    }

    public void showDownloadDialog(String rawUrl, String initialTitle, String initialArtist, String initialThumb, String source, String duration) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_download_options, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnClose = view.findViewById(R.id.btnCloseDialog);
        ImageView ivThumb = view.findViewById(R.id.ivDialogThumb);
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvArtist = view.findViewById(R.id.tvDialogArtist);
        TextView tvSource = view.findViewById(R.id.tvDialogSource);
        TextView tvDuration = view.findViewById(R.id.tvDialogDuration);

        LinearLayout llQualityContainer = view.findViewById(R.id.llQualityContainer);
        LinearLayout llSpotifyInfoContainer = view.findViewById(R.id.llSpotifyInfoContainer);

        LinearLayout opt128 = view.findViewById(R.id.optQuality128);
        LinearLayout opt192 = view.findViewById(R.id.optQuality192);
        LinearLayout opt320 = view.findViewById(R.id.optQuality320);

        TextView tvQ128 = view.findViewById(R.id.tvQuality128);
        TextView tvQ192 = view.findViewById(R.id.tvQuality192);
        TextView tvQ320 = view.findViewById(R.id.tvQuality320);

        TextView tvSubQ128 = view.findViewById(R.id.tvSubQuality128);
        TextView tvSubQ192 = view.findViewById(R.id.tvSubQuality192);
        TextView tvSubQ320 = view.findViewById(R.id.tvSubQuality320);

        Button btnStartDownload = view.findViewById(R.id.btnStartDownload);
        LinearLayout llLoading = view.findViewById(R.id.llLoadingTunnel);
        TextView tvLoadingStatus = view.findViewById(R.id.tvLoadingStatus);

        final String[] resolvedTargetUrl = { rawUrl };
        final String[] resolvedTitle = { (initialTitle != null && !initialTitle.isEmpty()) ? initialTitle : "Audio Track" };
        final String[] resolvedArtist = { (initialArtist != null && !initialArtist.isEmpty()) ? initialArtist : "Online Music" };

        tvTitle.setText(resolvedTitle[0]);
        tvArtist.setText(resolvedArtist[0]);

        boolean isSpotify = "spotify".equalsIgnoreCase(source);
        if (isSpotify) {
            tvSource.setText("Spotify");
            tvSource.setTextColor(Color.parseColor("#1DB954"));
            if (llQualityContainer != null) llQualityContainer.setVisibility(View.GONE);
            if (llSpotifyInfoContainer != null) llSpotifyInfoContainer.setVisibility(View.VISIBLE);
        } else {
            tvSource.setText("YouTube");
            tvSource.setTextColor(Color.parseColor("#FF3366"));
            if (llQualityContainer != null) llQualityContainer.setVisibility(View.VISIBLE);
            if (llSpotifyInfoContainer != null) llSpotifyInfoContainer.setVisibility(View.GONE);
        }

        if (duration != null && !duration.isEmpty()) {
            tvDuration.setText(duration);
            tvDuration.setVisibility(View.VISIBLE);
        } else {
            tvDuration.setVisibility(View.GONE);
        }

        if (initialThumb != null && !initialThumb.isEmpty()) {
            imageLoader.displayImage(initialThumb, ivThumb, R.drawable.ic_music_note);
        } else {
            ivThumb.setImageResource(R.drawable.ic_music_note);
        }

        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            apiService.fetchUrlMetadata(rawUrl, new OnlineMusicApiService.ApiCallback<OnlineSongModel>() {
                @Override
                public void onSuccess(OnlineSongModel result) {
                    if (result != null) {
                        resolvedTitle[0] = result.getTitle();
                        resolvedArtist[0] = result.getArtist();
                        resolvedTargetUrl[0] = result.getUrl();

                        tvTitle.setText(result.getTitle());
                        tvArtist.setText(result.getArtist());
                        if (!result.getDurationStr().isEmpty()) {
                            tvDuration.setText(result.getDurationStr());
                            tvDuration.setVisibility(View.VISIBLE);
                        }
                        if (result.getThumbnailUrl() != null && !result.getThumbnailUrl().isEmpty()) {
                            imageLoader.displayImage(result.getThumbnailUrl(), ivThumb, R.drawable.ic_music_note);
                        }
                    }
                }

                @Override
                public void onError(String errorMessage) {}
            });
        }

        selectedQuality = "128";
        Runnable updateQualityUI = () -> {
            opt128.setBackgroundResource("128".equals(selectedQuality) ? R.drawable.bg_card_rounded : R.drawable.bg_card_light);
            opt192.setBackgroundResource("192".equals(selectedQuality) ? R.drawable.bg_card_rounded : R.drawable.bg_card_light);
            opt320.setBackgroundResource("320".equals(selectedQuality) ? R.drawable.bg_card_rounded : R.drawable.bg_card_light);

            if (tvQ128 != null) tvQ128.setTextColor("128".equals(selectedQuality) ? Color.WHITE : Color.parseColor("#9CA3AF"));
            if (tvQ192 != null) tvQ192.setTextColor("192".equals(selectedQuality) ? Color.WHITE : Color.parseColor("#9CA3AF"));
            if (tvQ320 != null) tvQ320.setTextColor("320".equals(selectedQuality) ? Color.WHITE : Color.parseColor("#9CA3AF"));

            if (tvSubQ128 != null) tvSubQ128.setTextColor("128".equals(selectedQuality) ? Color.parseColor("#00E5FF") : Color.parseColor("#6B7280"));
            if (tvSubQ192 != null) tvSubQ192.setTextColor("192".equals(selectedQuality) ? Color.parseColor("#00E5FF") : Color.parseColor("#6B7280"));
            if (tvSubQ320 != null) tvSubQ320.setTextColor("320".equals(selectedQuality) ? Color.parseColor("#00E5FF") : Color.parseColor("#6B7280"));
        };
        updateQualityUI.run();

        opt128.setOnClickListener(v -> { selectedQuality = "128"; updateQualityUI.run(); });
        opt192.setOnClickListener(v -> { selectedQuality = "192"; updateQualityUI.run(); });
        opt320.setOnClickListener(v -> { selectedQuality = "320"; updateQualityUI.run(); });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnStartDownload.setOnClickListener(v -> {
            btnStartDownload.setEnabled(false);
            btnStartDownload.setAlpha(0.6f);
            llLoading.setVisibility(View.VISIBLE);
            tvLoadingStatus.setText("Connecting to high-speed audio tunnel...");

            String dlUrl = resolvedTargetUrl[0];

            if (isSpotify) {
                String fallbackQ = resolvedTitle[0] + " " + resolvedArtist[0];
                apiService.fetchSpotifyDownload(dlUrl, fallbackQ, new OnlineMusicApiService.ApiCallback<OnlineMusicApiService.DownloadResult>() {
                    @Override
                    public void onSuccess(OnlineMusicApiService.DownloadResult result) {
                        dialog.dismiss();
                        String saveTitle = (result.title != null && !result.title.isEmpty()) ? result.title : resolvedTitle[0];
                        String saveArtist = (result.artist != null && !result.artist.isEmpty()) ? result.artist : resolvedArtist[0];
                        SongDownloadHelper.startDownload(context, result.downloadUrl, result.filename, saveTitle, saveArtist, "spotify");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnStartDownload.setEnabled(true);
                        btnStartDownload.setAlpha(1.0f);
                        llLoading.setVisibility(View.GONE);
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                apiService.fetchYoutubeDownload(dlUrl, selectedQuality, new OnlineMusicApiService.ApiCallback<OnlineMusicApiService.DownloadResult>() {
                    @Override
                    public void onSuccess(OnlineMusicApiService.DownloadResult result) {
                        dialog.dismiss();
                        String saveTitle = (result.title != null && !result.title.isEmpty()) ? result.title : resolvedTitle[0];
                        String saveArtist = (result.artist != null && !result.artist.isEmpty()) ? result.artist : resolvedArtist[0];
                        SongDownloadHelper.startDownload(context, result.downloadUrl, result.filename, saveTitle, saveArtist, "youtube");
                    }

                    @Override
                    public void onError(String errorMessage) {
                        btnStartDownload.setEnabled(true);
                        btnStartDownload.setAlpha(1.0f);
                        llLoading.setVisibility(View.GONE);
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private static String formatTimeMs(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(Locale.US, "%d:%02d", min, sec);
    }

    private static void startCachedPlayback(Context context, OnlineSongModel song, String streamUrl,
                                            AudioPlaybackManager audioManager, ProgressBar pbPlayerBuffering,
                                            ImageButton btnPreviewPlayPause, OnlineSongAdapter ad) {
        OnlineStreamCacheManager.getInstance().getCachedAudio(context, song.getId(), streamUrl, new OnlineStreamCacheManager.CacheCallback() {
            @Override
            public void onSuccess(File cachedAudioFile) {
                if (song.getId().equals(ad.getActivePreviewSongId())) {
                    String localUri = android.net.Uri.fromFile(cachedAudioFile).toString();
                    audioManager.playOnlineTrack(context, song, localUri);
                    pbPlayerBuffering.setVisibility(View.GONE);
                    btnPreviewPlayPause.setVisibility(View.VISIBLE);
                    btnPreviewPlayPause.setImageResource(R.drawable.ic_pause);
                    ad.setPreviewState(song.getId(), true, false);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (song.getId().equals(ad.getActivePreviewSongId())) {
                    audioManager.playOnlineTrack(context, song, streamUrl);
                    pbPlayerBuffering.setVisibility(View.GONE);
                    btnPreviewPlayPause.setVisibility(View.VISIBLE);
                    btnPreviewPlayPause.setImageResource(R.drawable.ic_pause);
                    ad.setPreviewState(song.getId(), true, false);
                }
            }
        });
    }

    public void showOnlineSearchDialog(String initialQuery, String initialSource) {
        Dialog searchDialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_online_search, null);
        searchDialog.setContentView(view);

        ImageButton btnClose = view.findViewById(R.id.btnCloseSearchDialog);
        EditText etInput = view.findViewById(R.id.etOnlineSearchInput);
        ImageButton btnClear = view.findViewById(R.id.btnClearOnlineSearch);
        Button btnSubmit = view.findViewById(R.id.btnSubmitOnlineSearch);

        LinearLayout btnSourceYT = view.findViewById(R.id.btnSourceYT);
        LinearLayout btnSourceSpotify = view.findViewById(R.id.btnSourceSpotify);
        TextView tvTabYTIssue = view.findViewById(R.id.tvTabYTIssue);
        TextView tvTabSpotifyIssue = view.findViewById(R.id.tvTabSpotifyIssue);

        RecyclerView rvResults = view.findViewById(R.id.rvOnlineResults);
        LinearLayout llLoading = view.findViewById(R.id.llOnlineLoading);
        TextView tvLoadingText = view.findViewById(R.id.tvOnlineLoadingText);
        LinearLayout llEmpty = view.findViewById(R.id.llOnlineEmpty);
        TextView tvEmptyTitle = view.findViewById(R.id.tvOnlineEmptyTitle);
        TextView tvEmptySubtitle = view.findViewById(R.id.tvOnlineEmptySubtitle);

        View cardPlayer = view.findViewById(R.id.cardOnlinePreviewPlayer);
        View rlPreviewContentArea = view.findViewById(R.id.rlPreviewContentArea);
        SeekBar sbPreview = view.findViewById(R.id.sbOnlinePreview);
        ImageView ivPreviewThumb = view.findViewById(R.id.ivPreviewThumb);
        TextView tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle);
        TextView tvPreviewBadge = view.findViewById(R.id.tvPreviewBadge);
        TextView tvPreviewTime = view.findViewById(R.id.tvPreviewTime);
        ImageButton btnPreviewPrev = view.findViewById(R.id.btnPreviewPrev);
        ImageButton btnPreviewPlayPause = view.findViewById(R.id.btnPreviewPlayPause);
        ProgressBar pbPlayerBuffering = view.findViewById(R.id.pbPreviewPlayerBuffering);
        ImageButton btnPreviewNext = view.findViewById(R.id.btnPreviewNext);
        ImageButton btnPreviewDownload = view.findViewById(R.id.btnPreviewDownload);
        ImageButton btnPreviewClose = view.findViewById(R.id.btnPreviewClose);

        if (tvPreviewTitle != null) tvPreviewTitle.setSelected(true);

        final String[] currentSource = { (initialSource != null && !initialSource.isEmpty()) ? initialSource : "youtube" };
        final List<OnlineSongModel> resultsList = new ArrayList<>();
        final OnlineSongModel[] currentPlayingSong = { null };
        final OnlineSongAdapter[] adapterRef = new OnlineSongAdapter[1];
        final boolean[] isUserSeeking = { false };

        AudioPlaybackManager.PlaybackCallback playbackCallback = new AudioPlaybackManager.PlaybackCallback() {
            @Override
            public void onSongChanged(MusicModel song, int index) {
                if (song != null) {
                    tvPreviewTitle.setText(song.getTitle());
                    int dur = audioManager.getDuration();
                    if (dur > 0) {
                        sbPreview.setMax(dur);
                        tvPreviewTime.setText("0:00 / " + formatTimeMs(dur));
                    } else if (song.getDuration() != null && !song.getDuration().isEmpty()) {
                        tvPreviewTime.setText("0:00 / " + song.getDuration());
                    }
                    boolean isPlay = audioManager.isPlaying();
                    btnPreviewPlayPause.setImageResource(isPlay ? R.drawable.ic_pause : R.drawable.ic_play);
                    OnlineSongAdapter ad = adapterRef[0];
                    if (ad != null && currentPlayingSong[0] != null) {
                        ad.setPreviewState(currentPlayingSong[0].getId(), isPlay, false);
                    }
                }
            }

            @Override
            public void onPlaybackStateChanged(boolean isPlaying) {
                btnPreviewPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
                pbPlayerBuffering.setVisibility(View.GONE);
                btnPreviewPlayPause.setVisibility(View.VISIBLE);
                OnlineSongAdapter ad = adapterRef[0];
                if (ad != null && currentPlayingSong[0] != null) {
                    ad.setPreviewState(currentPlayingSong[0].getId(), isPlaying, false);
                }
            }

            @Override
            public void onProgressUpdate(int currentPos, int totalDuration) {
                if (!isUserSeeking[0]) {
                    int dur = totalDuration > 0 ? totalDuration : audioManager.getDuration();
                    if (dur > 0) {
                        sbPreview.setMax(dur);
                        sbPreview.setProgress(currentPos);
                        tvPreviewTime.setText(formatTimeMs(currentPos) + " / " + formatTimeMs(dur));
                    } else {
                        tvPreviewTime.setText(formatTimeMs(currentPos) + " / 0:00");
                    }
                }
            }

            @Override
            public void onSpatialModeChanged(Spatial8DEngine.AudioMode mode) {
                if (tvPreviewBadge != null) {
                    switch (mode) {
                        case OFF:
                            tvPreviewBadge.setText("STEREO");
                            tvPreviewBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                            break;
                        case MODE_3D:
                            tvPreviewBadge.setText("3D SURROUND");
                            tvPreviewBadge.setTextColor(ContextCompat.getColor(context, R.color.secondary));
                            break;
                        case MODE_8D:
                            tvPreviewBadge.setText("8D ORBIT");
                            tvPreviewBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_pink));
                            break;
                        case MODE_16D:
                            tvPreviewBadge.setText("16D ULTRA");
                            tvPreviewBadge.setTextColor(ContextCompat.getColor(context, R.color.accent_pink));
                            break;
                    }
                }
            }

            @Override
            public void onLoopModeChanged(AudioPlaybackManager.LoopMode mode) {}
        };

        audioManager.addCallback(playbackCallback);

        final java.util.function.Consumer<OnlineSongModel> playOnlineSong = song -> {
            if (song == null) return;
            OnlineSongAdapter ad = adapterRef[0];
            if (ad == null) return;

            currentPlayingSong[0] = song;
            cardPlayer.setVisibility(View.VISIBLE);

            long initialDurMs = song.getDurationSec() > 0 ? (song.getDurationSec() * 1000L) : 0L;
            if (initialDurMs <= 0 && !song.getDurationStr().isEmpty()) {
                initialDurMs = OnlineMusicApiService.parseDurationStringToSec(song.getDurationStr()) * 1000L;
            }
            if (initialDurMs > 0) {
                sbPreview.setMax((int) initialDurMs);
            }
            sbPreview.setProgress(0);

            tvPreviewTitle.setText(song.getTitle());
            tvPreviewTime.setText("0:00 / " + (initialDurMs > 0 ? formatTimeMs(initialDurMs) : song.getDurationStr()));
            imageLoader.displayImage(song.getThumbnailUrl(), ivPreviewThumb, R.drawable.ic_headphone);

            btnPreviewPlayPause.setVisibility(View.GONE);
            pbPlayerBuffering.setVisibility(View.VISIBLE);
            ad.setPreviewState(song.getId(), false, true);

            if (song.isSpotify()) {
                String fallbackQ = song.getTitle() + " " + song.getArtist();
                apiService.fetchSpotifyDownload(song.getUrl(), fallbackQ, new OnlineMusicApiService.ApiCallback<OnlineMusicApiService.DownloadResult>() {
                    @Override
                    public void onSuccess(OnlineMusicApiService.DownloadResult result) {
                        if (song.getId().equals(ad.getActivePreviewSongId())) {
                            String streamUrl = result.downloadUrl;
                            if (streamUrl != null && !streamUrl.isEmpty()) {
                                startCachedPlayback(context, song, streamUrl, audioManager, pbPlayerBuffering, btnPreviewPlayPause, ad);
                            } else {
                                ad.stopPreview();
                                pbPlayerBuffering.setVisibility(View.GONE);
                                btnPreviewPlayPause.setVisibility(View.VISIBLE);
                                Toast.makeText(context, "Unable to stream this track", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (song.getId().equals(ad.getActivePreviewSongId())) {
                            ad.stopPreview();
                            pbPlayerBuffering.setVisibility(View.GONE);
                            btnPreviewPlayPause.setVisibility(View.VISIBLE);
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                apiService.fetchYoutubeDownload(song.getUrl(), "128", new OnlineMusicApiService.ApiCallback<OnlineMusicApiService.DownloadResult>() {
                    @Override
                    public void onSuccess(OnlineMusicApiService.DownloadResult result) {
                        if (song.getId().equals(ad.getActivePreviewSongId())) {
                            String streamUrl = result.downloadUrl;
                            if (streamUrl != null && !streamUrl.isEmpty()) {
                                startCachedPlayback(context, song, streamUrl, audioManager, pbPlayerBuffering, btnPreviewPlayPause, ad);
                            } else {
                                ad.stopPreview();
                                pbPlayerBuffering.setVisibility(View.GONE);
                                btnPreviewPlayPause.setVisibility(View.VISIBLE);
                                Toast.makeText(context, "Unable to stream this track", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (song.getId().equals(ad.getActivePreviewSongId())) {
                            ad.stopPreview();
                            pbPlayerBuffering.setVisibility(View.GONE);
                            btnPreviewPlayPause.setVisibility(View.VISIBLE);
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };

        rvResults.setLayoutManager(new LinearLayoutManager(context));
        OnlineSongAdapter adapter = new OnlineSongAdapter(resultsList, new OnlineSongAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(OnlineSongModel song) {
                handleItemTap(song);
            }

            @Override
            public void onDownloadClick(OnlineSongModel song) {
                showDownloadDialog(song);
            }

            @Override
            public void onPreviewPlayClick(OnlineSongModel song) {
                handleItemTap(song);
            }

            private void handleItemTap(OnlineSongModel song) {
                if (song == null) return;
                OnlineSongAdapter ad = adapterRef[0];
                if (ad == null) return;

                if (song.getId().equals(ad.getActivePreviewSongId())) {
                    audioManager.togglePlayPause(context);
                    return;
                }
                playOnlineSong.accept(song);
            }
        });

        adapterRef[0] = adapter;
        rvResults.setAdapter(adapter);

        if (rlPreviewContentArea != null) {
            rlPreviewContentArea.setOnClickListener(v -> {
                if (audioManager.getCurrentSong() != null) {
                    Intent intent = new Intent(context, PlayerActivity.class);
                    context.startActivity(intent);
                }
            });
        }

        btnPreviewPlayPause.setOnClickListener(v -> audioManager.togglePlayPause(context));

        btnPreviewPrev.setOnClickListener(v -> {
            if (currentPlayingSong[0] != null && !resultsList.isEmpty()) {
                int curIdx = -1;
                for (int i = 0; i < resultsList.size(); i++) {
                    if (resultsList.get(i).getId().equals(currentPlayingSong[0].getId())) {
                        curIdx = i;
                        break;
                    }
                }
                if (curIdx > 0) {
                    playOnlineSong.accept(resultsList.get(curIdx - 1));
                } else {
                    playOnlineSong.accept(resultsList.get(resultsList.size() - 1));
                }
            }
        });

        btnPreviewNext.setOnClickListener(v -> {
            if (currentPlayingSong[0] != null && !resultsList.isEmpty()) {
                int curIdx = -1;
                for (int i = 0; i < resultsList.size(); i++) {
                    if (resultsList.get(i).getId().equals(currentPlayingSong[0].getId())) {
                        curIdx = i;
                        break;
                    }
                }
                if (curIdx != -1 && curIdx < resultsList.size() - 1) {
                    playOnlineSong.accept(resultsList.get(curIdx + 1));
                } else {
                    playOnlineSong.accept(resultsList.get(0));
                }
            }
        });

        btnPreviewDownload.setOnClickListener(v -> {
            if (currentPlayingSong[0] != null) {
                showDownloadDialog(currentPlayingSong[0]);
            }
        });

        btnPreviewClose.setOnClickListener(v -> {
            audioManager.pausePlayback();
            cardPlayer.setVisibility(View.GONE);
            adapter.stopPreview();
        });

        sbPreview.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int totalDur = audioManager.getDuration();
                    if (totalDur <= 0) totalDur = seekBar.getMax();
                    tvPreviewTime.setText(formatTimeMs(progress) + " / " + formatTimeMs(totalDur > 0 ? totalDur : 0));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking[0] = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int targetPos = seekBar.getProgress();
                audioManager.seekTo(targetPos);
                isUserSeeking[0] = false;
            }
        });

        Runnable updateSourceTabs = () -> {
            boolean isYT = "youtube".equalsIgnoreCase(currentSource[0]);
            btnSourceYT.setBackgroundResource(isYT ? R.drawable.bg_card_rounded : R.drawable.bg_card_light);
            if (tvTabYTIssue != null) tvTabYTIssue.setTextColor(isYT ? Color.WHITE : Color.parseColor("#9CA3AF"));

            btnSourceSpotify.setBackgroundResource(!isYT ? R.drawable.bg_card_rounded : R.drawable.bg_card_light);
            if (tvTabSpotifyIssue != null) tvTabSpotifyIssue.setTextColor(!isYT ? Color.WHITE : Color.parseColor("#9CA3AF"));
        };
        updateSourceTabs.run();

        Runnable performSearch = () -> {
            String q = etInput.getText().toString().trim();
            if (q.isEmpty()) return;

            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etInput.getWindowToken(), 0);

            String detectedSource = OnlineMusicApiService.detectUrlSource(q);
            if (detectedSource != null) {
                showDownloadDialogForUrl(q);
                return;
            }

            resultsList.clear();
            adapter.notifyDataSetChanged();
            llEmpty.setVisibility(View.GONE);
            llLoading.setVisibility(View.VISIBLE);
            tvLoadingText.setText("Searching " + ("youtube".equalsIgnoreCase(currentSource[0]) ? "YouTube" : "Spotify") + " for \"" + q + "\"...");

            if ("youtube".equalsIgnoreCase(currentSource[0])) {
                apiService.searchYoutube(q, 15, new OnlineMusicApiService.ApiCallback<List<OnlineSongModel>>() {
                    @Override
                    public void onSuccess(List<OnlineSongModel> result) {
                        llLoading.setVisibility(View.GONE);
                        resultsList.clear();
                        if (result != null) resultsList.addAll(result);
                        adapter.notifyDataSetChanged();

                        if (resultsList.isEmpty()) {
                            llEmpty.setVisibility(View.VISIBLE);
                            tvEmptyTitle.setText("No YouTube songs found");
                            tvEmptySubtitle.setText("Try searching with different keywords or artist name");
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        llLoading.setVisibility(View.GONE);
                        llEmpty.setVisibility(View.VISIBLE);
                        tvEmptyTitle.setText("Search Error");
                        tvEmptySubtitle.setText(errorMessage);
                    }
                });
            } else {
                apiService.searchSpotify(q, 15, new OnlineMusicApiService.ApiCallback<List<OnlineSongModel>>() {
                    @Override
                    public void onSuccess(List<OnlineSongModel> result) {
                        llLoading.setVisibility(View.GONE);
                        resultsList.clear();
                        if (result != null) resultsList.addAll(result);
                        adapter.notifyDataSetChanged();

                        if (resultsList.isEmpty()) {
                            llEmpty.setVisibility(View.VISIBLE);
                            tvEmptyTitle.setText("No Spotify songs found");
                            tvEmptySubtitle.setText("Try searching with different keywords or artist name");
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        llLoading.setVisibility(View.GONE);
                        llEmpty.setVisibility(View.VISIBLE);
                        tvEmptyTitle.setText("Search Error");
                        tvEmptySubtitle.setText(errorMessage);
                    }
                });
            }
        };

        btnSourceYT.setOnClickListener(v -> {
            if (!"youtube".equalsIgnoreCase(currentSource[0])) {
                currentSource[0] = "youtube";
                updateSourceTabs.run();
                if (!etInput.getText().toString().trim().isEmpty()) {
                    performSearch.run();
                }
            }
        });

        btnSourceSpotify.setOnClickListener(v -> {
            if (!"spotify".equalsIgnoreCase(currentSource[0])) {
                currentSource[0] = "spotify";
                updateSourceTabs.run();
                if (!etInput.getText().toString().trim().isEmpty()) {
                    performSearch.run();
                }
            }
        });

        btnSubmit.setOnClickListener(v -> performSearch.run());

        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performSearch.run();
                return true;
            }
            return false;
        });

        etInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        btnClear.setOnClickListener(v -> etInput.setText(""));
        btnClose.setOnClickListener(v -> searchDialog.dismiss());

        searchDialog.setOnDismissListener(dialog -> {
            audioManager.removeCallback(playbackCallback);
        });

        if (initialQuery != null && !initialQuery.trim().isEmpty()) {
            etInput.setText(initialQuery.trim());
            new Handler(Looper.getMainLooper()).postDelayed(performSearch, 150);
        }

        searchDialog.show();
    }
}
