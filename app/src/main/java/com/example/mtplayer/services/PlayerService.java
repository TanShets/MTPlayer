package com.example.mtplayer.services;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.Player;
import androidx.media3.common.audio.SonicAudioProcessor;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DefaultAudioSink;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.mtplayer.MainActivity;

import java.util.ArrayList;

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

        // Heavier processing factory: Overrides the audio renderer to use a custom high-fidelity sink
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this) {
            @Override
            protected void buildAudioRenderers(
                    Context context,
                    int extensionRendererMode,
                    MediaCodecSelector mediaCodecSelector,
                    boolean enableDecoderFallback,
                    AudioSink audioSink,
                    Handler eventHandler,
                    AudioRendererEventListener eventListener,
                    ArrayList<Renderer> out) {
                
                // We inject a custom Sonic processor that oversamples to 96kHz to reduce aliasing
                SonicAudioProcessor highQualityProcessor = new SonicAudioProcessor();
                highQualityProcessor.setOutputSampleRateHz(96000);

                AudioSink highQualitySink = new DefaultAudioSink.Builder(context)
                        .setAudioProcessorChain(new DefaultAudioSink.DefaultAudioProcessorChain(highQualityProcessor))
                        .setEnableFloatOutput(true) // 32-bit float precision
                        .setEnableAudioTrackPlaybackParams(true) // Use hardware-optimized resampler on top of our oversampling
                        .build();

                super.buildAudioRenderers(context, extensionRendererMode, mediaCodecSelector, 
                        enableDecoderFallback, highQualitySink, eventHandler, eventListener, out);
            }
        };

        player = new ExoPlayer.Builder(this, renderersFactory)
                .setAudioAttributes(audioAttributes, true)
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
