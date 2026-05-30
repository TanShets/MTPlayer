cmake_minimum_required(VERSION 3.22.1)

project("mtplayer")

# --- RubberBand ---
add_library(rubberband-jni SHARED
    RubberBandStretcherJNI.cpp
    # Use the single-unit implementation for easier compilation
    rubberband/single/RubberBandSingle.cpp
)

target_include_directories(rubberband-jni PRIVATE
    .
    rubberband
)

find_library(log-lib log)

target_link_libraries(rubberband-jni
    ${log-lib}
    android
)

# --- SoundTouch ---
# Enable float samples for better quality
set(INTEGER_SAMPLES OFF CACHE BOOL "" FORCE)
set(SOUNDSTRETCH OFF CACHE BOOL "" FORCE)
set(SOUNDTOUCH_DLL OFF CACHE BOOL "" FORCE)

add_subdirectory(soundtouch)

add_library(soundtouch-jni SHARED
    SoundTouchJNI.cpp
)

target_include_directories(soundtouch-jni PRIVATE
    .
    soundtouch/include
)

target_link_libraries(soundtouch-jni
    SoundTouch
    ${log-lib}
    android
)
