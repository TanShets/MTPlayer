package com.example.mtplayer.audio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * An {@link AudioProcessor} that uses SoundTouch for pitch shifting and time stretching.
 * Optimized for high quality using PCM FLOAT processing.
 */
@UnstableApi
public class SoundTouchAudioProcessor implements AudioProcessor {

    private float pitch = 1.0f;
    private float speed = 1.0f;
    private boolean parametersChanged = false;

    private AudioFormat inputAudioFormat;
    private AudioFormat outputAudioFormat;

    private @Nullable SoundTouch soundTouch;
    private ByteBuffer buffer;
    private ByteBuffer outputBuffer;
    private boolean inputEnded;

    private float[] inputFloats;
    private short[] inputShorts;
    private float[] outputFloats;

    public SoundTouchAudioProcessor() {
        inputAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
        buffer = ByteBuffer.allocateDirect(1024 * 64).order(ByteOrder.nativeOrder());
        outputBuffer = EMPTY_BUFFER;
        inputFloats = new float[0];
        inputShorts = new short[0];
        outputFloats = new float[0];
    }

    public synchronized void setPitch(float pitch) {
        float clampedPitch = Math.max(0.1f, Math.min(pitch, 8.0f));
        if (this.pitch != clampedPitch) {
            this.pitch = clampedPitch;
            parametersChanged = true;
        }
    }

    public synchronized void setSpeed(float speed) {
        float clampedSpeed = Math.max(0.1f, Math.min(speed, 8.0f));
        if (this.speed != clampedSpeed) {
            this.speed = clampedSpeed;
            parametersChanged = true;
        }
    }

    public synchronized long getMediaDuration(long playoutDuration) {
        return (long) (playoutDuration * speed);
    }

    private synchronized void updateParameters() {
        if (soundTouch != null && parametersChanged) {
            soundTouch.setPitch(pitch);
            soundTouch.setTempo(speed);
            parametersChanged = false;
        }
    }

    @Override
    @NonNull
    public synchronized AudioFormat configure(@NonNull AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }
        
        if (!this.inputAudioFormat.equals(inputAudioFormat)) {
            flush();
            this.inputAudioFormat = inputAudioFormat;
            // Force output to FLOAT for higher quality processing chain
            this.outputAudioFormat = new AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_FLOAT);
        }
        
        return outputAudioFormat;
    }

    @Override
    public synchronized boolean isActive() {
        return inputAudioFormat != AudioFormat.NOT_SET;
    }

    @Override
    public synchronized void queueInput(@NonNull ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining == 0) {
            return;
        }

        if (soundTouch == null) {
            soundTouch = new SoundTouch();
            soundTouch.setSampleRate(inputAudioFormat.sampleRate);
            soundTouch.setChannels(inputAudioFormat.channelCount);
            soundTouch.setPitch(pitch);
            soundTouch.setTempo(speed);
        }

        updateParameters();

        // Ensure we are reading in native byte order to avoid static/noise
        inputBuffer.order(ByteOrder.nativeOrder());

        int frameCount;
        int totalSamples;

        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            frameCount = remaining / (4 * inputAudioFormat.channelCount);
            totalSamples = frameCount * inputAudioFormat.channelCount;
            if (totalSamples > 0) {
                if (inputFloats.length < totalSamples) {
                    inputFloats = new float[totalSamples];
                }
                inputBuffer.asFloatBuffer().get(inputFloats, 0, totalSamples);
                soundTouch.putSamples(inputFloats, frameCount);
                inputBuffer.position(inputBuffer.position() + totalSamples * 4);
            }
        } else {
            // PCM_16BIT
            frameCount = remaining / (2 * inputAudioFormat.channelCount);
            totalSamples = frameCount * inputAudioFormat.channelCount;
            if (totalSamples > 0) {
                if (inputFloats.length < totalSamples) {
                    inputFloats = new float[totalSamples];
                }
                if (inputShorts.length < totalSamples) {
                    inputShorts = new short[totalSamples];
                }
                inputBuffer.asShortBuffer().get(inputShorts, 0, totalSamples);
                // Convert 16-bit short to float [-1.0, 1.0]
                for (int i = 0; i < totalSamples; i++) {
                    inputFloats[i] = inputShorts[i] / 32768.0f;
                }
                soundTouch.putSamples(inputFloats, frameCount);
                inputBuffer.position(inputBuffer.position() + totalSamples * 2);
            }
        }
    }

    @Override
    public synchronized void queueEndOfStream() {
        inputEnded = true;
    }

    @Override
    @NonNull
    public synchronized ByteBuffer getOutput() {
        if (soundTouch == null) {
            return EMPTY_BUFFER;
        }

        if (outputBuffer.hasRemaining()) {
            return outputBuffer;
        }

        int maxReceive = 4096;
        int channels = inputAudioFormat.channelCount;
        int maxSamples = maxReceive * channels;
        
        if (outputFloats.length < maxSamples) {
            outputFloats = new float[maxSamples];
        }

        int receivedFrames = soundTouch.receiveSamples(outputFloats, maxReceive);
        if (receivedFrames > 0) {
            int receivedSamples = receivedFrames * channels;
            int byteCount = receivedSamples * 4;
            
            if (buffer.capacity() < byteCount) {
                buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
            } else {
                buffer.clear();
            }
            
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            floatBuffer.put(outputFloats, 0, receivedSamples);
            buffer.limit(byteCount);
            buffer.position(0);
            outputBuffer = buffer;
        } else {
            outputBuffer = EMPTY_BUFFER;
        }

        return outputBuffer;
    }

    @Override
    public synchronized boolean isEnded() {
        return inputEnded && (soundTouch == null || soundTouch.numSamples() == 0) && !outputBuffer.hasRemaining();
    }

    @Override
    public synchronized void flush() {
        if (soundTouch != null) {
            soundTouch.release();
            soundTouch = null;
        }
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
    }

    @Override
    public synchronized void reset() {
        flush();
        inputAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
    }
}
