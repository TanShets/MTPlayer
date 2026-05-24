package com.example.mtplayer.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert
    long insertPlaylist(Playlist playlist);

    @androidx.room.Update
    void updatePlaylist(Playlist playlist);

    @Delete
    void deletePlaylist(Playlist playlist);

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    LiveData<List<Playlist>> getAllPlaylists();

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    void insertPlaylistSong(PlaylistSong playlistSong);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void deleteAllSongsFromPlaylist(int playlistId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    void deleteSongFromPlaylist(int playlistId, long songId);

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId")
    LiveData<List<Long>> getSongIdsForPlaylist(int playlistId);
}
