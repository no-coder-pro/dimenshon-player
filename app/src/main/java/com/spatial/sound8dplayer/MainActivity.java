package com.spatial.sound8dplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AudioPlaybackManager.PlaybackCallback {

    private static final int PERMISSION_REQ_CODE = 2001;

    public enum SortType {
        TITLE_AZ, TITLE_ZA, DATE_ADDED, DURATION, ARTIST
    }

    private DrawerLayout drawerLayout;
    private LinearLayout llHeaderLogo;
    private LinearLayout llDrawerDeveloper;
    private LinearLayout llDrawerChannel;

    private EditText etSearch;
    private ImageButton btnClearSearch;
    private ImageButton btnMainOnlineSearch;
    private ImageButton btnOpenFile;
    private ImageButton btnMainSleepTimer;
    private Button btnTabAll, btnTabFavorites;
    private LinearLayout llSortBtn;
    private TextView tvSongCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvSongs;
    private FastScrollIndexBar fastScrollIndexBar;
    private TextView tvLetterBubble;
    private LinearLayout llEmptyState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private LinearLayout llEmptyOnlineOptions;
    private LinearLayout btnEmptySearchYT;
    private LinearLayout btnEmptySearchSpotify;

    private LinearLayout miniPlayerCard;
    private RelativeLayout rlMiniContentArea;
    private SeekBar miniSeekBar;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private TextView tvMiniModeBadge;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniNext;
    private ImageButton btnMiniPrev;

    private final List<MusicModel> allSongsList = new ArrayList<>();
    private final List<MusicModel> currentDisplayList = new ArrayList<>();
    private final int[] letterPositions = new int[27];
    private MusicAdapter musicAdapter;
    private SortType currentSortType = SortType.TITLE_AZ;

    private AudioPlaybackManager audioManager;
    private FavoritesManager favoritesManager;
    private SongMetadataManager metadataManager;
    private DownloadDialogHelper downloadDialogHelper;
    private MusicModel pendingRingtoneSong = null;
    private boolean isShowingFavoritesOnly = false;
    private boolean isUserSeekingMini = false;
    private final Handler bubbleHandler = new Handler(Looper.getMainLooper());
    private final Handler miniSeekHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    MusicModel customSong = new MusicModel(
                            System.currentTimeMillis(),
                            "Selected Audio Track",
                            "Local Audio",
                            "Custom Album",
                            "",
                            0L,
                            0L,
                            System.currentTimeMillis() / 1000L,
                            uri.toString(),
                            uri
                    );
                    metadataManager.applySavedMetadata(customSong);
                    allSongsList.add(0, customSong);
                    filterSongs(etSearch.getText().toString());
                    audioManager.setPlaylist(allSongsList);
                    audioManager.playSong(this, 0);
                    openPlayerScreen();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = AudioPlaybackManager.getInstance();
        favoritesManager = FavoritesManager.getInstance(this);
        metadataManager = new SongMetadataManager(this);
        downloadDialogHelper = new DownloadDialogHelper(this);

        SongDownloadHelper.setGlobalDownloadListener((title, path) -> runOnUiThread(this::loadDeviceAudioFiles));

        initViews();
        setupSideDrawer();
        setupSearch();
        setupTabs();
        setupFastScroll();
        setupSwipeRefresh();
        setupMiniPlayer();
        checkPermissionsAndLoadSongs();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            String url = OnlineMusicApiService.extractUrlFromText(sharedText);
            if (url != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (downloadDialogHelper != null) {
                        downloadDialogHelper.showDownloadDialogForUrl(url);
                    }
                }, 300);
            }
        } else if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            String url = intent.getData().toString();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (downloadDialogHelper != null) {
                    downloadDialogHelper.showDownloadDialogForUrl(url);
                }
            }, 300);
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        llHeaderLogo = findViewById(R.id.llHeaderLogo);
        llDrawerDeveloper = findViewById(R.id.llDrawerDeveloper);
        llDrawerChannel = findViewById(R.id.llDrawerChannel);

        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnMainOnlineSearch = findViewById(R.id.btnMainOnlineSearch);
        btnOpenFile = findViewById(R.id.btnOpenFile);
        btnMainSleepTimer = findViewById(R.id.btnMainSleepTimer);
        btnTabAll = findViewById(R.id.btnTabAll);
        btnTabFavorites = findViewById(R.id.btnTabFavorites);
        llSortBtn = findViewById(R.id.llSortBtn);
        tvSongCount = findViewById(R.id.tvSongCount);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvSongs = findViewById(R.id.rvSongs);
        fastScrollIndexBar = findViewById(R.id.fastScrollIndexBar);
        tvLetterBubble = findViewById(R.id.tvLetterBubble);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        llEmptyOnlineOptions = findViewById(R.id.llEmptyOnlineOptions);
        btnEmptySearchYT = findViewById(R.id.btnEmptySearchYT);
        btnEmptySearchSpotify = findViewById(R.id.btnEmptySearchSpotify);

        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        rlMiniContentArea = findViewById(R.id.rlMiniContentArea);
        miniSeekBar = findViewById(R.id.miniSeekBar);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        tvMiniModeBadge = findViewById(R.id.tvMiniModeBadge);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        btnMiniPrev = findViewById(R.id.btnMiniPrev);

        if (btnMainOnlineSearch != null) {
            btnMainOnlineSearch.setOnClickListener(v -> {
                String q = etSearch.getText().toString().trim();
                downloadDialogHelper.showOnlineSearchDialog(q, "youtube");
            });
        }

        if (btnEmptySearchYT != null) {
            btnEmptySearchYT.setOnClickListener(v -> {
                String q = etSearch.getText().toString().trim();
                downloadDialogHelper.showOnlineSearchDialog(q, "youtube");
            });
        }

        if (btnEmptySearchSpotify != null) {
            btnEmptySearchSpotify.setOnClickListener(v -> {
                String q = etSearch.getText().toString().trim();
                downloadDialogHelper.showOnlineSearchDialog(q, "spotify");
            });
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvSongs.setLayoutManager(layoutManager);
        rvSongs.setHasFixedSize(true);
        rvSongs.setItemViewCacheSize(30);
        rvSongs.getRecycledViewPool().setMaxRecycledViews(0, 35);

        musicAdapter = new MusicAdapter(currentDisplayList, favoritesManager, new MusicAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(MusicModel song, int position) {
                int index = allSongsList.indexOf(song);
                if (index != -1) {
                    audioManager.setPlaylist(allSongsList);
                    audioManager.playSong(MainActivity.this, index);
                }
            }

            @Override
            public void onFavoriteClick(MusicModel song, int position) {
                if (isShowingFavoritesOnly) {
                    filterSongs(etSearch.getText().toString());
                }
            }

            @Override
            public void onMenuClick(MusicModel song, int position, View anchorView) {
                showSongMenuDialog(song, position);
            }
        });
        rvSongs.setAdapter(musicAdapter);

        tvMiniTitle.setSelected(true);

        if (llSortBtn != null) {
            llSortBtn.setOnClickListener(v -> showSortDialog());
        }
        if (tvSongCount != null) {
            tvSongCount.setOnClickListener(v -> showSortDialog());
        }

        btnOpenFile.setOnClickListener(v -> filePickerLauncher.launch("audio/*"));
        btnMainSleepTimer.setOnClickListener(v -> showSleepTimerDialog());
    }

    private void setupSideDrawer() {
        llHeaderLogo.setOnClickListener(v -> {
            if (drawerLayout != null) {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            }
        });

        if (llDrawerDeveloper != null) {
            llDrawerDeveloper.setOnClickListener(v -> openTelegramLink("no_coder_pro"));
        }

        if (llDrawerChannel != null) {
            llDrawerChannel.setOnClickListener(v -> openTelegramLink("no_coder_xone"));
        }
    }

    private void openTelegramLink(String username) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/" + username));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open Telegram link: @" + username, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
                Color.parseColor("#8A2BE2"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#FF007F")
        );
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#161824"));

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadDeviceAudioFiles();
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Refreshed: " + allSongsList.size() + " songs found", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupFastScroll() {
        fastScrollIndexBar.setOnLetterTouchListener((letter, index) -> {
            tvLetterBubble.setText(letter);
            tvLetterBubble.setVisibility(View.VISIBLE);
            bubbleHandler.removeCallbacksAndMessages(null);
            bubbleHandler.postDelayed(() -> tvLetterBubble.setVisibility(View.GONE), 800);

            if (index >= 0 && index < letterPositions.length) {
                int pos = letterPositions[index];
                if (pos != -1 && pos < currentDisplayList.size()) {
                    ((LinearLayoutManager) rvSongs.getLayoutManager()).scrollToPositionWithOffset(pos, 0);
                }
            }
        });
    }

    private void setupTabs() {
        btnTabAll.setOnClickListener(v -> {
            isShowingFavoritesOnly = false;
            updateTabUI();
            filterSongs(etSearch.getText().toString());
        });

        btnTabFavorites.setOnClickListener(v -> {
            isShowingFavoritesOnly = true;
            updateTabUI();
            filterSongs(etSearch.getText().toString());
        });
    }

    private void updateTabUI() {
        int activeBg = ContextCompat.getColor(this, R.color.primary);
        int inactiveBg = ContextCompat.getColor(this, R.color.bg_card_light);
        int textActive = ContextCompat.getColor(this, R.color.text_primary);
        int textInactive = ContextCompat.getColor(this, R.color.text_secondary);

        btnTabAll.setBackgroundColor(!isShowingFavoritesOnly ? activeBg : inactiveBg);
        btnTabAll.setTextColor(!isShowingFavoritesOnly ? textActive : textInactive);

        btnTabFavorites.setBackgroundColor(isShowingFavoritesOnly ? activeBg : inactiveBg);
        btnTabFavorites.setTextColor(isShowingFavoritesOnly ? textActive : textInactive);
    }

    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                String detectedSource = OnlineMusicApiService.detectUrlSource(query);
                if (detectedSource != null) {
                    btnClearSearch.setVisibility(View.VISIBLE);
                    downloadDialogHelper.showDownloadDialogForUrl(query);
                    return;
                }

                if (searchRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> filterSongs(query);
                if (query.isEmpty()) {
                    filterSongs("");
                } else {
                    searchDebounceHandler.postDelayed(searchRunnable, 60);
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    public static String normalizeSearchText(String input) {
        if (input == null || input.isEmpty()) return "";
        String str = input;
        if (str.contains("%")) {
            try {
                str = URLDecoder.decode(str, "UTF-8");
            } catch (Exception ignored) {}
        }
        StringBuilder sb = new StringBuilder(str.length());
        boolean lastWasSpace = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
                lastWasSpace = false;
            } else {
                if (!lastWasSpace) {
                    sb.append(' ');
                    lastWasSpace = true;
                }
            }
        }
        return sb.toString().trim();
    }

    private void filterSongs(String query) {
        currentDisplayList.clear();

        if (query == null || query.trim().isEmpty()) {
            for (MusicModel song : allSongsList) {
                if (!isShowingFavoritesOnly || favoritesManager.isFavorite(song.getId())) {
                    currentDisplayList.add(song);
                }
            }
        } else {
            String normQuery = normalizeSearchText(query);
            String tightQuery = normQuery.replace(" ", "");
            String[] queryWords = normQuery.split(" ");

            for (MusicModel song : allSongsList) {
                if (isShowingFavoritesOnly && !favoritesManager.isFavorite(song.getId())) {
                    continue;
                }

                String songKey = song.getNormalizedSearchKey();
                String songKeyNoSpace = song.getNormalizedSearchKeyNoSpace();

                boolean isMatch = false;
                if (songKey.contains(normQuery) || songKeyNoSpace.contains(tightQuery)) {
                    isMatch = true;
                } else if (queryWords.length > 1) {
                    boolean allWords = true;
                    for (String word : queryWords) {
                        if (!word.isEmpty() && !songKey.contains(word)) {
                            allWords = false;
                            break;
                        }
                    }
                    isMatch = allWords;
                }

                if (isMatch) {
                    currentDisplayList.add(song);
                }
            }
        }

        switch (currentSortType) {
            case TITLE_AZ:
                Collections.sort(currentDisplayList, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
            case TITLE_ZA:
                Collections.sort(currentDisplayList, (a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;
            case DATE_ADDED:
                Collections.sort(currentDisplayList, (a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
                break;
            case DURATION:
                Collections.sort(currentDisplayList, (a, b) -> Long.compare(b.getDurationMs(), a.getDurationMs()));
                break;
            case ARTIST:
                Collections.sort(currentDisplayList, (a, b) -> a.getArtist().compareToIgnoreCase(b.getArtist()));
                break;
        }

        Arrays.fill(letterPositions, -1);
        for (int i = 0; i < currentDisplayList.size(); i++) {
            String title = currentDisplayList.get(i).getTitle().trim();
            if (title.isEmpty()) continue;
            char c = Character.toUpperCase(title.charAt(0));
            int alphaIdx;
            if (c >= 'A' && c <= 'Z') {
                alphaIdx = c - 'A' + 1;
            } else {
                alphaIdx = 0;
            }
            if (letterPositions[alphaIdx] == -1) {
                letterPositions[alphaIdx] = i;
            }
        }

        musicAdapter.notifyDataSetChanged();
        updateSongCount(query);
    }

    private void updateSongCount() {
        updateSongCount(etSearch != null ? etSearch.getText().toString() : "");
    }

    private void updateSongCount(String query) {
        String label = isShowingFavoritesOnly ? "Favorites" : "Songs";
        tvSongCount.setText(currentDisplayList.size() + " " + label + " ▾");

        if (currentDisplayList.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            String q = (query != null) ? query.trim() : "";
            if (q.isEmpty()) {
                if (tvEmptyTitle != null) tvEmptyTitle.setText(isShowingFavoritesOnly ? "No favorite songs yet" : "No songs found");
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText(isShowingFavoritesOnly ? "Tap the star icon on any song to add it to favorites" : "Pull down to refresh or open audio files");
                if (llEmptyOnlineOptions != null) llEmptyOnlineOptions.setVisibility(View.GONE);
            } else {
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No local songs for \"" + q + "\"");
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText("Search & download from YouTube or Spotify:");
                if (llEmptyOnlineOptions != null) llEmptyOnlineOptions.setVisibility(View.VISIBLE);
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void showSortDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sort_options, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageView ivAZ = dialogView.findViewById(R.id.ivSortCheckAZ);
        ImageView ivZA = dialogView.findViewById(R.id.ivSortCheckZA);
        ImageView ivDate = dialogView.findViewById(R.id.ivSortCheckDate);
        ImageView ivDur = dialogView.findViewById(R.id.ivSortCheckDuration);
        ImageView ivArtist = dialogView.findViewById(R.id.ivSortCheckArtist);

        ivAZ.setVisibility(currentSortType == SortType.TITLE_AZ ? View.VISIBLE : View.GONE);
        ivZA.setVisibility(currentSortType == SortType.TITLE_ZA ? View.VISIBLE : View.GONE);
        ivDate.setVisibility(currentSortType == SortType.DATE_ADDED ? View.VISIBLE : View.GONE);
        ivDur.setVisibility(currentSortType == SortType.DURATION ? View.VISIBLE : View.GONE);
        ivArtist.setVisibility(currentSortType == SortType.ARTIST ? View.VISIBLE : View.GONE);

        dialogView.findViewById(R.id.sortTitleAZ).setOnClickListener(v -> {
            currentSortType = SortType.TITLE_AZ;
            filterSongs(etSearch.getText().toString());
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.sortTitleZA).setOnClickListener(v -> {
            currentSortType = SortType.TITLE_ZA;
            filterSongs(etSearch.getText().toString());
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.sortDateAdded).setOnClickListener(v -> {
            currentSortType = SortType.DATE_ADDED;
            filterSongs(etSearch.getText().toString());
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.sortDuration).setOnClickListener(v -> {
            currentSortType = SortType.DURATION;
            filterSongs(etSearch.getText().toString());
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.sortArtist).setOnClickListener(v -> {
            currentSortType = SortType.ARTIST;
            filterSongs(etSearch.getText().toString());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showSongMenuDialog(MusicModel song, int position) {
        if (song == null) return;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_song_options, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogSongTitle);
        TextView tvArtist = dialogView.findViewById(R.id.tvDialogSongArtist);
        ImageView ivFav = dialogView.findViewById(R.id.ivOptFavIcon);
        TextView tvFav = dialogView.findViewById(R.id.tvOptFavText);

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());

        boolean isFav = favoritesManager.isFavorite(song.getId());
        ivFav.setImageResource(isFav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        tvFav.setText(isFav ? "Remove from Favorites" : "Add to Favorites ⭐");

        dialogView.findViewById(R.id.optSetRingtone).setOnClickListener(v -> {
            dialog.dismiss();
            setSongAsRingtone(song);
        });

        dialogView.findViewById(R.id.optShareSong).setOnClickListener(v -> {
            dialog.dismiss();
            shareSong(song);
        });

        dialogView.findViewById(R.id.optSongDetails).setOnClickListener(v -> {
            dialog.dismiss();
            showSongDetailsDialog(song);
        });

        dialogView.findViewById(R.id.optToggleFav).setOnClickListener(v -> {
            boolean nowFav = favoritesManager.toggleFavorite(song.getId());
            musicAdapter.notifyItemChanged(position);
            if (isShowingFavoritesOnly) {
                filterSongs(etSearch.getText().toString());
            }
            Toast.makeText(this, nowFav ? "Added to Favorites ⭐" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.optDeleteSong).setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteSong(song);
        });

        dialog.show();
    }

    private void confirmDeleteSong(MusicModel song) {
        if (song == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Track?")
                .setMessage("Are you sure you want to delete \"" + song.getTitle() + "\" from your device?")
                .setPositiveButton("Delete", (d, w) -> deleteSongPermanently(song))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSongPermanently(MusicModel song) {
        if (song == null) return;

        MusicModel current = audioManager.getCurrentSong();
        if (current != null && current.getId() == song.getId()) {
            audioManager.stopPlayback();
            miniPlayerCard.setVisibility(View.GONE);
        }

        try {
            if (song.getDataPath() != null && !song.getDataPath().isEmpty()) {
                File file = new File(song.getDataPath());
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception ignored) {}

        try {
            getContentResolver().delete(song.getUri(), null, null);
        } catch (Exception ignored) {}

        favoritesManager.removeFavorite(song.getId());
        allSongsList.remove(song);
        currentDisplayList.remove(song);
        audioManager.setPlaylist(allSongsList);

        musicAdapter.notifyDataSetChanged();
        updateSongCount();

        Toast.makeText(this, "🗑️ Track deleted from device", Toast.LENGTH_SHORT).show();
    }

    private void setSongAsRingtone(MusicModel song) {
        if (song == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.System.canWrite(this)) {
                pendingRingtoneSong = song;
                new AlertDialog.Builder(this)
                        .setTitle("System Permission Required")
                        .setMessage("To set this track as your phone ringtone, please turn ON 'Allow modify system settings' on the next screen, then press Back.")
                        .setPositiveButton("Continue", (d, w) -> {
                            try {
                                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(this, "Unable to open system settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", (d, w) -> pendingRingtoneSong = null)
                        .show();
                return;
            }
        }
        applyRingtone(song);
    }

    private void applyRingtone(MusicModel song) {
        if (song == null) return;
        try {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                values.put(MediaStore.Audio.Media.IS_MUSIC, true);
                getContentResolver().update(song.getUri(), values, null, null);
            } catch (Exception ignored) {}

            android.media.RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    android.media.RingtoneManager.TYPE_RINGTONE,
                    song.getUri()
            );
            Toast.makeText(this, "🔔 Set as Phone Ringtone:\n" + song.getTitle(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to set ringtone: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareSong(MusicModel song) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, song.getUri());
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, song.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Listen to " + song.getTitle() + " on Dimen Spatial Music Player 🎧");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Track via"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share track: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSongDetailsDialog(MusicModel song) {
        if (song == null) return;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_song_details, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etTitle = dialogView.findViewById(R.id.etDetailsTitle);
        EditText etArtist = dialogView.findViewById(R.id.etDetailsArtist);
        EditText etAlbum = dialogView.findViewById(R.id.etDetailsAlbum);
        TextView tvDuration = dialogView.findViewById(R.id.tvDetailsDuration);
        TextView tvSize = dialogView.findViewById(R.id.tvDetailsSize);
        TextView tvPath = dialogView.findViewById(R.id.tvDetailsPath);
        Button btnClose = dialogView.findViewById(R.id.btnDetailsClose);
        Button btnSave = dialogView.findViewById(R.id.btnDetailsSave);

        etTitle.setText(song.getTitle());
        etArtist.setText(song.getArtist());
        etAlbum.setText(song.getAlbum());
        tvDuration.setText(song.getDuration());

        double sizeMb = song.getSize() / (1024.0 * 1024.0);
        if (sizeMb > 0) {
            tvSize.setText(String.format(Locale.US, "%.2f MB", sizeMb));
        } else {
            tvSize.setText("Audio Track");
        }

        String path = song.getDataPath();
        if (path == null || path.isEmpty()) {
            path = song.getUri() != null ? song.getUri().toString() : "Local Device Storage";
        }
        tvPath.setText(path);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newTitle = etTitle.getText().toString().trim();
            String newArtist = etArtist.getText().toString().trim();
            String newAlbum = etAlbum.getText().toString().trim();

            if (newTitle.isEmpty()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            song.setTitle(newTitle);
            song.setArtist(newArtist.isEmpty() ? "Unknown Artist" : newArtist);
            song.setAlbum(newAlbum.isEmpty() ? "Unknown Album" : newAlbum);

            metadataManager.saveMetadata(song.getId(), song.getTitle(), song.getArtist(), song.getAlbum());

            try {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.Audio.Media.TITLE, song.getTitle());
                cv.put(MediaStore.Audio.Media.ARTIST, song.getArtist());
                cv.put(MediaStore.Audio.Media.ALBUM, song.getAlbum());
                getContentResolver().update(song.getUri(), cv, null, null);
            } catch (Exception ignored) {}

            if (audioManager.getCurrentSong() != null && audioManager.getCurrentSong().getId() == song.getId()) {
                tvMiniTitle.setText(song.getTitle());
                tvMiniArtist.setText(song.getArtist());
            }

            musicAdapter.notifyDataSetChanged();
            Toast.makeText(this, "💾 Track info updated successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupMiniPlayer() {
        if (rlMiniContentArea != null) {
            rlMiniContentArea.setOnClickListener(v -> openPlayerScreen());
        }

        btnMiniPrev.setOnClickListener(v -> audioManager.playPrev(this));
        btnMiniPlayPause.setOnClickListener(v -> audioManager.togglePlayPause(this));
        btnMiniNext.setOnClickListener(v -> audioManager.playNext(this));

        miniSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                miniSeekHandler.removeCallbacksAndMessages(null);
                isUserSeekingMini = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                audioManager.seekTo(seekBar.getProgress());
                miniSeekHandler.removeCallbacksAndMessages(null);
                miniSeekHandler.postDelayed(() -> isUserSeekingMini = false, 400);
            }
        });
    }

    private void openPlayerScreen() {
        if (audioManager.getCurrentSong() != null) {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        }
    }

    private void checkPermissionsAndLoadSongs() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQ_CODE);
        } else {
            loadDeviceAudioFiles();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadDeviceAudioFiles();
        } else {
            Toast.makeText(this, "Storage permission is needed to scan device songs. You can also pick audio files manually.", Toast.LENGTH_LONG).show();
            updateSongCount();
        }
    }

    private void loadDeviceAudioFiles() {
        allSongsList.clear();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        try (Cursor cursor = getContentResolver().query(collection, projection, selection, null, MediaStore.Audio.Media.TITLE + " ASC")) {
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
                int sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
                int dateCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
                int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

                do {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = (albumCol != -1) ? cursor.getString(albumCol) : "Unknown Album";
                    long durationMs = cursor.getLong(durCol);
                    String displayName = cursor.getString(nameCol);
                    long size = (sizeCol != -1) ? cursor.getLong(sizeCol) : 0L;
                    long dateAdded = (dateCol != -1) ? cursor.getLong(dateCol) : 0L;
                    String dataPath = (dataCol != -1) ? cursor.getString(dataCol) : "";

                    if (title == null || title.trim().isEmpty()) {
                        title = displayName != null ? displayName : "Audio Track";
                    }

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    MusicModel song = new MusicModel(id, title, artist, album, formatTime(durationMs), durationMs, size, dateAdded, dataPath, contentUri);
                    metadataManager.applySavedMetadata(song);
                    allSongsList.add(song);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        filterSongs(etSearch.getText().toString());
        audioManager.setPlaylist(allSongsList);
    }

    private void showSleepTimerDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sleep_timer, null);
        TextView tvTimerStatus = dialogView.findViewById(R.id.tvTimerStatus);
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
            tvTimerStatus.setText("Active timer: " + (rem / 60000) + " minutes remaining");
            selectedMinutes[0] = (int) (rem / 60000);
        } else {
            tvTimerStatus.setText("Select duration to turn off audio:");
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnStartTimer.setOnClickListener(v -> {
            audioManager.startSleepTimer(selectedMinutes[0]);
            Toast.makeText(this, "Sleep timer set for " + selectedMinutes[0] + " minutes", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnTurnOff.setOnClickListener(v -> {
            audioManager.stopSleepTimer();
            Toast.makeText(this, "Sleep timer disabled", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
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
            miniPlayerCard.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(song.getTitle());
            tvMiniArtist.setText(song.getArtist());
            musicAdapter.setSelectedPosition(currentDisplayList.indexOf(song));
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @Override
    public void onProgressUpdate(int currentPos, int totalDuration) {
        if (totalDuration > 0) {
            miniSeekBar.setMax(totalDuration);
            if (!isUserSeekingMini) {
                miniSeekBar.setProgress(currentPos);
            }
        }
    }

    @Override
    public void onSpatialModeChanged(Spatial8DEngine.AudioMode mode) {
        switch (mode) {
            case OFF:
                tvMiniModeBadge.setText("STEREO");
                tvMiniModeBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                break;
            case MODE_3D:
                tvMiniModeBadge.setText("3D SURROUND");
                tvMiniModeBadge.setTextColor(ContextCompat.getColor(this, R.color.secondary));
                break;
            case MODE_8D:
                tvMiniModeBadge.setText("8D ORBIT");
                tvMiniModeBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));
                break;
            case MODE_16D:
                tvMiniModeBadge.setText("16D ULTRA");
                tvMiniModeBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_pink));
                break;
        }
    }

    @Override
    public void onLoopModeChanged(AudioPlaybackManager.LoopMode mode) {}

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingRingtoneSong != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.System.canWrite(this)) {
                MusicModel song = pendingRingtoneSong;
                pendingRingtoneSong = null;
                applyRingtone(song);
            }
        }
        audioManager.addCallback(this);
        MusicModel currentSong = audioManager.getCurrentSong();
        if (currentSong != null) {
            miniPlayerCard.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(currentSong.getTitle());
            tvMiniArtist.setText(currentSong.getArtist());
            int dur = audioManager.getDuration();
            int pos = audioManager.getCurrentPosition();
            if (dur > 0) {
                miniSeekBar.setMax(dur);
                miniSeekBar.setProgress(pos);
            }
            btnMiniPlayPause.setImageResource(audioManager.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            onSpatialModeChanged(audioManager.getSpatialEngine().getMode());
            musicAdapter.setSelectedPosition(currentDisplayList.indexOf(currentSong));
        }
        favoritesManager.reloadFavorites();
        if (musicAdapter != null) {
            musicAdapter.notifyDataSetChanged();
        }
        if (isShowingFavoritesOnly) {
            filterSongs(etSearch.getText().toString());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        audioManager.removeCallback(this);
    }
}
