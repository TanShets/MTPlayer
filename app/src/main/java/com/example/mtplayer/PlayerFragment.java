package com.example.mtplayer;

import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.Player;

import com.bumptech.glide.Glide;
import com.example.mtplayer.databinding.FragmentPlayerBinding;
import com.example.mtplayer.viewmodels.SongViewModel;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayerFragment extends Fragment {

    private FragmentPlayerBinding binding;
    private SongViewModel viewModel;
    private boolean isUserSeeking = false;
    private Visualizer visualizer;
    private int currentSessionId = -1;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPlayerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SongViewModel.class);

        setupUI();
        observeViewModel();
    }

    private void setupVisualizer(int audioSessionId) {
        if (visualizer != null && currentSessionId == audioSessionId) {
            return;
        }

        if (visualizer != null) {
            visualizer.release();
        }

        currentSessionId = audioSessionId;
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
            
            Boolean isPlaying = viewModel.getIsPlaying().getValue();
            visualizer.setEnabled(isPlaying != null && isPlaying);
        } catch (Exception e) {
            Log.e("PlayerFragment", "Error initializing visualizer", e);
        }
    }

    private void setupUI() {
        binding.btnPlayPause.setOnClickListener(v -> viewModel.togglePlayPause());
        binding.btnNext.setOnClickListener(v -> viewModel.playNext());
        binding.btnPrev.setOnClickListener(v -> viewModel.playPrevious());
        binding.btnRewind.setOnClickListener(v -> viewModel.rewind());
        binding.btnForward.setOnClickListener(v -> viewModel.forward());
        binding.btnShuffleLinear.setOnClickListener(v -> {
            viewModel.toggleShuffleMode();
            Boolean enabled = viewModel.getShuffleModeEnabled().getValue();
            Toast.makeText(getContext(), Boolean.TRUE.equals(enabled) ? "Shuffle: On" : "Shuffle: Off", Toast.LENGTH_SHORT).show();
        });
        binding.btnRepeatMode.setOnClickListener(v -> {
            viewModel.cycleRepeatMode();
            showRepeatModeToast();
        });

        binding.visualizerView.setOnSeekBarChangeListener((progress, fromUser) -> {
            if (fromUser && viewModel.getDuration().getValue() != null) {
                long newPos = (long) (progress * viewModel.getDuration().getValue());
                viewModel.seekTo(newPos);
            }
        });

        binding.sbTempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float tempo = 0.5f + (progress / 100.0f);
                binding.tvTempoLabel.setText(String.format(Locale.getDefault(), "Tempo: %.2fx", tempo));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float tempo = 0.5f + (seekBar.getProgress() / 100.0f);
                viewModel.setSpeed(tempo);
            }
        });

        binding.sbPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int semitones = progress - 12;
                float multiplier = (float) Math.pow(2.0, semitones / 12.0);
                binding.tvPitchLabel.setText(String.format(Locale.getDefault(), "Pitch: %+d semitones (%.2fx)", semitones, multiplier));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int semitones = seekBar.getProgress() - 12;
                float multiplier = (float) Math.pow(2.0, semitones / 12.0);
                viewModel.setPitch(multiplier);
            }
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
                Glide.with(this)
                        .load(song.getAlbumArtUri())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(binding.ivPlayerAlbumArt);
            }
        });

        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (isPlaying != null) {
                binding.btnPlayPause.setImageResource(isPlaying ? 
                        android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                if (visualizer != null) {
                    try {
                        visualizer.setEnabled(isPlaying);
                    } catch (IllegalStateException e) {
                        Log.e("PlayerFragment", "Error toggling visualizer", e);
                    }
                }
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
                
                Long duration = viewModel.getDuration().getValue();
                if (duration != null && duration > 0) {
                    binding.visualizerView.setProgress(position.floatValue() / duration);
                }
            }
        });

        viewModel.getAudioSessionId().observe(getViewLifecycleOwner(), sessionId -> {
            if (sessionId != null) {
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
                // Multiplier to semitones: s = 12 * log2(m)
                int semitones = (int) Math.round(12.0 * Math.log(pitch) / Math.log(2.0));
                binding.sbPitch.setProgress(semitones + 12);
                binding.tvPitchLabel.setText(String.format(Locale.getDefault(), "Pitch: %+d semitones (%.2fx)", semitones, pitch));
            }
        });

        viewModel.getShuffleModeEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (enabled != null) {
                binding.btnShuffleLinear.setAlpha(enabled ? 1.0f : 0.3f);
            }
        });

        viewModel.getRepeatMode().observe(getViewLifecycleOwner(), mode -> updateRepeatUI());

        viewModel.getStopAfterCurrent().observe(getViewLifecycleOwner(), stop -> updateRepeatUI());

        viewModel.getPlaybackSource().observe(getViewLifecycleOwner(), source -> {
            if (source != null && getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar();
                if (actionBar != null) {
                    if ("All Songs".equals(source)) {
                        actionBar.setTitle("All Songs");
                    } else {
                        actionBar.setTitle("Playing from: " + source);
                    }
                }
            }
        });
    }

    private void showRepeatModeToast() {
        Integer mode = viewModel.getRepeatMode().getValue();
        Boolean stop = viewModel.getStopAfterCurrent().getValue();
        if (mode == null || stop == null) return;

        String message;
        if (stop) message = "Behavior: Stop after this song";
        else if (mode == Player.REPEAT_MODE_ONE) message = "Behavior: Repeat Current";
        else message = "Behavior: Next Song";
        
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateRepeatUI() {
        Integer mode = viewModel.getRepeatMode().getValue();
        Boolean stop = viewModel.getStopAfterCurrent().getValue();
        
        if (mode == null || stop == null) return;

        if (stop) {
            binding.btnRepeatMode.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            binding.btnRepeatMode.setAlpha(1.0f);
        } else if (mode == Player.REPEAT_MODE_ONE) {
            binding.btnRepeatMode.setImageResource(android.R.drawable.stat_notify_sync);
            binding.btnRepeatMode.setAlpha(1.0f);
        } else {
            binding.btnRepeatMode.setImageResource(android.R.drawable.stat_notify_sync);
            binding.btnRepeatMode.setAlpha(0.3f);
        }
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
