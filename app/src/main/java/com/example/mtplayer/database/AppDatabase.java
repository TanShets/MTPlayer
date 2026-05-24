package com.example.mtplayer.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Playlist.class, PlaylistSong.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PlaylistDao playlistDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "mtplayer_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
