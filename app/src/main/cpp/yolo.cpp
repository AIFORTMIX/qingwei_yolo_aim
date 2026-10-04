#include "yolo.h"

#include "layer.h"

#include <algorithm>
#include <cmath>

namespace yoloaim {

int YoloNcnn::load(const char* paramPath, const char* binPath, bool useGpu) {
    net_.clear();
    net_.opt.use_vulkan_compute = useGpu;
    net_.opt.use_fp16_packed = true;
    net_.opt.use_fp16_storage = true;

    int ret = net_.load_param(paramPath);
    if (ret != 0) return ret;
    ret = net_.load_model(binPath);
    return ret;
}

void YoloNcnn::destroy() {
    net_.clear();
}

static inline float letterbox_ratio(float inW, float inH, int target) {
    return std::min(target / inW, target / inH);
}

/**
 * 前处理：RGBA -> letterbox 后 RGB 输入张量。
 * 返回 letterbox 后图像尺寸 (lw, lh) 与 pad。
 */
static void letterbox_preprocess(const unsigned char* rgba, int w, int h,
                                 ncnn::Mat& out, int target,
                                 float& ratio, float& padX, float& padY) {
    ratio = letterbox_ratio((float)w, (float)h, target);
    int lw = (int)std::round(w * ratio);
    int lh = (int)std::round(h * ratio);
    padX = (target - lw) / 2.f;
    padY = (target - lh) / 2.f;

    out = ncnn::Mat(target, target, 3, (void*)0, (size_t)4, 3);
    ncnn::Mat inRGB(w, h, 3, (void*)0, (size_t)4, 3);
    // RGBA -> RGB
    for (int i = 0; i < w * h; i++) {
        float* rgb = (float*)inRGB.data + i * 3;
        const unsigned char* p = rgba + i * 4;
        rgb[0] = p[0];
        rgb[1] = p[1];
        rgb[2] = p[2];
    }
    out.fill(114.f); // letterbox 灰边
    // 重采样缩放（简单最近邻，实际项目建议 opengl/双线性）
    float* dst = (float*)out.data;
    for (int y = 0; y < std::max(target, 1); y++) {
        int sy = (int)((y - padY) / ratio);
        if (sy < 0 || sy >= h) continue;
        for (int x = 0; x < target; x++) {
            int sx = (int)((x - padX) / ratio);
            if (sx < 0 || sx >= w) continue;
            const float* srcP = (float*)inRGB.data + (sy * w + sx) * 3;
            float* dstP = dst + (y * target + x) * 3;
            dstP[0] = srcP[0];
            dstP[1] = srcP[1];
            dstP[2] = srcP[2];
        }
    }
}

static float iou(const Object& a, const Object& b) {
    float x1 = std::max(a.x, b.x);
    float y1 = std::max(a.y, b.y);
    float x2 = std::min(a.x + a.w, b.x + b.w);
    float y2 = std::min(a.y + a.h, b.y + b.h);
    float inter = std::max(0.f, x2 - x1) * std::max(0.f, y2 - y1);
    float areaA = a.w * a.h;
    float areaB = b.w * b.h;
    return inter / (areaA + areaB - inter + 1e-6f);
}

std::vector<Object> YoloNcnn::detect(const unsigned char* rgba, int w, int h,
                                     float confThresh, float nmsIoU) {
    const int target = 640;
    float ratio, padX, padY;
    ncnn::Mat in;
    letterbox_preprocess(rgba, w, h, in, target, ratio, padX, padY);

    ncnn::Extractor ex = net_.create_extractor();
    ex.input("images", in);

    // YOLOv8 通常只有一个输出节点，名称为配置导出时决定。
    ncnn::Mat out;
    ex.extract("output0", out); // 按实际 .param 中输出名调整

    // out 形状：C = 4+nc，S = 8400（或更大）
    int C = out.c;
    int S = out.w * out.h;
    int nc = C - 4;
    nc_ = nc;

    std::vector<Object> cand;
    cand.reserve(512);
    const float* ptr = out.channel(0); // chw 布局

    for (int s = 0; s < S; s++) {
        // 该预测的类概率最大值
        float bestScore = 0.f;
        int bestCls = -1;
        for (int c = 4; c < C; c++) {
            float p = ptr[c * S + s];
            if (p > bestScore) {
                bestScore = p;
                bestCls = c - 4;
            }
        }
        if (bestScore < confThresh || bestCls < 0) continue;

        float x_c = ptr[0 * S + s];
        float y_c = ptr[1 * S + s];
        float bw = ptr[2 * S + s];
        float bh = ptr[3 * S + s];

        Object o;
        o.label = bestCls;
        o.score = bestScore;
        // letterbox 归位到原图
        o.x = ((x_c - padX) / ratio);
        o.y = ((y_c - padY) / ratio);
        o.w = (bw / ratio);
        o.h = (bh / ratio);
        // 坐标可能是中心+宽高 → 转左上角
        float left = o.x - o.w * 0.5f;
        float top = o.y - o.h * 0.5f;
        o.x = left;
        o.y = top;
        cand.push_back(o);
    }

    // NMS
    std::sort(cand.begin(), cand.end(), [](const Object& a, const Object& b) {
        return a.score > b.score;
    });
    std::vector<Object> keep;
    std::vector<bool> removed(cand.size(), false);
    for (size_t i = 0; i < cand.size(); i++) {
        if (removed[i]) continue;
        keep.push_back(cand[i]);
        for (size_t j = i + 1; j < cand.size(); j++) {
            if (removed[j]) continue;
            if (cand[i].label == cand[j].label && iou(cand[i], cand[j]) > nmsIoU) {
                removed[j] = true;
            }
        }
    }
    return keep;
}

} // namespace yoloaim