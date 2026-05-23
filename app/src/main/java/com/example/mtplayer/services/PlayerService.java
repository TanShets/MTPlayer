package com.example.mtplayer.services;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.common.Player;
import android.util.Log;

import com.example.mtplayer.MainActivity;

public class PlayerService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true) // handles audio focus
                .setHandleAudioBecomingNoisy(true)
                .build();
        player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Log.e("PlayerService", "ExoPlayer error: " + error.getMessage(), error);
            }
        });

        ForwardingPlayer forwardingPlayer = new ForwardingPlayer(player) {
            @Override
            public void seekToPrevious() {
                if (getCurrentPosition() > 10000) {
                    seekTo(0);
                } else {
                    super.seekToPrevious();
                }
            }
        };

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        mediaSession = new MediaSession.Builder(this, forwardingPlayer)
                .setSessionActivity(pendingIntent)
                .setCallback(new MediaSession.Callback() {
                    @Override
                    public MediaSession.ConnectionResult onConnect(MediaSession session, MediaSession.ControllerInfo controllerInfo) {
                        MediaSession.ConnectionResult connectionResult = MediaSession.Callback.super.onConnect(session, controllerInfo);
                        return MediaSession.ConnectionResult.accept(
                                connectionResult.availableSessionCommands,
                                connectionResult.availablePlayerCommands.buildUpon()
                                        .add(Player.COMMAND_SEEK_TO_NEXT)
                                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                                        .build());
                    }
                })
                .build();
        
        Bundle extras = new Bundle();
        extras.putInt("audio_session_id", player.getAudioSessionId());
        mediaSession.setSessionExtras(extras);
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (player != null && !player.getPlayWhenReady()) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }
}
