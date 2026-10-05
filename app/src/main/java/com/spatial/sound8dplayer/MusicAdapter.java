package com.spatial.sound8dplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(MusicModel song, int position);
        void onFavoriteClick(MusicModel song, int position);
        void onMenuClick(MusicModel song, int position, View anchorView);
    }

    private final List<MusicModel> songList;
    private final FavoritesManager favoritesManager;
    private final OnSongClickListener listener;
    private int selectedPosition = -1;

    private int colorSecondary;
    private int colorTextPrimary;
    private boolean colorsInitialized = false;

    public MusicAdapter(List<MusicModel> songList, FavoritesManager favManager, OnSongClickListener listener) {
        this.songList = songList;
        this.favoritesManager = favManager;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < songList.size()) {
            return songList.get(position).getId();
        }
        return RecyclerView.NO_ID;
    }

    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != -1) notifyItemChanged(previous);
        if (selectedPosition != -1) notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (!colorsInitialized) {
            Context ctx = parent.getContext();
            colorSecondary = ContextCompat.getColor(ctx, R.color.secondary);
            colorTextPrimary = ContextCompat.getColor(ctx, R.color.text_primary);
            colorsInitialized = true;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new MusicViewHolder(view, listener, songList, favoritesManager);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicModel song = songList.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvDuration.setText(song.getDuration());

        if (position == selectedPosition) {
            holder.tvTitle.setTextColor(colorSecondary);
        } else {
            holder.tvTitle.setTextColor(colorTextPrimary);
        }

        boolean isFav = favoritesManager.isFavorite(song.getId());
        holder.ivFavorite.setImageResource(isFav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvArtist, tvDuration;
        final ImageButton ivFavorite, btnSongMenu;

        public MusicViewHolder(@NonNull View itemView, OnSongClickListener listener, List<MusicModel> songList, FavoritesManager favManager) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvSongArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            btnSongMenu = itemView.findViewById(R.id.btnSongMenu);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < songList.size() && listener != null) {
                    listener.onSongClick(songList.get(pos), pos);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < songList.size() && listener != null) {
                    listener.onMenuClick(songList.get(pos), pos, v);
                }
                return true;
            });

            ivFavorite.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < songList.size()) {
                    MusicModel song = songList.get(pos);
                    boolean nowFav = favManager.toggleFavorite(song.getId());
                    ivFavorite.setImageResource(nowFav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                    if (listener != null) {
                        listener.onFavoriteClick(song, pos);
                    }
                }
            });

            btnSongMenu.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < songList.size() && listener != null) {
                    listener.onMenuClick(songList.get(pos), pos, v);
                }
            });
        }
    }
}
