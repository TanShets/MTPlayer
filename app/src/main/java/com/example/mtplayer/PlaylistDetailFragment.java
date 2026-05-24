package com.example.mtplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.mtplayer.adapters.SongAdapter;
import com.example.mtplayer.database.AppDatabase;
import com.example.mtplayer.databinding.FragmentPlaylistDetailBinding;
import com.example.mtplayer.models.Song;
import com.example.mtplayer.viewmodels.SongViewModel;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment {
    public static final String ARG_PLAYLIST_ID = "playlist_id";
    public static final String ARG_PLAYLIST_NAME = "playlist_name";

    private FragmentPlaylistDetailBinding binding;
    private SongViewModel viewModel;
    private SongAdapter adapter;
    private AppDatabase database;
    private int playlistId;
    private String playlistName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getInt(ARG_PLAYLIST_ID);
            playlistName = getArguments().getString(ARG_PLAYLIST_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaylistDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SongViewModel.class);
        database = AppDatabase.getDatabase(requireContext());

        setupRecyclerView();
        loadPlaylistSongs();
    }

    private void setupRecyclerView() {
        adapter = new SongAdapter(new SongAdapter.OnSongClickListener() {
            @Override
            public void onSongClick(Song song) {
                List<Song> playlistSongs = adapter.getSongs();
                int position = playlistSongs.indexOf(song);
                viewModel.playQueue(playlistSongs, position, playlistName);
                NavHostFragment.findNavController(PlaylistDetailFragment.this)
                        .navigate(R.id.PlayerFragment);
            }

            @Override
            public void onMoreClick(Song song, View view) {
                // For now, keep it simple or implement song menu if needed
            }
        });

        binding.rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPlaylistSongs.setAdapter(adapter);
    }

    private void loadPlaylistSongs() {
        LiveData<List<Long>> songIdsLiveData = database.playlistDao().getSongIdsForPlaylist(playlistId);
        LiveData<List<Song>> allSongsLiveData = viewModel.getSongs();

        MediatorLiveData<List<Song>> playlistSongsMediator = new MediatorLiveData<>();
        playlistSongsMediator.addSource(songIdsLiveData, songIds -> {
            playlistSongsMediator.setValue(combine(songIds, allSongsLiveData.getValue()));
        });
        playlistSongsMediator.addSource(allSongsLiveData, allSongs -> {
            playlistSongsMediator.setValue(combine(songIdsLiveData.getValue(), allSongs));
        });

        playlistSongsMediator.observe(getViewLifecycleOwner(), songs -> {
            adapter.setSongs(songs);
            binding.tvEmptyPlaylist.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private List<Song> combine(List<Long> songIds, List<Song> allSongs) {
        List<Song> playlistSongs = new ArrayList<>();
        if (songIds == null || allSongs == null) return playlistSongs;
        for (Long id : songIds) {
            for (Song s : allSongs) {
                if (s.getId() == id) {
                    playlistSongs.add(s);
                    break;
                }
            }
        }
        return playlistSongs;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
