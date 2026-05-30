package com.example.mtplayer.audio;

/**
 * JNI wrapper for the SoundTouch library.
 */
public class SoundTouch {
    static {
        System.loadLibrary("soundtouch-jni");
    }

    // SoundTouch Settings (matching SoundTouch.h)
    public static final int SETTING_USE_AA_FILTER = 0;
    public static final int SETTING_AA_FILTER_LENGTH = 1;
    public static final int SETTING_USE_QUICKSEEK = 2;
    public static final int SETTING_SEQUENCE_MS = 3;
    public static final int SETTING_SEEKWINDOW_MS = 4;
    public static final int SETTING_OVERLAP_MS = 5;
    public static final int SETTING_NOMINAL_INPUT_SEQUENCE = 6;
    public static final int SETTING_NOMINAL_OUTPUT_SEQUENCE = 7;
    public static final int SETTING_INITIAL_LATENCY = 8;

    private long nativeHandle;

    public SoundTouch() {
        nativeHandle = nativeInit();
    }

    public void setPitch(float pitch) {
        nativeSetPitch(nativeHandle, pitch);
    }

    public void setPitchSemiTones(float semitones) {
        nativeSetPitchSemiTones(nativeHandle, semitones);
    }

    public void setTempo(float tempo) {
        nativeSetTempo(nativeHandle, tempo);
    }

    /**
     * @param samples The float samples.
     * @param numFrames Number of frames (samples per channel).
     */
    public void putSamples(float[] samples, int numFrames) {
        nativePutSamples(nativeHandle, samples, numFrames);
    }

    /**
     * @param outBuffer Buffer to receive samples.
     * @param maxFrames Maximum number of frames to receive.
     * @return Number of frames received.
     */
    public int receiveSamples(float[] outBuffer, int maxFrames) {
        return nativeReceiveSamples(nativeHandle, outBuffer, maxFrames);
    }

    public int numSamples() {
        return nativeNumSamples(nativeHandle);
    }

    public void flush() {
        nativeFlush(nativeHandle);
    }

    public void release() {
        if (nativeHandle != 0) {
            nativeRelease(nativeHandle);
            nativeHandle = 0;
        }
    }

    public void setSampleRate(int sampleRate) {
        nativeSetSampleRate(nativeHandle, sampleRate);
    }

    public void setChannels(int channels) {
        nativeSetChannels(nativeHandle, channels);
    }

    public void setSetting(int settingId, int value) {
        nativeSetSetting(nativeHandle, settingId, value);
    }

    private native long nativeInit();
    private native void nativeRelease(long handle);
    private native void nativeSetPitch(long handle, float pitch);
    private native void nativeSetPitchSemiTones(long handle, float semitones);
    private native void nativeSetTempo(long handle, float tempo);
    private native void nativeSetSampleRate(long handle, int sampleRate);
    private native void nativeSetChannels(long handle, int channels);
    private native void nativePutSamples(long handle, float[] samples, int numFrames);
    private native int nativeReceiveSamples(long handle, float[] outBuffer, int maxFrames);
    private native int nativeNumSamples(long handle);
    private native void nativeFlush(long handle);
    private native void nativeSetSetting(long handle, int settingId, int value);
}
