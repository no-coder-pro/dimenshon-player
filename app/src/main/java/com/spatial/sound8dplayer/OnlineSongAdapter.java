package com.spatial.sound8dplayer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class OnlineSongAdapter extends RecyclerView.Adapter<OnlineSongAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(OnlineSongModel song);
        void onDownloadClick(OnlineSongModel song);
        void onPreviewPlayClick(OnlineSongModel song);
    }

    private final List<OnlineSongModel> songList;
    private final OnItemClickListener listener;
    private final ImageLoader imageLoader;

    private String activePreviewSongId = null;
    private boolean isPreviewPlaying = false;
    private boolean isPreviewBuffering = false;

    public OnlineSongAdapter(List<OnlineSongModel> songList, OnItemClickListener listener) {
        this.songList = songList;
        this.listener = listener;
        this.imageLoader = ImageLoader.getInstance();
    }

    public void setPreviewState(String songId, boolean isPlaying, boolean isBuffering) {
        String prevSongId = this.activePreviewSongId;
        this.activePreviewSongId = songId;
        this.isPreviewPlaying = isPlaying;
        this.isPreviewBuffering = isBuffering;

        if (prevSongId != null) {
            for (int i = 0; i < songList.size(); i++) {
                if (prevSongId.equals(songList.get(i).getId())) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
        if (songId != null && !songId.equals(prevSongId)) {
            for (int i = 0; i < songList.size(); i++) {
                if (songId.equals(songList.get(i).getId())) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    public String getActivePreviewSongId() {
        return activePreviewSongId;
    }

    public boolean isPreviewPlaying() {
        return isPreviewPlaying;
    }

    public void stopPreview() {
        setPreviewState(null, false, false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OnlineSongModel song = songList.get(position);
        holder.tvTitle.setText(song.getTitle());

        String duration = song.getDurationStr();
        String subtitle = song.getArtist();
        if (!duration.isEmpty()) {
            subtitle += " • " + duration;
        }
        holder.tvArtist.setText(subtitle);

        if (song.isSpotify()) {
            holder.tvSourceBadge.setText("Spotify");
            holder.tvSourceBadge.setTextColor(Color.parseColor("#1DB954"));
        } else {
            holder.tvSourceBadge.setText("YouTube");
            holder.tvSourceBadge.setTextColor(Color.parseColor("#FF3366"));
        }

        imageLoader.displayImage(song.getThumbnailUrl(), holder.ivThumbnail, R.drawable.ic_music_note);

        boolean isCurrentItem = activePreviewSongId != null && activePreviewSongId.equals(song.getId());

        if (isCurrentItem) {
            if (isPreviewBuffering) {
                holder.pbPreviewLoading.setVisibility(View.VISIBLE);
                holder.ivPlayPreview.setVisibility(View.GONE);
            } else {
                holder.pbPreviewLoading.setVisibility(View.GONE);
                holder.ivPlayPreview.setVisibility(View.VISIBLE);
                holder.ivPlayPreview.setImageResource(isPreviewPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
            }
        } else {
            holder.pbPreviewLoading.setVisibility(View.GONE);
            holder.ivPlayPreview.setVisibility(View.VISIBLE);
            holder.ivPlayPreview.setImageResource(R.drawable.ic_play);
        }

        View.OnClickListener previewClickListener = v -> {
            if (listener != null) listener.onPreviewPlayClick(song);
        };
        holder.cardThumb.setOnClickListener(previewClickListener);
        holder.llSongInfo.setOnClickListener(previewClickListener);

        holder.btnDownload.setOnClickListener(v -> {
            if (listener != null) listener.onDownloadClick(song);
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View cardThumb;
        ImageView ivThumbnail;
        ImageView ivPlayPreview;
        ProgressBar pbPreviewLoading;
        LinearLayout llSongInfo;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvSourceBadge;
        LinearLayout btnDownload;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardThumb = itemView.findViewById(R.id.cardThumb);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivPlayPreview = itemView.findViewById(R.id.ivPlayPreview);
            pbPreviewLoading = itemView.findViewById(R.id.pbPreviewLoading);
            llSongInfo = itemView.findViewById(R.id.llSongInfo);
            tvTitle = itemView.findViewById(R.id.tvOnlineTitle);
            tvArtist = itemView.findViewById(R.id.tvOnlineArtist);
            tvSourceBadge = itemView.findViewById(R.id.tvSourceBadge);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
    }
}
