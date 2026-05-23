package com.example.mtplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.mtplayer.adapters.SongAdapter;
import com.example.mtplayer.databinding.FragmentFirstBinding;
import com.example.mtplayer.viewmodels.SongViewModel;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private SongViewModel viewModel;
    private SongAdapter adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SongViewModel.class);
        adapter = new SongAdapter(song -> {
            viewModel.selectSong(song);
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment);
        });

        binding.rvSongs.setAdapter(adapter);

        adapter.setOnFilterResultsListener(count -> 
            binding.tvEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE)
        );

        viewModel.getSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                adapter.setSongs(songs);
                binding.tvEmptyState.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);

                String query = viewModel.getSearchQuery().getValue();
                if (query != null && !query.isEmpty()) {
                    adapter.getFilter().filter(query);
                }
            }
        });

        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            adapter.getFilter().filter(query);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}