package com.example.mtplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mtplayer.adapters.SongAdapter;
import com.example.mtplayer.databinding.FragmentFirstBinding;
import com.example.mtplayer.models.Song;
import com.example.mtplayer.viewmodels.SongViewModel;

import java.util.List;

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
                
                // Update scroll handle once songs are loaded
                binding.rvSongs.post(this::updateScrollHandlePosition);
            }
        });

        viewModel.getSearchQuery().observe(getViewLifecycleOwner(), query -> {
            adapter.getFilter().filter(query);
        });

        setupSideIndex();
        setupRecyclerViewScrollListener();
    }

    private void setupRecyclerViewScrollListener() {
        binding.rvSongs.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateScrollHandlePosition();
            }
        });
    }

    private void updateScrollHandlePosition() {
        if (binding == null || binding.sideIndex.getHeight() == 0) return;

        float offset = binding.rvSongs.computeVerticalScrollOffset();
        float extent = binding.rvSongs.computeVerticalScrollExtent();
        float range = binding.rvSongs.computeVerticalScrollRange();

        if (range <= extent) {
            binding.scrollHandle.setVisibility(View.INVISIBLE);
            return;
        }

        binding.scrollHandle.setVisibility(View.VISIBLE);
        float percentage = offset / (range - extent);
        float handleY = percentage * (binding.sideIndex.getHeight() - binding.scrollHandle.getHeight());
        binding.scrollHandle.setY(handleY);

        // Update bubble text to sync with the top visible song
        updateBubbleText();
    }

    private void updateBubbleText() {
        if (binding == null || binding.tvIndexBubble.getVisibility() != View.VISIBLE) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager) binding.rvSongs.getLayoutManager();
        if (layoutManager != null) {
            int firstVisible = layoutManager.findFirstVisibleItemPosition();
            if (firstVisible != RecyclerView.NO_POSITION) {
                List<Song> songs = adapter.getSongs();
                if (songs != null && firstVisible < songs.size()) {
                    String title = songs.get(firstVisible).getTitle();
                    if (title != null && !title.isEmpty()) {
                        char firstChar = title.toUpperCase().charAt(0);
                        String letter = Character.isLetter(firstChar) ? String.valueOf(firstChar) : "#";
                        binding.tvIndexBubble.setText(letter);
                    }
                }
            }
        }
    }

    private void setupSideIndex() {
        String[] indexList = {"#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

        binding.sideIndex.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.performClick();
            }
            float y = event.getY();
            float height = (float) v.getHeight();
            if (height > 0) {
                y = Math.max(0, Math.min(y, height));
                
                // Calculate letter based on touch position to jump the list
                int index = (int) (y / (height / indexList.length));
                index = Math.max(0, Math.min(index, indexList.length - 1));
                String targetLetter = indexList[index];
                
                scrollToLetter(targetLetter);
                
                // Show bubble and position it
                showBubbleAt(y + v.getTop());
                // The actual letter in the bubble will be updated by updateBubbleText() 
                // via the scroll listener triggered by scrollToLetter()
            }
            
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                binding.tvIndexBubble.setVisibility(View.GONE);
            }
            return true;
        });
    }

    private void scrollToLetter(String letter) {
        List<Song> songs = adapter.getSongs();
        if (songs == null || songs.isEmpty()) return;

        for (int i = 0; i < songs.size(); i++) {
            String title = songs.get(i).getTitle().toUpperCase();
            if (title.isEmpty()) continue;

            if (letter.equals("#")) {
                if (!Character.isLetter(title.charAt(0))) {
                    if (binding.rvSongs.getLayoutManager() instanceof LinearLayoutManager) {
                        ((LinearLayoutManager) binding.rvSongs.getLayoutManager()).scrollToPositionWithOffset(i, 0);
                    }
                    break;
                }
            } else if (title.startsWith(letter)) {
                if (binding.rvSongs.getLayoutManager() instanceof LinearLayoutManager) {
                    ((LinearLayoutManager) binding.rvSongs.getLayoutManager()).scrollToPositionWithOffset(i, 0);
                }
                break;
            }
        }
    }

    private void showBubbleAt(float y) {
        binding.tvIndexBubble.setVisibility(View.VISIBLE);
        updateBubbleText();
        
        float bubbleHeight = binding.tvIndexBubble.getHeight();
        if (bubbleHeight == 0) bubbleHeight = 64 * getResources().getDisplayMetrics().density;
        
        float targetY = y - (bubbleHeight / 2);
        targetY = Math.max(0, Math.min(targetY, binding.getRoot().getHeight() - bubbleHeight));
        
        binding.tvIndexBubble.setY(targetY);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
