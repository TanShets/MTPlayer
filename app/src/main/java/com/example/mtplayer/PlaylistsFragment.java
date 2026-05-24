package com.example.mtplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.mtplayer.adapters.PlaylistAdapter;
import com.example.mtplayer.database.AppDatabase;
import com.example.mtplayer.database.Playlist;
import com.example.mtplayer.databinding.FragmentPlaylistsBinding;
import java.util.List;

public class PlaylistsFragment extends Fragment {
    private FragmentPlaylistsBinding binding;
    private PlaylistAdapter adapter;
    private AppDatabase database;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlaylistsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = AppDatabase.getDatabase(requireContext());
        setupRecyclerView();

        binding.btnCreatePlaylist.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PlaylistEditorActivity.class);
            startActivity(intent);
        });

        database.playlistDao().getAllPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            adapter.setPlaylists(playlists);
            binding.tvEmptyPlaylists.setVisibility(playlists.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void setupRecyclerView() {
        adapter = new PlaylistAdapter(new PlaylistAdapter.OnPlaylistInteractionListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                // TODO: Step 6 - Play the playlist
            }

            @Override
            public void onPlaylistEdit(Playlist playlist) {
                Intent intent = new Intent(requireContext(), PlaylistEditorActivity.class);
                intent.putExtra(PlaylistEditorActivity.EXTRA_PLAYLIST_ID, playlist.getId());
                intent.putExtra(PlaylistEditorActivity.EXTRA_PLAYLIST_NAME, playlist.getName());
                startActivity(intent);
            }

            @Override
            public void onPlaylistDelete(Playlist playlist) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.delete_playlist_title)
                        .setMessage(getString(R.string.delete_playlist_message, playlist.getName()))
                        .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                            new Thread(() -> {
                                database.playlistDao().deletePlaylist(playlist);
                            }).start();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        binding.rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPlaylists.setAdapter(adapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
