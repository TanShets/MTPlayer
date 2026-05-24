package com.example.mtplayer.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mtplayer.databinding.ItemSelectableSongBinding;
import com.example.mtplayer.models.Song;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectableSongAdapter extends RecyclerView.Adapter<SelectableSongAdapter.ViewHolder> {
    private List<Song> songs = new ArrayList<>();
    private final Set<Long> selectedSongIds = new HashSet<>();

    public void setSongs(List<Song> songs, List<Long> initialSelectedIds) {
        this.songs = songs;
        if (initialSelectedIds != null) {
            selectedSongIds.addAll(initialSelectedIds);
        }
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedSongIds() {
        return selectedSongIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelectableSongBinding binding = ItemSelectableSongBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSelectableSongBinding binding;

        ViewHolder(ItemSelectableSongBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Song song) {
            binding.tvTitle.setText(song.getTitle());
            binding.tvArtist.setText(song.getArtist());
            binding.cbSelect.setChecked(selectedSongIds.contains(song.getId()));

            Glide.with(binding.ivAlbumArt.getContext())
                    .load(song.getAlbumArtUri())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivAlbumArt);

            binding.getRoot().setOnClickListener(v -> {
                long id = song.getId();
                if (selectedSongIds.contains(id)) {
                    selectedSongIds.remove(id);
                } else {
                    selectedSongIds.add(id);
                }
                notifyItemChanged(getAdapterPosition());
            });
        }
    }
}
