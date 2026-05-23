package com.example.mtplayer.viewmodels;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.mtplayer.managers.PlaylistManager;
import com.example.mtplayer.models.Song;
import com.example.mtplayer.services.PlayerService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class SongViewModel extends ViewModel {
    private final MutableLiveData<List<Song>> songs = new MutableLiveData<>();
    private final MutableLiveData<Song> selectedSong = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> audioSessionId = new MutableLiveData<>(0);
    private final MutableLiveData<Float> speed = new MutableLiveData<>(1.0f);
    private final MutableLiveData<Float> pitch = new MutableLiveData<>(1.0f);
    
    private final PlaylistManager playlistManager = new PlaylistManager();
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                currentPosition.setValue(mediaController.getCurrentPosition());
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    public void initController(Context context) {
        if (mediaController != null) return;

        SessionToken sessionToken = new SessionToken(context, new ComponentName(context, PlayerService.class));
        controllerFuture = new MediaController.Builder(context, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                
                if (mediaController.getConnectedToken() != null) {
                    Bundle extras = mediaController.getConnectedToken().getExtras();
                    if (extras.containsKey("audio_session_id")) {
                        audioSessionId.setValue(extras.getInt("audio_session_id"));
                    }
                }

                // Apply initial playback parameters
                mediaController.setPlaybackParameters(new PlaybackParameters(speed.getValue(), pitch.getValue()));

                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean playing) {
                        isPlaying.setValue(playing);
                        if (playing) {
                            progressHandler.post(updateProgressAction);
                        } else {
                            progressHandler.removeCallbacks(updateProgressAction);
                        }
                    }

                    @Override
                    public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                        if (mediaItem != null) {
                            duration.setValue(mediaController.getDuration());
                            // Sync current song from playlist manager if needed
                        }
                    }
                    
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == Player.STATE_READY) {
                            duration.setValue(mediaController.getDuration());
                        } else if (playbackState == Player.STATE_ENDED) {
                            playNext();
                        }
                    }
                });
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    public void setSongs(List<Song> songList) {
        songs.setValue(songList);
        playlistManager.setPlaylist(songList);
    }

    public LiveData<List<Song>> getSongs() {
        return songs;
    }

    public void selectSong(Song song) {
        selectedSong.setValue(song);
        playlistManager.setCurrentSong(song);
        
        if (mediaController != null) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(song.getUri())
                    .setMediaId(String.valueOf(song.getId()))
                    .build();
            mediaController.setMediaItem(mediaItem);
            mediaController.prepare();
            mediaController.play();
        }
    }

    public void togglePlayPause() {
        if (mediaController != null) {
            if (mediaController.isPlaying()) {
                mediaController.pause();
            } else {
                mediaController.play();
            }
        }
    }

    public void playNext() {
        Song nextSong = playlistManager.next();
        if (nextSong != null) {
            selectSong(nextSong);
        }
    }

    public void playPrevious() {
        Song prevSong = playlistManager.previous();
        if (prevSong != null) {
            selectSong(prevSong);
        }
    }

    public void seekTo(long position) {
        if (mediaController != null) {
            mediaController.seekTo(position);
        }
    }

    public void rewind() {
        if (mediaController != null) {
            long newPos = Math.max(0, mediaController.getCurrentPosition() - 5000);
            mediaController.seekTo(newPos);
        }
    }

    public void forward() {
        if (mediaController != null) {
            long newPos = Math.min(mediaController.getDuration(), mediaController.getCurrentPosition() + 5000);
            mediaController.seekTo(newPos);
        }
    }

    public void setSpeed(float speedValue) {
        speed.setValue(speedValue);
        if (mediaController != null) {
            mediaController.setPlaybackParameters(new PlaybackParameters(speedValue, pitch.getValue()));
        }
    }

    public void setPitch(float pitchValue) {
        pitch.setValue(pitchValue);
        if (mediaController != null) {
            mediaController.setPlaybackParameters(new PlaybackParameters(speed.getValue(), pitchValue));
        }
    }

    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Long> getCurrentPosition() { return currentPosition; }
    public LiveData<Long> getDuration() { return duration; }
    public LiveData<Integer> getAudioSessionId() { return audioSessionId; }
    public LiveData<Float> getSpeed() { return speed; }
    public LiveData<Float> getPitch() { return pitch; }

    @Override
    protected void onCleared() {
        super.onCleared();
        progressHandler.removeCallbacks(updateProgressAction);
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
    }

    public LiveData<Song> getSelectedSong() {
        return selectedSong;
    }

    public PlaylistManager getPlaylistManager() {
        return playlistManager;
    }
}
