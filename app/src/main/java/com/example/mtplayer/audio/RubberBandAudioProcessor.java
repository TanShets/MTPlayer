package com.example.mtplayer.audio;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.util.UnstableApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An {@link AudioProcessor} that uses the Rubber Band Library for high-quality
 * pitch shifting and time stretching.
 */
@UnstableApi
public class RubberBandAudioProcessor implements AudioProcessor {

    private static final String TAG = "RubberBandAudioProcessor";

    private float pitch = 1.0f;
    private float speed = 1.0f;
    private boolean parametersChanged = false;

    private AudioFormat inputAudioFormat;
    private AudioFormat outputAudioFormat;

    private @Nullable RubberBandStretcher stretcher;
    private ByteBuffer buffer;
    private ByteBuffer outputBuffer;
    private boolean inputEnded;

    private float[] inputFloats;
    private float[] outputFloats;
    private short[] inputShorts;
    private short[] outputShorts;

    private long totalInputFrames = 0;
    private long totalOutputFrames = 0;
    private long lastLogTime = 0;

    public RubberBandAudioProcessor() {
        inputAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
        // Pre-allocate a reasonable buffer to avoid GC pressure
        buffer = ByteBuffer.allocateDirect(1024 * 64).order(ByteOrder.nativeOrder());
        outputBuffer = EMPTY_BUFFER;
        inputFloats = new float[0];
        outputFloats = new float[0];
        inputShorts = new short[0];
        outputShorts = new short[0];
    }

    /**
     * Sets the target pitch.
     */
    public synchronized void setPitch(float pitch) {
        float roundedPitch = Math.round(pitch * 100.0f) / 100.0f;
        if (this.pitch != roundedPitch) {
            Log.d(TAG, "PITCH_CHANGE: " + this.pitch + " -> " + roundedPitch);
            this.pitch = roundedPitch;
            parametersChanged = true;
        }
    }

    /**
     * Sets the target speed (tempo).
     */
    public synchronized void setSpeed(float speed) {
        float roundedSpeed = Math.round(speed * 100.0f) / 100.0f;
        if (this.speed != roundedSpeed) {
            Log.d(TAG, "SPEED_CHANGE: " + this.speed + " -> " + roundedSpeed);
            this.speed = roundedSpeed;
            parametersChanged = true;
        }
    }

    /**
     * Calculates the media duration from the playout duration.
     */
    public synchronized long getMediaDuration(long playoutDuration) {
        return (long) (playoutDuration * speed);
    }

    private synchronized void updateParameters() {
        if (stretcher != null && parametersChanged) {
            double roundedPitch = Math.round(pitch * 100.0) / 100.0;
            double roundedSpeed = Math.round(speed * 100.0) / 100.0;
            stretcher.setPitchScale(roundedPitch);
            stretcher.setTimeRatio(1.0 / roundedSpeed);
            parametersChanged = false;
        }
    }

    @Override
    public synchronized AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            // Force 16-bit PCM input. ExoPlayer will insert a converter if needed.
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }
        
        if (!this.inputAudioFormat.equals(inputAudioFormat)) {
            Log.d(TAG, "Format changed: " + this.inputAudioFormat + " -> " + inputAudioFormat);
            
            // Full reset to ensure no state from previous format leaks
            flush(); 
            if (stretcher != null) {
                stretcher.release();
                stretcher = null;
            }
            
            this.inputAudioFormat = inputAudioFormat;
            this.outputAudioFormat = inputAudioFormat;
        }
        
        return outputAudioFormat;
    }

    @Override
    public synchronized boolean isActive() {
        return inputAudioFormat != AudioFormat.NOT_SET;
    }

    @Override
    public synchronized void queueInput(ByteBuffer inputBuffer) {
        int remaining = inputBuffer.remaining();
        if (remaining == 0) {
            return;
        }

        if (stretcher == null) {
            Log.d(TAG, "Initializing stretcher for " + inputAudioFormat.sampleRate + "Hz, " + inputAudioFormat.channelCount + " channels");
            stretcher = new RubberBandStretcher(
                    inputAudioFormat.sampleRate,
                    inputAudioFormat.channelCount,
                    1.0 / speed,
                    pitch
            );
            if (!stretcher.isValid()) {
                Log.e(TAG, "Failed to initialize stretcher!");
                stretcher = null;
                inputBuffer.position(inputBuffer.limit());
                return;
            }
        }

        updateParameters();

        int frameSize = 2 * inputAudioFormat.channelCount;
        int frameCount = remaining / frameSize;
        int totalSamples = frameCount * inputAudioFormat.channelCount;
        
        if (totalSamples > 0) {
            if (inputFloats.length < totalSamples) {
                inputFloats = new float[totalSamples];
            }
            if (inputShorts.length < totalSamples) {
                inputShorts = new short[totalSamples];
            }
            
            // Optimization: Use bulk read from the ByteBuffer
            inputBuffer.asShortBuffer().get(inputShorts, 0, totalSamples);
            
            for (int inputIdx = 0; inputIdx < totalSamples; inputIdx++) {
                inputFloats[inputIdx] = inputShorts[inputIdx] / 32768.0f;
            }

            if (stretcher != null && stretcher.isValid()) {
                stretcher.process(inputFloats, totalSamples, false);
                totalInputFrames += frameCount;
            }
        }
        
        inputBuffer.position(inputBuffer.limit());
    }

    @Override
    public synchronized void queueEndOfStream() {
        Log.d(TAG, "queueEndOfStream() called. Total input frames: " + totalInputFrames);
        inputEnded = true;
        if (stretcher != null && stretcher.isValid()) {
            // Safe to call now that JNI has safety pointers
            stretcher.process(new float[0], 0, true);
        }
    }

    @Override
    public synchronized ByteBuffer getOutput() {
        if (stretcher == null || !stretcher.isValid()) {
            return EMPTY_BUFFER;
        }

        if (outputBuffer.hasRemaining()) {
            return outputBuffer;
        }

        int available = stretcher.available();
        if (available > 0) {
            int channels = inputAudioFormat.channelCount;
            int totalSamples = available * channels;
            int byteCount = totalSamples * 2; 
            
            if (buffer.capacity() < byteCount) {
                Log.d(TAG, "Reallocating output buffer to " + byteCount);
                buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
            } else {
                buffer.clear();
            }

            if (outputFloats.length < totalSamples) {
                outputFloats = new float[totalSamples];
            }
            if (outputShorts.length < totalSamples) {
                outputShorts = new short[totalSamples];
            }
            
            int retrieved = stretcher.retrieve(outputFloats);
            if (retrieved > 0) {
                totalOutputFrames += retrieved;
                
                // Monitor for drift every 5 seconds
                long currentTime = android.os.SystemClock.elapsedRealtime();
                if (currentTime - lastLogTime > 5000) {
                    double expectedOutput = totalInputFrames / speed;
                    double drift = totalOutputFrames - expectedOutput;
                    Log.d(TAG, "Drift: " + drift + " frames (Latency: " + stretcher.getLatency() + ")");
                    lastLogTime = currentTime;
                }

                int retrievedSamples = retrieved * channels;
                for (int m = 0; m < retrievedSamples; m++) {
                    float f = outputFloats[m];
                    if (f > 1.0f) f = 1.0f;
                    else if (f < -1.0f) f = -1.0f;
                    outputShorts[m] = (short) (f * 32767.0f);
                }
                
                // Bulk write to ByteBuffer
                buffer.asShortBuffer().put(outputShorts, 0, retrievedSamples);
                buffer.position(retrievedSamples * 2);
                buffer.flip();
                outputBuffer = buffer;
            } else {
                outputBuffer = EMPTY_BUFFER;
            }
        } else {
            outputBuffer = EMPTY_BUFFER;
            if (inputEnded && available == 0) {
                Log.d(TAG, "Drained all frames after EOS");
            }
        }

        return outputBuffer;
    }

    @Override
    public synchronized boolean isEnded() {
        // Must drain the current output buffer first
        if (outputBuffer.hasRemaining()) {
            return false;
        }
        if (!inputEnded) {
            return false;
        }
        if (stretcher == null) {
            return true;
        }
        // Rubber Band available() returns -1 when the stream is fully processed and all output retrieved.
        int avail = stretcher.available();
        if (avail == -1) {
            Log.d(TAG, "isEnded: Stretcher signaled completion (-1). Total output frames: " + totalOutputFrames);
            return true;
        }
        return avail == 0;
    }

    @Override
    public synchronized void flush() {
        Log.d(TAG, "flush() called - recreating stretcher");
        if (stretcher != null) {
            stretcher.release();
            stretcher = null;
        }
        outputBuffer = EMPTY_BUFFER;
        inputEnded = false;
        totalInputFrames = 0;
        totalOutputFrames = 0;
    }

    @Override
    public synchronized void reset() {
        flush();
        if (stretcher != null) {
            stretcher.release();
            stretcher = null;
        }
        inputAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
    }
}
