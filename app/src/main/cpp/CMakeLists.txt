cmake_minimum_required(VERSION 3.10)
project(shizukuyolo)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ncnn：将 prebuilt 库放入 src/main/cpp/ncnn 目录。
# 结构:
#   ncnn/
#     include/ncnn/net.h ...
#     lib/arm64-v8a/libncnn.a
#     lib/armeabi-v7a/libncnn.a
set(NCNN_DIR ${CMAKE_CURRENT_SOURCE_DIR}/ncnn)
set(NCNN_LIB ${NCNN_DIR}/lib/${ANDROID_ABI}/libncnn.a)

add_library(shizukuYolo SHARED
    yolo.cpp
    com_example_kernelsustyleuikit_yolo_YoloNative.cpp
)

target_include_directories(shizukuYolo PRIVATE
    ${NCNN_DIR}/include
)

target_link_libraries(shizukuYolo
    ${NCNN_LIB}
    jnigraphics
    android
    log
    -lz
)

target_compile_options(shizukuYolo PRIVATE -fopenmp)
# ncnn 静态库使用 OpenMP，链接期同样需要，否则会出现未定义引用。
target_link_options(shizukuYolo PRIVATE -fopenmp)
find_package(Threads)
target_link_libraries(shizukuYolo Threads::Threads)