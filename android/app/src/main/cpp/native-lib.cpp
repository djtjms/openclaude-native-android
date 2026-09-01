#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_openclaude_EngineNative_nativeEngineInfo(JNIEnv *env, jobject /* this */) {
    std::string info = "openclaude-native-android v0.1.0 (JNI engine bridge ready)";
    return env->NewStringUTF(info.c_str());
}