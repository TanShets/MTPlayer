package com.example.mtplayer.managers;

import com.example.mtplayer.models.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistManager {
    private List<Song> originalPlaylist = new ArrayList<>();
    private List<Song> currentPlaylist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isShuffleMode = false;
    private boolean isRepeatMode = false;

    public void setPlaylist(List<Song> songs) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPlaylist = new ArrayList<>(songs);
        if (isShuffleMode) {
            Collections.shuffle(currentPlaylist);
        }
        currentIndex = -1;
    }

    public List<Song> getCurrentPlaylist() {
        return currentPlaylist;
    }

    public Song setCurrentSong(Song song) {
        currentIndex = currentPlaylist.indexOf(song);
        return song;
    }

    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < currentPlaylist.size()) {
            return currentPlaylist.get(currentIndex);
        }
        return null;
    }

    public Song next() {
        if (currentPlaylist.isEmpty()) return null;
        
        currentIndex++;
        if (currentIndex >= currentPlaylist.size()) {
            currentIndex = 0;
        }
        return currentPlaylist.get(currentIndex);
    }

    public Song previous() {
        if (currentPlaylist.isEmpty()) return null;

        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = currentPlaylist.size() - 1;
        }
        return currentPlaylist.get(currentIndex);
    }

    public void toggleShuffle() {
        isShuffleMode = !isShuffleMode;
        Song currentSong = getCurrentSong();
        if (isShuffleMode) {
            Collections.shuffle(currentPlaylist);
        } else {
            currentPlaylist = new ArrayList<>(originalPlaylist);
        }
        if (currentSong != null) {
            currentIndex = currentPlaylist.indexOf(currentSong);
        }
    }

    public void toggleRepeat() {
        isRepeatMode = !isRepeatMode;
    }

    public boolean isShuffleMode() {
        return isShuffleMode;
    }

    public boolean isRepeatMode() {
        return isRepeatMode;
    }
}
