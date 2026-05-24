package com.example.mtplayer.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "playlist_songs",
    primaryKeys = {"playlistId", "songId"},
    foreignKeys = @ForeignKey(
        entity = Playlist.class,
        parentColumns = "id",
        childColumns = "playlistId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("playlistId")}
)
public class PlaylistSong {
    private int playlistId;
    private long songId;

    public PlaylistSong(int playlistId, long songId) {
        this.playlistId = playlistId;
        this.songId = songId;
    }

    public int getPlaylistId() { return playlistId; }
    public void setPlaylistId(int playlistId) { this.playlistId = playlistId; }
    public long getSongId() { return songId; }
    public void setSongId(long songId) { this.songId = songId; }
}
