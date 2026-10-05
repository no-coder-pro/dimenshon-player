package com.spatial.sound8dplayer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    public interface OnQueueSongClickListener {
        void onQueueSongClick(MusicModel song, int position);
    }

    private final List<MusicModel> playlist;
    private final OnQueueSongClickListener clickListener;
    private int currentPlayingIndex = -1;

    public QueueAdapter(List<MusicModel> playlist, OnQueueSongClickListener clickListener) {
        this.playlist = playlist;
        this.clickListener = clickListener;
    }

    public void setCurrentPlayingIndex(int index) {
        int oldIndex = currentPlayingIndex;
        currentPlayingIndex = index;
        if (oldIndex >= 0 && oldIndex < playlist.size()) notifyItemChanged(oldIndex);
        if (currentPlayingIndex >= 0 && currentPlayingIndex < playlist.size()) notifyItemChanged(currentPlayingIndex);
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_song, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        MusicModel song = playlist.get(position);
        holder.tvQueueSongTitle.setText(song.getTitle());
        holder.tvQueueSongArtist.setText(song.getArtist());
        holder.tvQueueSongDuration.setText(song.getDuration());

        boolean isCurrent = (position == currentPlayingIndex);

        if (isCurrent) {
            holder.tvQueueIndex.setVisibility(View.GONE);
            holder.ivQueuePlayingIcon.setVisibility(View.VISIBLE);
            holder.tvQueueSongTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary));
            holder.flQueueIndex.setBackgroundResource(R.drawable.bg_circle_button);
            holder.itemView.setBackgroundColor(Color.parseColor("#1C233D"));
        } else {
            holder.tvQueueIndex.setVisibility(View.VISIBLE);
            holder.tvQueueIndex.setText(String.valueOf(position + 1));
            holder.ivQueuePlayingIcon.setVisibility(View.GONE);
            holder.tvQueueSongTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.flQueueIndex.setBackgroundResource(R.drawable.bg_card_rounded);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onQueueSongClick(song, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlist != null ? playlist.size() : 0;
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flQueueIndex;
        TextView tvQueueIndex;
        ImageView ivQueuePlayingIcon;
        TextView tvQueueSongTitle;
        TextView tvQueueSongArtist;
        TextView tvQueueSongDuration;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            flQueueIndex = itemView.findViewById(R.id.flQueueIndex);
            tvQueueIndex = itemView.findViewById(R.id.tvQueueIndex);
            ivQueuePlayingIcon = itemView.findViewById(R.id.ivQueuePlayingIcon);
            tvQueueSongTitle = itemView.findViewById(R.id.tvQueueSongTitle);
            tvQueueSongArtist = itemView.findViewById(R.id.tvQueueSongArtist);
            tvQueueSongDuration = itemView.findViewById(R.id.tvQueueSongDuration);
        }
    }
}
