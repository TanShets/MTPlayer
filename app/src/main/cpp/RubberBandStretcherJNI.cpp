#include <jni.h>
#include <vector>
#include <android/log.h>
#include "rubberband/RubberBandStretcher.h"

#define LOG_TAG "RubberBandJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace RubberBand;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeInit(JNIEnv *env, jobject thiz,
                                                              jint sample_rate, jint channels,
                                                              jdouble time_ratio,
                                                              jdouble pitch_scale) {
    try {
        // OptionProcessRealTime: minimize latency
        // OptionPitchHighConsistency: natural sound
        RubberBandStretcher *stretcher = new RubberBandStretcher(
                sample_rate, channels,
                RubberBandStretcher::OptionProcessRealTime | RubberBandStretcher::OptionPitchHighConsistency);

        stretcher->setTimeRatio(time_ratio);
        stretcher->setPitchScale(pitch_scale);

        LOGD("Initialized stretcher: %dHz, %d channels", sample_rate, channels);
        return reinterpret_cast<jlong>(stretcher);
    } catch (const std::exception &e) {
        LOGE("Failed to initialize RubberBandStretcher: %s", e.what());
        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeProcess(JNIEnv *env, jobject thiz,
                                                                 jlong handle, jfloatArray input,
                                                                 jint length,
                                                                 jboolean is_final) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (!stretcher) return;

    size_t channels = stretcher->getChannelCount();
    if (channels == 0) return;

    size_t samples = static_cast<size_t>(length) / channels;

    if (samples > 0) {
        jfloat *data = env->GetFloatArrayElements(input, nullptr);
        if (!data) return;

        // De-interleave
        std::vector<float *> buffers(channels);
        std::vector<std::vector<float>> channelData(channels, std::vector<float>(samples));

        for (size_t c = 0; c < channels; ++c) {
            for (size_t s = 0; s < samples; ++s) {
                channelData[c][s] = data[s * channels + c];
            }
            buffers[c] = channelData[c].data();
        }
        stretcher->process(buffers.data(), samples, is_final);
        env->ReleaseFloatArrayElements(input, data, JNI_ABORT);
    } else if (is_final) {
        // Safe EOS flush using valid pointers to avoid SIGSEGV in some versions
        std::vector<float *> pointers(channels, nullptr);
        stretcher->process(pointers.data(), 0, true);
        LOGD("nativeProcess: Sent EOS flush (is_final=true)");
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeAvailable(JNIEnv *env, jobject thiz,
                                                                   jlong handle) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (!stretcher) return 0;
    return static_cast<jint>(stretcher->available());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeRetrieve(JNIEnv *env, jobject thiz,
                                                                  jlong handle,
                                                                  jfloatArray output) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (!stretcher) return 0;

    size_t available = stretcher->available();
    if (available == 0) return 0;

    jsize outLen = env->GetArrayLength(output);
    size_t channels = stretcher->getChannelCount();
    if (channels == 0) return 0;

    size_t samplesToRetrieve = outLen / channels;
    if (samplesToRetrieve > available) samplesToRetrieve = available;

    if (samplesToRetrieve == 0) return 0;

    jfloat *data = env->GetFloatArrayElements(output, nullptr);
    if (!data) {
        LOGE("nativeRetrieve: Failed to get float array elements");
        return 0;
    }

    // De-interleave style retrieval
    std::vector<float *> buffers(channels);
    std::vector<std::vector<float>> channelData(channels, std::vector<float>(samplesToRetrieve));
    for (size_t c = 0; c < channels; ++c) {
        buffers[c] = channelData[c].data();
    }

    size_t retrieved = stretcher->retrieve(buffers.data(), samplesToRetrieve);

    if (retrieved > 0) {
        // Interleave back
        for (size_t c = 0; c < channels; ++c) {
            for (size_t s = 0; s < retrieved; ++s) {
                data[s * channels + c] = channelData[c][s];
            }
        }
    }

    env->ReleaseFloatArrayElements(output, data, 0);
    return static_cast<jint>(retrieved);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeSetPitchScale(JNIEnv *env, jobject thiz,
                                                                       jlong handle,
                                                                       jdouble pitch_scale) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (stretcher && pitch_scale > 0) {
        stretcher->setPitchScale(pitch_scale);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeSetTimeRatio(JNIEnv *env, jobject thiz,
                                                                      jlong handle,
                                                                      jdouble time_ratio) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (stretcher && time_ratio > 0) {
        stretcher->setTimeRatio(time_ratio);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeReset(JNIEnv *env, jobject thiz,
                                                               jlong handle) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (stretcher) stretcher->reset();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeGetLatency(JNIEnv *env, jobject thiz,
                                                                    jlong handle) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (!stretcher) return 0;
    return static_cast<jint>(stretcher->getStartDelay());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_RubberBandStretcher_nativeRelease(JNIEnv *env, jobject thiz,
                                                                 jlong handle) {
    RubberBandStretcher *stretcher = reinterpret_cast<RubberBandStretcher *>(handle);
    if (stretcher) {
        delete stretcher;
        LOGD("Released stretcher");
    }
}
