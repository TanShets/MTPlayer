package com.example.mtplayer;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.example.mtplayer.managers.PlaylistManager;
import com.example.mtplayer.models.Song;

import java.util.ArrayList;
import java.util.List;

public class PlaylistManagerTest {

    private PlaylistManager playlistManager;
    private List<Song> testSongs;

    @Before
    public void setUp() {
        playlistManager = new PlaylistManager();
        testSongs = new ArrayList<>();
        // Mocking songs (Uri won't be used in logic, just need non-null for index check if necessary, 
        // but Song constructor takes it. In Unit tests, Uri.parse might fail if not mocked/robolectric, 
        // let's see if we can use null or a dummy string if the logic doesn't touch it.)
        // Actually, let's just use dummy values.
        testSongs.add(new Song(1, "Title 1", "Artist 1", "Album 1", 101, 3000, null));
        testSongs.add(new Song(2, "Title 2", "Artist 2", "Album 2", 102, 4000, null));
        testSongs.add(new Song(3, "Title 3", "Artist 3", "Album 3", 103, 5000, null));
        
        playlistManager.setPlaylist(testSongs);
    }

    @Test
    public void testSetPlaylist() {
        assertEquals(3, playlistManager.getCurrentPlaylist().size());
        assertNull(playlistManager.getCurrentSong());
    }

    @Test
    public void testNextCircular() {
        playlistManager.next(); // index 0
        assertEquals("Title 1", playlistManager.getCurrentSong().getTitle());
        playlistManager.next(); // index 1
        assertEquals("Title 2", playlistManager.getCurrentSong().getTitle());
        playlistManager.next(); // index 2
        assertEquals("Title 3", playlistManager.getCurrentSong().getTitle());
        playlistManager.next(); // index 0 (circular)
        assertEquals("Title 1", playlistManager.getCurrentSong().getTitle());
    }

    @Test
    public void testPreviousCircular() {
        playlistManager.previous(); // index 2
        assertEquals("Title 3", playlistManager.getCurrentSong().getTitle());
        playlistManager.previous(); // index 1
        assertEquals("Title 2", playlistManager.getCurrentSong().getTitle());
        playlistManager.previous(); // index 0
        assertEquals("Title 1", playlistManager.getCurrentSong().getTitle());
        playlistManager.previous(); // index 2 (circular)
        assertEquals("Title 3", playlistManager.getCurrentSong().getTitle());
    }

    @Test
    public void testSetCurrentSong() {
        Song target = testSongs.get(1);
        playlistManager.setCurrentSong(target);
        assertEquals(target, playlistManager.getCurrentSong());
        assertEquals(1, testSongs.indexOf(target));
    }

    @Test
    public void testToggleShuffle() {
        playlistManager.setCurrentSong(testSongs.get(0));
        assertFalse(playlistManager.isShuffleMode());
        
        playlistManager.toggleShuffle();
        assertTrue(playlistManager.isShuffleMode());
        
        // Ensure current song is still the same after shuffle
        assertEquals("Title 1", playlistManager.getCurrentSong().getTitle());
        
        playlistManager.toggleShuffle();
        assertFalse(playlistManager.isShuffleMode());
        assertEquals(testSongs.get(0), playlistManager.getCurrentPlaylist().get(0));
    }
}
