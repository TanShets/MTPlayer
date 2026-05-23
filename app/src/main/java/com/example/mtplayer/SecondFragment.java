package com.example.mtplayer;

import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mtplayer.databinding.FragmentSecondBinding;
import com.example.mtplayer.viewmodels.SongViewModel;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private SongViewModel viewModel;
    private boolean isUserSeeking = false;
    private Visualizer visualizer;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SongViewModel.class);

        setupUI();
        observeViewModel();
    }

    private void setupVisualizer(int audioSessionId) {
        if (visualizer != null) {
            visualizer.release();
        }

        try {
            // Using 0 for global mix if session ID is not provided or invalid
            // Note: In a real app, getting the specific session ID is better.
            visualizer = new Visualizer(audioSessionId);
            visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    if (binding != null) {
                        binding.visualizerView.updateVisualizer(waveform);
                    }
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, false);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            Log.e("SecondFragment", "Error initializing visualizer", e);
        }
    }

    private void setupUI() {
        binding.btnPlayPause.setOnClickListener(v -> viewModel.togglePlayPause());
        binding.btnNext.setOnClickListener(v -> viewModel.playNext());
        binding.btnPrev.setOnClickListener(v -> viewModel.playPrevious());
        binding.btnRewind.setOnClickListener(v -> viewModel.rewind());
        binding.btnForward.setOnClickListener(v -> viewModel.forward());

        binding.sbTempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float tempo = 0.5f + (progress / 100.0f);
                binding.tvTempoLabel.setText(String.format(Locale.getDefault(), "Tempo: %.2fx", tempo));
                if (fromUser) {
                    viewModel.setSpeed(tempo);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.sbPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = 0.5f + (progress / 100.0f);
                binding.tvPitchLabel.setText(String.format(Locale.getDefault(), "Pitch: %.2fx", pitch));
                if (fromUser) {
                    viewModel.setPitch(pitch);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.sbPlayerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                viewModel.seekTo(seekBar.getProgress());
            }
        });
    }

    private void observeViewModel() {
        viewModel.getSelectedSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null) {
                binding.tvPlayerTitle.setText(song.getTitle());
                binding.tvPlayerArtist.setText(song.getArtist());
            }
        });

        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (isPlaying != null) {
                binding.btnPlayPause.setImageResource(isPlaying ? 
                        android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }
        });

        viewModel.getDuration().observe(getViewLifecycleOwner(), duration -> {
            if (duration != null && duration > 0) {
                binding.sbPlayerProgress.setMax(duration.intValue());
                binding.tvTotalTime.setText(formatTime(duration));
            }
        });

        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), position -> {
            if (position != null && !isUserSeeking) {
                binding.sbPlayerProgress.setProgress(position.intValue());
                binding.tvCurrentTime.setText(formatTime(position));
            }
        });

        viewModel.getAudioSessionId().observe(getViewLifecycleOwner(), sessionId -> {
            if (sessionId != null && sessionId != 0) {
                setupVisualizer(sessionId);
            }
        });

        viewModel.getSpeed().observe(getViewLifecycleOwner(), speed -> {
            if (speed != null) {
                int progress = (int) ((speed - 0.5f) * 100);
                binding.sbTempo.setProgress(progress);
                binding.tvTempoLabel.setText(String.format(Locale.getDefault(), "Tempo: %.2fx", speed));
            }
        });

        viewModel.getPitch().observe(getViewLifecycleOwner(), pitch -> {
            if (pitch != null) {
                int progress = (int) ((pitch - 0.5f) * 100);
                binding.sbPitch.setProgress(progress);
                binding.tvPitchLabel.setText(String.format(Locale.getDefault(), "Pitch: %.2fx", pitch));
            }
        });
    }

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - 
                      TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
        binding = null;
    }
}
