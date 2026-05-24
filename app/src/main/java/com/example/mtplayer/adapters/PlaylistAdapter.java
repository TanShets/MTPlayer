package com.example.mtplayer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mtplayer.R;
import com.example.mtplayer.database.Playlist;
import com.example.mtplayer.databinding.ItemPlaylistBinding;
import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private List<Playlist> playlists = new ArrayList<>();
    private final OnPlaylistInteractionListener listener;

    public interface OnPlaylistInteractionListener {
        void onPlaylistClick(Playlist playlist);
        void onPlaylistView(Playlist playlist);
        void onPlaylistEdit(Playlist playlist);
        void onPlaylistDelete(Playlist playlist);
    }

    public PlaylistAdapter(OnPlaylistInteractionListener listener) {
        this.listener = listener;
    }

    public void setPlaylists(List<Playlist> playlists) {
        this.playlists = playlists;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlaylistBinding binding = ItemPlaylistBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPlaylistBinding binding;

        ViewHolder(ItemPlaylistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Playlist playlist) {
            binding.tvPlaylistName.setText(playlist.getName());
            binding.getRoot().setOnClickListener(v -> listener.onPlaylistClick(playlist));
            
            binding.btnPlaylistMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.menu_playlist_item, popup.getMenu());
                
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.action_view) {
                        listener.onPlaylistView(playlist);
                        return true;
                    } else if (itemId == R.id.action_edit) {
                        listener.onPlaylistEdit(playlist);
                        return true;
                    } else if (itemId == R.id.action_delete) {
                        listener.onPlaylistDelete(playlist);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
