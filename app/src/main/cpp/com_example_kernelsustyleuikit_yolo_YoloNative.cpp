// JNI 实现，对应 Kotlin 的 com.example.kernelsustyleuikit.yolo.YoloNative。
#include <jni.h>
#include <string>

#include "yolo.h"

using namespace yoloaim;

static YoloNcnn gYolo;

extern "C" JNIEXPORT jint JNICALL
Java_com_example_kernelsustyleuikit_yolo_YoloNative_init(
        JNIEnv* env, jobject, jstring paramPath, jstring binPath, jboolean useGpu) {
    const char* param = env->GetStringUTFChars(paramPath, JNI_FALSE);
    const char* bin = env->GetStringUTFChars(binPath, JNI_FALSE);
    int ret = gYolo.load(param, bin, (bool)useGpu);
    env->ReleaseStringUTFChars(paramPath, param);
    env->ReleaseStringUTFChars(binPath, bin);
    return ret;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_kernelsustyleuikit_yolo_YoloNative_detect(
        JNIEnv* env, jobject, jbyteArray rgba, jint w, jint h,
        jfloat conf, jfloat nms) {
    jbyte* bytes = env->GetByteArrayElements(rgba, nullptr);
    std::vector<Object> objs = gYolo.detect(
            reinterpret_cast<const unsigned char*>(bytes), w, h, conf, nms);
    env->ReleaseByteArrayElements(rgba, bytes, JNI_ABORT);

    // 每组 6 个浮点: label, score, cx, cy, w, h
    jsize count = (jsize)(objs.size() * 6);
    jfloatArray arr = env->NewFloatArray(count);
    if (count == 0) return arr;

    std::vector<jfloat> buf(count);
    jsize idx = 0;
    for (const auto& o : objs) {
        buf[idx++] = (jfloat)o.label;
        buf[idx++] = o.score;
        buf[idx++] = o.cx();
        buf[idx++] = o.cy();
        buf[idx++] = o.w;
        buf[idx++] = o.h;
    }
    env->SetFloatArrayRegion(arr, 0, count, buf.data());
    return arr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_kernelsustyleuikit_yolo_YoloNative_destroy(JNIEnv*, jobject) {
    gYolo.destroy();
}