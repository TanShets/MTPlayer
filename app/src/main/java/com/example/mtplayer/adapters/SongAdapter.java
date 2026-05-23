package com.example.mtplayer.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mtplayer.databinding.ItemSongBinding;
import com.example.mtplayer.models.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private final OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SongAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSongBinding binding = ItemSongBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SongViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final ItemSongBinding binding;

        SongViewHolder(ItemSongBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Song song, OnSongClickListener listener) {
            binding.tvTitle.setText(song.getTitle());
            binding.tvArtist.setText(song.getArtist());
            binding.tvDuration.setText(formatDuration(song.getDuration()));
            
            // Note: Album art loading will be handled in a later step if needed,
            // or we can use a placeholder for now.
            
            binding.getRoot().setOnClickListener(v -> listener.onSongClick(song));
        }

        private String formatDuration(long duration) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - 
                          TimeUnit.MINUTES.toSeconds(minutes);
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
}
