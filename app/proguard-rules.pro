-keepclassmembers class com.example.shizukuyoloaim.yolo.YoloNative {
    native <methods>;
}
# ncnn / onnx JNI 保持
-keep class org.opencv.** { *; }