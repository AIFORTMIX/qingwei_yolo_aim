#ifndef YOLO_H
#define YOLO_H

#include <vector>
#include <string>

// ncnn 头文件（需将 ncnn 放入 src/main/cpp/ncnn/include）
#include "net.h"

namespace yoloaim {

/** 检测框，坐标为原始输入图像像素。 */
struct Object {
    int label;
    float score;
    float x;   // 左上角 x
    float y;   // 左上角 y
    float w;   // 宽
    float h;   // 高
    float cx() const { return x + w / 2.f; }
    float cy() const { return y + h / 2.f; }
};

/**
 * YOLOv8(n) ncnn 封装。
 * 输出张量约定为 [1, (4+NC), 8400]，即每个预测包含 4 坐标 + NC 类概率，
 * 通道第一。缺省 NC=80（COCO）。
 */
class YoloNcnn {
public:
    int load(const char* paramPath, const char* binPath, bool useGpu);
    std::vector<Object> detect(const unsigned char* rgba, int w, int h,
                               float confThresh, float nmsIoU);
    void destroy();

private:
    ncnn::Net net_;
    int nc_ = 80;
    // letterbox 需要的原始尺寸（ncnn Net::extract 后我们手工 decode）
};

} // namespace yoloaim

#endif // YOLO_H