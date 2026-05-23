package com.example.mtplayer;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.mtplayer.models.Song;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mtplayer.databinding.ActivityMainBinding;
import com.example.mtplayer.viewmodels.SongViewModel;
import com.bumptech.glide.Glide;

import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private SongViewModel songViewModel;
    private static final String TAG = "MainActivity";
    private ContentObserver songObserver;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean readGranted = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Boolean granted = result.get(Manifest.permission.READ_MEDIA_AUDIO);
                    readGranted = (granted != null && granted);
                } else {
                    Boolean granted = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);
                    readGranted = (granted != null && granted);
                }
                
                if (readGranted) {
                    loadSongs();
                } else {
                    Snackbar.make(binding.getRoot(), "Permission denied to read audio files", Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        songViewModel = new ViewModelProvider(this).get(SongViewModel.class);
        songViewModel.initController(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found. Make sure the layout contains a NavHostFragment with id nav_host_fragment_content_main");
        }
        NavController navController = navHostFragment.getNavController();
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        setupMiniPlayer(navController);

        checkPermissionsAndLoadSongs();
        setupContentObserver();
    }

    private void setupContentObserver() {
        songObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                Log.d(TAG, "MediaStore changed, reloading songs...");
                loadSongs();
            }
        };
        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                songObserver
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (songObserver != null) {
            getContentResolver().unregisterContentObserver(songObserver);
        }
    }

    private void setupMiniPlayer(NavController navController) {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.SecondFragment) {
                binding.miniPlayer.miniPlayerContainer.setVisibility(View.GONE);
            } else {
                updateMiniPlayerVisibility();
            }
        });

        songViewModel.getSelectedSong().observe(this, song -> {
            if (song != null) {
                binding.miniPlayer.tvMiniTitle.setText(song.getTitle());
                binding.miniPlayer.tvMiniArtist.setText(song.getArtist());
                Glide.with(this)
                        .load(song.getAlbumArtUri())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.miniPlayer.ivMiniAlbumArt);
                
                updateMiniPlayerVisibility();
            }
        });

        songViewModel.getIsPlaying().observe(this, isPlaying -> {
            binding.miniPlayer.btnMiniPlayPause.setImageResource(isPlaying ?
                    android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        });

        binding.miniPlayer.btnMiniPlayPause.setOnClickListener(v -> songViewModel.togglePlayPause());
        binding.miniPlayer.btnMiniPrev.setOnClickListener(v -> songViewModel.playPrevious());
        binding.miniPlayer.btnMiniNext.setOnClickListener(v -> songViewModel.playNext());
        
        binding.miniPlayer.miniPlayerContainer.setOnClickListener(v -> {
            navController.navigate(R.id.SecondFragment);
        });
    }

    private void updateMiniPlayerVisibility() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            boolean showMiniPlayer = navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.SecondFragment &&
                songViewModel.getSelectedSong().getValue() != null;
            
            binding.miniPlayer.miniPlayerContainer.setVisibility(showMiniPlayer ? View.VISIBLE : View.GONE);
        }
    }

    private void checkPermissionsAndLoadSongs() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        permissions.add(Manifest.permission.RECORD_AUDIO);

        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissionsLauncher.launch(listPermissionsNeeded.toArray(new String[0]));
        } else {
            loadSongs();
        }
    }

    private void loadSongs() {
        Log.d(TAG, "Starting loadSongs...");
        
        new Thread(() -> {
            Uri collection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }

            String[] projection = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.MIME_TYPE
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

            List<Song> songList = new ArrayList<>();

            try (Cursor cursor = getContentResolver().query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder
            )) {
                if (cursor != null) {
                    Log.d(TAG, "Cursor count: " + cursor.getCount());
                    if (cursor.moveToFirst()) {
                        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                        int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                        int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                        int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                        int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                        int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                        do {
                            long id = cursor.getLong(idColumn);
                            String title = cursor.getString(titleColumn);
                            String artist = cursor.getString(artistColumn);
                            String album = cursor.getString(albumColumn);
                            long albumId = cursor.getLong(albumIdColumn);
                            long duration = cursor.getLong(durationColumn);

                            Uri contentUri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                            Song song = new Song(id, title, artist, album, albumId, duration, contentUri);
                            songList.add(song);
                        } while (cursor.moveToNext());
                    }
                } else {
                    Log.e(TAG, "Cursor is null");
                }
                
                final List<Song> finalSongList = songList;
                runOnUiThread(() -> {
                    Log.d(TAG, "Updating ViewModel with " + finalSongList.size() + " songs");
                    songViewModel.setSongs(finalSongList);
                    if (finalSongList.isEmpty()) {
                        Snackbar.make(binding.getRoot(), "No music files found", Snackbar.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading songs", e);
                runOnUiThread(() -> Snackbar.make(binding.getRoot(), "Error loading songs: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem darkModeItem = menu.findItem(R.id.action_dark_mode);
        if (darkModeItem != null) {
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            darkModeItem.setChecked(prefs.getBoolean("dark_mode", false));
        }

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        songViewModel.setSearchQuery(newText);
                        return true;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_dark_mode) {
            boolean isDarkMode = !item.isChecked();
            item.setChecked(isDarkMode);
            SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
            prefs.edit().putBoolean("dark_mode", isDarkMode).apply();
            
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found. Make sure the layout contains a NavHostFragment with id nav_host_fragment_content_main");
        }
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}