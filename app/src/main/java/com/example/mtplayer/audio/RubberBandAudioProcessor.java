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

    public RubberBandAudioProcessor() {
        inputAudioFormat = AudioFormat.NOT_SET;
        outputAudioFormat = AudioFormat.NOT_SET;
        buffer = EMPTY_BUFFER;
        outputBuffer = EMPTY_BUFFER;
        inputFloats = new float[0];
        outputFloats = new float[0];
    }

    /**
     * Sets the target pitch.
     */
    public synchronized void setPitch(float pitch) {
        Log.d(TAG, "PITCH_CHANGE: " + pitch);
        if (this.pitch != pitch) {
            this.pitch = pitch;
            parametersChanged = true;
        }
    }

    /**
     * Sets the target speed (tempo).
     */
    public synchronized void setSpeed(float speed) {
        Log.d(TAG, "SPEED_CHANGE: " + speed);
        if (this.speed != speed) {
            this.speed = speed;
            parametersChanged = true;
        }
    }

    /**
     * Calculates the media duration from the playout duration, taking speed into account.
     */
    public synchronized long getMediaDuration(long playoutDuration) {
        return (long) (playoutDuration * speed);
    }

    private synchronized void updateParameters() {
        if (stretcher != null && parametersChanged) {
            stretcher.setPitchScale(pitch);
            stretcher.setTimeRatio(1.0 / speed);
            parametersChanged = false;
        }
    }

    @Override
    public synchronized AudioFormat configure(AudioFormat inputAudioFormat) throws UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            // Force 16-bit PCM input. ExoPlayer will insert a converter if needed.
            throw new UnhandledAudioFormatException(inputAudioFormat);
        }
        this.inputAudioFormat = inputAudioFormat;
        this.outputAudioFormat = inputAudioFormat;
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
            Log.d(TAG, "Initializing stretcher: " + inputAudioFormat.sampleRate + "Hz, " + inputAudioFormat.channelCount + "ch");
            stretcher = new RubberBandStretcher(
                    inputAudioFormat.sampleRate,
                    inputAudioFormat.channelCount,
                    1.0 / speed,
                    pitch
            );
            if (!stretcher.isValid()) {
                Log.e(TAG, "Failed to initialize RubberBandStretcher");
                stretcher = null;
                inputBuffer.position(inputBuffer.limit());
                return;
            }
        }

        updateParameters();

        int totalFloats = remaining / (2 * inputAudioFormat.channelCount);
        int totalFloatsCount = totalFloats * inputAudioFormat.channelCount;
        
        if (inputFloats.length < totalFloatsCount) {
            inputFloats = new float[totalFloatsCount];
        }
        
        for (int i = 0; i < totalFloatsCount; i++) {
            inputFloats[i] = inputBuffer.getShort() / 32768.0f;
        }

        if (totalFloatsCount > 0 && stretcher != null && stretcher.isValid()) {
            try {
                Log.v(TAG, "Process start: " + totalFloatsCount);
                stretcher.process(inputFloats, totalFloatsCount, false);
                Log.v(TAG, "Process end");
            } catch (Exception e) {
                Log.e(TAG, "Error during native process", e);
            }
        }
    }

    @Override
    public synchronized void queueEndOfStream() {
        if (stretcher != null && stretcher.isValid()) {
            Log.d(TAG, "EOS_MARKER: Queueing end of stream - avoiding native call");
        }
        inputEnded = true;
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
            Log.v(TAG, "Available: " + available);
            int totalSamples = available * inputAudioFormat.channelCount;
            int byteCount = totalSamples * 2; // 16-bit PCM output
            
            if (buffer.capacity() < byteCount) {
                Log.d(TAG, "Reallocating output buffer: " + byteCount + " bytes");
                buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
            } else {
                buffer.clear();
            }

            if (outputFloats.length < totalSamples) {
                outputFloats = new float[totalSamples];
            }
            
            Log.v(TAG, "Retrieve start: " + available);
            int retrieved = stretcher.retrieve(outputFloats);
            Log.v(TAG, "Retrieve end: " + retrieved);
            
            if (retrieved > 0) {
                int retrievedSamples = retrieved * inputAudioFormat.channelCount;
                for (int i = 0; i < retrievedSamples; i++) {
                    float f = outputFloats[i];
                    // Clamp to [-1.0, 1.0]
                    if (f > 1.0f) f = 1.0f;
                    else if (f < -1.0f) f = -1.0f;
                    buffer.putShort((short) (f * 32767.0f));
                }
                buffer.flip();
                outputBuffer = buffer;
            } else {
                outputBuffer = EMPTY_BUFFER;
            }
        } else {
            outputBuffer = EMPTY_BUFFER;
        }

        return outputBuffer;
    }

    @Override
    public synchronized boolean isEnded() {
        return inputEnded && (stretcher == null || stretcher.available() == 0);
    }

    @Override
    public synchronized void flush() {
        if (stretcher != null) {
            stretcher.release();
            stretcher = null;
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
