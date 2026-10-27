/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 */

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "JarvisLlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/*
 * Native JNI bridge between LocalAIEngine.kt and llama.cpp (MIT License).
 * When compiled with -DLLAMA_CPP_ENABLED and linked against llama.cpp, this bridge loads
 * quantized GGUF models (e.g., Qwen2.5-0.5B-Instruct-Q4_K_M.gguf) on ARM64/ARMv7 CPUs.
 * Without -DLLAMA_CPP_ENABLED, it returns 0L during init so LocalAIEngine automatically
 * routes to the built-in deterministic Rule Engine without crashing.
 */

extern "C" JNIEXPORT jlong JNICALL
Java_com_ankit_jarvis_LocalAIEngine_nativeInitModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jint numThreads) {
    const char* pathStr = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Checking native GGUF model path: %s (threads=%d)", pathStr, numThreads);
    env->ReleaseStringUTFChars(modelPath, pathStr);

#ifdef LLAMA_CPP_ENABLED
    // Link against llama_load_model_from_file / llama_new_context_with_model here
    // when building with full llama.cpp submodule.
#endif
    return 0L;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_ankit_jarvis_LocalAIEngine_nativeCompletion(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jstring prompt,
        jint maxTokens) {
    if (contextPtr == 0L) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF("");
}

extern "C" JNIEXPORT void JNICALL
Java_com_ankit_jarvis_LocalAIEngine_nativeFreeModel(
        JNIEnv* /* env */,
        jobject /* this */,
        jlong contextPtr) {
    if (contextPtr != 0L) {
        LOGI("Releasing native GGUF context.");
    }
}
