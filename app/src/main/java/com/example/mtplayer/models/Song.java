package com.example.mtplayer.models;

import android.net.Uri;

public class Song {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final long duration;
    private final Uri uri;

    public Song(long id, String title, String artist, String album, long duration, Uri uri) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.uri = uri;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public long getDuration() { return duration; }
    public Uri getUri() { return uri; }
}
