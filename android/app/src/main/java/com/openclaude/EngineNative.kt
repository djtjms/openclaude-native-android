package com.openclaude

/**
 * JNI bridge into the bundled native engine.
 *
 * The native library (`libocengine.so`, built from src/main/cpp) currently
 * exposes a single info function used to prove the NDK/JNI toolchain works
 * end-to-end and to surface an engine version string in the UI status chip.
 */
object EngineNative {
    init {
        System.loadLibrary("ocengine")
    }

    external fun nativeEngineInfo(): String
}