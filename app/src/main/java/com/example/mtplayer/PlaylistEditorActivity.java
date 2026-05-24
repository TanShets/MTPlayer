package com.example.mtplayer;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.mtplayer.adapters.SelectableSongAdapter;
import com.example.mtplayer.database.AppDatabase;
import com.example.mtplayer.database.Playlist;
import com.example.mtplayer.database.PlaylistSong;
import com.example.mtplayer.databinding.ActivityPlaylistEditorBinding;
import com.example.mtplayer.models.Song;
import com.example.mtplayer.viewmodels.SongViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlaylistEditorActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYLIST_ID = "extra_playlist_id";
    public static final String EXTRA_PLAYLIST_NAME = "extra_playlist_name";

    private ActivityPlaylistEditorBinding binding;
    private SelectableSongAdapter adapter;
    private AppDatabase database;
    private int playlistId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaylistEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        database = AppDatabase.getDatabase(this);
        adapter = new SelectableSongAdapter();
        binding.rvSongsSelection.setAdapter(adapter);

        if (getIntent().hasExtra(EXTRA_PLAYLIST_ID)) {
            playlistId = getIntent().getIntExtra(EXTRA_PLAYLIST_ID, -1);
            String name = getIntent().getStringExtra(EXTRA_PLAYLIST_NAME);
            binding.etPlaylistName.setText(name);
            setTitle("Edit Playlist");
            loadPlaylistSongs();
        } else {
            setTitle("Create Playlist");
            loadAllSongs(null);
        }
    }

    private void loadPlaylistSongs() {
        database.playlistDao().getSongIdsForPlaylist(playlistId).observe(this, this::loadAllSongs);
    }

    private void loadAllSongs(List<Long> selectedIds) {
        new Thread(() -> {
            Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q 
                ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
            };

            List<Song> songList = new ArrayList<>();
            try (Cursor cursor = getContentResolver().query(collection, projection, 
                    MediaStore.Audio.Media.IS_MUSIC + " != 0", null, 
                    MediaStore.Audio.Media.TITLE + " ASC")) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                    int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                    do {
                        long id = cursor.getLong(idCol);
                        songList.add(new Song(id, cursor.getString(titleCol), 
                            cursor.getString(artistCol), cursor.getString(albumCol),
                            cursor.getLong(albumIdCol), cursor.getLong(durationCol),
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)));
                    } while (cursor.moveToNext());
                }
            }
            runOnUiThread(() -> adapter.setSongs(songList, selectedIds));
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_playlist_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_save) {
            savePlaylist();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void savePlaylist() {
        String name = binding.etPlaylistName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a playlist name", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<Long> selectedIds = adapter.getSelectedSongIds();
        new Thread(() -> {
            if (playlistId == -1) {
                // Create new
                Playlist playlist = new Playlist(name);
                int newId = (int) database.playlistDao().insertPlaylist(playlist);
                for (Long songId : selectedIds) {
                    database.playlistDao().insertPlaylistSong(new PlaylistSong(newId, songId));
                }
            } else {
                // Update existing
                Playlist playlist = new Playlist(name);
                playlist.setId(playlistId);
                database.playlistDao().updatePlaylist(playlist);
                
                database.playlistDao().deleteAllSongsFromPlaylist(playlistId);
                for (Long songId : selectedIds) {
                    database.playlistDao().insertPlaylistSong(new PlaylistSong(playlistId, songId));
                }
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Playlist saved", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
