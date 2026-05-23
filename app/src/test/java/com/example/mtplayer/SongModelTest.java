package com.example.mtplayer;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.mtplayer.models.Song;

public class SongModelTest {

    @Test
    public void testSongProperties() {
        Song song = new Song(1, "Test Title", "Test Artist", "Test Album", 101, 3000, null);
        
        assertEquals(1, song.getId());
        assertEquals("Test Title", song.getTitle());
        assertEquals("Test Artist", song.getArtist());
        assertEquals("Test Album", song.getAlbum());
        assertEquals(101, song.getAlbumId());
        assertEquals(3000, song.getDuration());
    }
}
