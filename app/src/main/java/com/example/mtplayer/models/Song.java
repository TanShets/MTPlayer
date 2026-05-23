package com.example.mtplayer.models;

import android.content.ContentUris;
import android.net.Uri;

public class Song {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final long albumId;
    private final long duration;
    private final Uri uri;

    public Song(long id, String title, String artist, String album, long albumId, long duration, Uri uri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumId = albumId;
        this.duration = duration;
        this.uri = uri;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public long getAlbumId() { return albumId; }
    public long getDuration() { return duration; }
    public Uri getUri() { return uri; }
    
    public Uri getAlbumArtUri() {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
    }
}
