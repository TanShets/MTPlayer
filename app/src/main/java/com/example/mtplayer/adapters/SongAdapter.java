package com.example.mtplayer.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mtplayer.databinding.ItemSongBinding;
import com.example.mtplayer.models.Song;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> implements Filterable {

    private List<Song> songs = new ArrayList<>();
    private List<Song> songsFull = new ArrayList<>();
    private final OnSongClickListener listener;
    private OnFilterResultsListener filterListener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
        void onMoreClick(Song song, android.view.View view);
    }

    public interface OnFilterResultsListener {
        void onFilterResults(int count);
    }

    public SongAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setOnFilterResultsListener(OnFilterResultsListener filterListener) {
        this.filterListener = filterListener;
    }

    public void setSongs(List<Song> songs) {
        this.songs = new ArrayList<>(songs);
        this.songsFull = new ArrayList<>(songs);
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return songs;
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

    @Override
    public Filter getFilter() {
        return songFilter;
    }

    private final Filter songFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Song> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(songsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Song item : songsFull) {
                    if (item.getTitle().toLowerCase().contains(filterPattern) ||
                        item.getArtist().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            songs.clear();
            if (results.values != null) {
                songs.addAll((List<Song>) results.values);
            }
            notifyDataSetChanged();
            if (filterListener != null) {
                filterListener.onFilterResults(getItemCount());
            }
        }
    };

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
            
            Glide.with(binding.ivAlbumArt.getContext())
                    .load(song.getAlbumArtUri())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivAlbumArt);
            
            binding.getRoot().setOnClickListener(v -> listener.onSongClick(song));
            binding.btnMore.setOnClickListener(v -> listener.onMoreClick(song, v));
        }

        private String formatDuration(long duration) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - 
                          TimeUnit.MINUTES.toSeconds(minutes);
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
}
