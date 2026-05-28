cmake_minimum_required(VERSION 3.22.1)

project("mtplayer")

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
