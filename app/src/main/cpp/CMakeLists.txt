# Copyright (c) 2026 Ankit Gothwal. All rights reserved, subject to applicable third-party license terms.
# Project: JARVIS AI | Developer: Ankit Gothwal

cmake_minimum_required(VERSION 3.22.1)
project("jarvis_llama_jni")

# To link full llama.cpp source when Android NDK is installed:
# 1. Clone https://github.com/ggerganov/llama.cpp into app/src/main/cpp/llama.cpp
# 2. Uncomment add_subdirectory(llama.cpp) and target_link_libraries(... llama)
# add_subdirectory(llama.cpp)

add_library(
    jarvis_llama_jni
    SHARED
    jarvis_llama_jni.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    jarvis_llama_jni
    ${log-lib}
    # llama
)
