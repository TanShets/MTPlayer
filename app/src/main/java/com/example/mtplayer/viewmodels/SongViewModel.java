package com.example.mtplayer.viewmodels;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.mtplayer.managers.PlaylistManager;
import com.example.mtplayer.models.Song;
import com.example.mtplayer.services.PlayerService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
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
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>();
    private final MutableLiveData<Boolean> shuffleModeEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> repeatMode = new MutableLiveData<>(Player.REPEAT_MODE_ALL);
    private final MutableLiveData<Boolean> stopAfterCurrent = new MutableLiveData<>(false);
    private final MutableLiveData<String> playbackSource = new MutableLiveData<>("All Songs");
    
    private List<Song> allSongs = new ArrayList<>();
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "MTPlayerPrefs";
    private static final String KEY_SHUFFLE = "shuffle_mode";
    private static final String KEY_REPEAT = "repeat_mode";
    private static final String KEY_STOP_AFTER = "stop_after_current";

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

        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        shuffleModeEnabled.setValue(prefs.getBoolean(KEY_SHUFFLE, false));
        repeatMode.setValue(prefs.getInt(KEY_REPEAT, Player.REPEAT_MODE_ALL));
        stopAfterCurrent.setValue(prefs.getBoolean(KEY_STOP_AFTER, false));

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
                if (speed.getValue() != null && pitch.getValue() != null) {
                    mediaController.setPlaybackParameters(new PlaybackParameters(speed.getValue(), pitch.getValue()));
                }
                if (shuffleModeEnabled.getValue() != null) {
                    mediaController.setShuffleModeEnabled(shuffleModeEnabled.getValue());
                }
                if (repeatMode.getValue() != null) {
                    mediaController.setRepeatMode(repeatMode.getValue());
                }

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
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && Boolean.TRUE.equals(stopAfterCurrent.getValue())) {
                            mediaController.pause();
                            // Optional: Reset stopAfterCurrent if you want it to be a one-shot behavior
                            // stopAfterCurrent.setValue(false);
                            // if (prefs != null) prefs.edit().putBoolean(KEY_STOP_AFTER, false).apply();
                        }

                        if (mediaItem != null && mediaItem.mediaId != null) {
                            duration.setValue(mediaController.getDuration());
                            
                            // Find the song in our list and update selectedSong
                            List<Song> currentSongs = songs.getValue();
                            if (currentSongs != null) {
                                for (Song s : currentSongs) {
                                    if (String.valueOf(s.getId()).equals(mediaItem.mediaId)) {
                                        selectedSong.setValue(s);
                                        playlistManager.setCurrentSong(s);
                                        break;
                                    }
                                }
                            }
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
        this.allSongs = new ArrayList<>(songList);
        songs.setValue(songList);
        playlistManager.setPlaylist(songList);
    }

    public void playQueue(List<Song> newQueue, int startIndex, String sourceName) {
        if (mediaController != null && newQueue != null && !newQueue.isEmpty()) {
            playbackSource.setValue(sourceName);
            playlistManager.setPlaylist(newQueue);
            
            List<MediaItem> mediaItems = new ArrayList<>();
            for (Song s : newQueue) {
                MediaMetadata metadata = new MediaMetadata.Builder()
                        .setTitle(s.getTitle())
                        .setArtist(s.getArtist())
                        .setAlbumTitle(s.getAlbum())
                        .setArtworkUri(s.getAlbumArtUri())
                        .build();

                mediaItems.add(new MediaItem.Builder()
                        .setUri(s.getUri())
                        .setMediaId(String.valueOf(s.getId()))
                        .setMediaMetadata(metadata)
                        .build());
            }
            mediaController.setMediaItems(mediaItems, startIndex, 0);
            mediaController.prepare();
            mediaController.play();
        }
    }

    public LiveData<List<Song>> getSongs() {
        return songs;
    }

    public void selectSong(Song song) {
        if (mediaController != null) {
            // Constraint: Reset to all songs when selected from "All Songs" tab
            playQueue(allSongs, allSongs.indexOf(song), "All Songs");
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
        if (mediaController != null) {
            if (Boolean.TRUE.equals(stopAfterCurrent.getValue())) {
                mediaController.pause();
            } else if (repeatMode.getValue() != null && repeatMode.getValue() == Player.REPEAT_MODE_ONE) {
                mediaController.seekTo(0);
            } else {
                if (mediaController.hasNextMediaItem()) {
                    mediaController.seekToNext();
                } else {
                    // Manual circular behavior if seekToNext is restricted
                    mediaController.seekTo(0, 0);
                }
            }
        }
    }

    public void playPrevious() {
        if (mediaController != null) {
            if (mediaController.hasPreviousMediaItem()) {
                mediaController.seekToPrevious();
            } else if (mediaController.getMediaItemCount() > 0) {
                mediaController.seekTo(mediaController.getMediaItemCount() - 1, 0);
            }
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
        if (speed.getValue() != null && Math.abs(speed.getValue() - speedValue) < 0.01f) {
            return;
        }
        speed.setValue(speedValue);
        if (mediaController != null) {
            mediaController.setPlaybackParameters(new PlaybackParameters(speedValue, pitch.getValue() != null ? pitch.getValue() : 1.0f));
        }
    }

    public void setPitch(float pitchValue) {
        if (pitch.getValue() != null && Math.abs(pitch.getValue() - pitchValue) < 0.01f) {
            return;
        }
        pitch.setValue(pitchValue);
        if (mediaController != null) {
            mediaController.setPlaybackParameters(new PlaybackParameters(speed.getValue() != null ? speed.getValue() : 1.0f, pitchValue));
        }
    }

    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Long> getCurrentPosition() { return currentPosition; }
    public LiveData<Long> getDuration() { return duration; }
    public LiveData<Integer> getAudioSessionId() { return audioSessionId; }
    public LiveData<Float> getSpeed() { return speed; }
    public LiveData<Float> getPitch() { return pitch; }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }

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

    public LiveData<Boolean> getShuffleModeEnabled() {
        return shuffleModeEnabled;
    }

    public LiveData<Integer> getRepeatMode() {
        return repeatMode;
    }

    public LiveData<Boolean> getStopAfterCurrent() {
        return stopAfterCurrent;
    }

    public LiveData<String> getPlaybackSource() {
        return playbackSource;
    }

    public void toggleShuffleMode() {
        boolean newValue = !Boolean.TRUE.equals(shuffleModeEnabled.getValue());
        shuffleModeEnabled.setValue(newValue);
        if (mediaController != null) {
            mediaController.setShuffleModeEnabled(newValue);
        }
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_SHUFFLE, newValue).apply();
        }
    }

    public void cycleRepeatMode() {
        // Next Song (Repeat Off/All) -> Stop -> Repeat Current -> Next Song
        if (Boolean.TRUE.equals(stopAfterCurrent.getValue())) {
            // Currently on "Stop", move to "Repeat Current"
            stopAfterCurrent.setValue(false);
            repeatMode.setValue(Player.REPEAT_MODE_ONE);
            if (mediaController != null) {
                mediaController.setRepeatMode(Player.REPEAT_MODE_ONE);
            }
            if (prefs != null) {
                prefs.edit().putBoolean(KEY_STOP_AFTER, false).putInt(KEY_REPEAT, Player.REPEAT_MODE_ONE).apply();
            }
        } else if (repeatMode.getValue() != null && repeatMode.getValue() == Player.REPEAT_MODE_ONE) {
            // Currently on "Repeat Current", move to "Next Song" (Circular)
            repeatMode.setValue(Player.REPEAT_MODE_ALL);
            if (mediaController != null) {
                mediaController.setRepeatMode(Player.REPEAT_MODE_ALL);
            }
            if (prefs != null) {
                prefs.edit().putInt(KEY_REPEAT, Player.REPEAT_MODE_ALL).apply();
            }
        } else {
            // Currently on "Next Song", move to "Stop"
            stopAfterCurrent.setValue(true);
            if (prefs != null) {
                prefs.edit().putBoolean(KEY_STOP_AFTER, true).apply();
            }
        }
    }

    public PlaylistManager getPlaylistManager() {
        return playlistManager;
    }
}
