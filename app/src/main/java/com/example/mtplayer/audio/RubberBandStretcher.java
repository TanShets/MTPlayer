package com.example.mtplayer.audio;

/**
 * JNI wrapper for the Rubber Band Library (C++).
 * Rubber Band is a high-quality frequency-domain pitch shifter and time stretcher.
 */
public class RubberBandStretcher {
    static {
        System.loadLibrary("rubberband-jni");
    }

    private long nativeHandle;

    public RubberBandStretcher(int sampleRate, int channels, double timeRatio, double pitchScale) {
        nativeHandle = nativeInit(sampleRate, channels, timeRatio, pitchScale);
    }

    /**
     * Feeds audio data into the stretcher.
     * @param input Interleaved PCM data.
     * @param length Number of floats to process from the input array.
     * @param isFinal True if this is the last chunk of the stream.
     */
    public void process(float[] input, int length, boolean isFinal) {
        if (nativeHandle != 0) {
            // Ensure we don't pass a null or empty array if length is 0, 
            // though length 0 is valid for end-of-stream.
            float[] safeInput = (input == null || input.length == 0) ? new float[1] : input;
            nativeProcess(nativeHandle, safeInput, length, isFinal);
        }
    }

    /**
     * @return Number of samples currently available to be retrieved.
     */
    public int available() {
        return nativeHandle != 0 ? nativeAvailable(nativeHandle) : 0;
    }

    /**
     * Retrieves processed audio data.
     * @param output Array to be filled with interleaved PCM data.
     * @return Number of samples actually retrieved.
     */
    public int retrieve(float[] output) {
        if (nativeHandle != 0 && output != null && output.length > 0) {
            return nativeRetrieve(nativeHandle, output);
        }
        return 0;
    }

    public void setPitchScale(double pitchScale) {
        if (nativeHandle != 0) {
            android.util.Log.d("RubberBandStretcher", "Setting pitch scale: " + pitchScale);
            nativeSetPitchScale(nativeHandle, pitchScale);
        }
    }

    public void setTimeRatio(double timeRatio) {
        if (nativeHandle != 0) {
            android.util.Log.d("RubberBandStretcher", "Setting time ratio: " + timeRatio);
            nativeSetTimeRatio(nativeHandle, timeRatio);
        }
    }

    public void release() {
        if (nativeHandle != 0) {
            nativeRelease(nativeHandle);
            nativeHandle = 0;
        }
    }

    public boolean isValid() {
        return nativeHandle != 0;
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private native long nativeInit(int sampleRate, int channels, double timeRatio, double pitchScale);
    private native void nativeProcess(long handle, float[] input, int length, boolean isFinal);
    private native int nativeAvailable(long handle);
    private native int nativeRetrieve(long handle, float[] output);
    private native void nativeSetPitchScale(long handle, double pitchScale);
    private native void nativeSetTimeRatio(long handle, double timeRatio);
    private native void nativeRelease(long handle);
}
