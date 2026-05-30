#include <jni.h>
#include <android/log.h>

// Use float samples for better quality
#define SOUNDTOUCH_FLOAT_SAMPLES 1

#include "SoundTouch.h"

#define LOG_TAG "SoundTouchJNI"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace soundtouch;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeInit(JNIEnv *env, jobject thiz) {
    SoundTouch *st = new SoundTouch();
    // Enable Anti-Aliasing filter by default for better quality
    st->setSetting(SETTING_USE_AA_FILTER, 1);
    return reinterpret_cast<jlong>(st);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeRelease(JNIEnv *env, jobject thiz, jlong handle) {
    delete reinterpret_cast<SoundTouch *>(handle);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeSetPitch(JNIEnv *env, jobject thiz, jlong handle, jfloat pitch) {
    if (handle != 0) {
        reinterpret_cast<SoundTouch *>(handle)->setPitch(pitch);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeSetPitchSemiTones(JNIEnv *env, jobject thiz, jlong handle, jfloat semitones) {
    if (handle != 0) {
        reinterpret_cast<SoundTouch *>(handle)->setPitchSemiTones(semitones);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeSetTempo(JNIEnv *env, jobject thiz, jlong handle, jfloat tempo) {
    if (handle != 0) {
        reinterpret_cast<SoundTouch *>(handle)->setTempo(tempo);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeSetSampleRate(JNIEnv *env, jobject thiz, jlong handle, jint sample_rate) {
    if (handle != 0) {
        reinterpret_cast<SoundTouch *>(handle)->setSampleRate(sample_rate);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeSetChannels(JNIEnv *env, jobject thiz, jlong handle, jint channels) {
    if (handle != 0) {
        reinterpret_cast<SoundTouch *>(handle)->setChannels(channels);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativePutSamples(JNIEnv *env, jobject thiz, jlong handle, jfloatArray samples, jint num_frames) {
    if (handle != 0 && samples != nullptr) {
        jfloat *ptr = env->GetFloatArrayElements(samples, nullptr);
        if (ptr != nullptr) {
            reinterpret_cast<SoundTouch *>(handle)->putSamples(ptr, num_frames);
            env->ReleaseFloatArrayElements(samples, ptr, JNI_ABORT);
        }
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeReceiveSamples(JNIEnv *env, jobject thiz, jlong handle, jfloatArray out_buffer, jint max_frames) {
    if (handle != 0 && out_buffer != nullptr) {
        jfloat *ptr = env->GetFloatArrayElements(out_buffer, nullptr);
        if (ptr != nullptr) {
            unsigned int received = reinterpret_cast<SoundTouch *>(handle)->receiveSamples(ptr, max_frames);
            env->ReleaseFloatArrayElements(out_buffer, ptr, 0);
            return static_cast<jint>(received);
        }
    }
    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeNumSamples(JNIEnv *env, jobject thiz, jlong handle) {
    if (handle != 0) {
        return static_cast<jint>(reinterpret_cast<SoundTouch *>(handle)->numSamples());
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeFlush(JNIEnv *env, jobject thiz, jlong handle) {
    if (handle != 0) {
        reinterpret_cast<SoundTouch *>(handle)->flush();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_mtplayer_audio_SoundTouch_nativeSetSetting(JNIEnv *env, jobject thiz, jlong handle, jint setting_id, jint value) {
    if (handle != 0) {
        reinterpret_cast<SoundTouch *>(handle)->setSetting(setting_id, value);
    }
}
