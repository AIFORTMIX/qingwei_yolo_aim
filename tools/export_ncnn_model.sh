#!/usr/bin/env bash
#
# 将官方预训练 yolov8n 导出为 ncnn 格式，并部署到 app/src/main/assets/models/。
# 3D-FPS 人物识别首选此零数据方案（COCO person 类，targetClass=0）。
#
# 使用方法（在仓库根目录执行）：
#   bash tools/export_ncnn_model.sh [yolov8n|yolov8s|...] [imgsz]
#   默认: yolov8n , 640
#
set -euo pipefail

MODEL="${1:-yolov8n}"
IMGSZ="${2:-640}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS_DIR="$ROOT_DIR/app/src/main/assets/models"
OUT_PARAM="$ASSETS_DIR/yolov8n.param"
OUT_BIN="$ASSETS_DIR/yolov8n.bin"

echo "==> 目标模型: ${MODEL}.pt , 输入尺寸: ${IMGSZ}"

# 已存在则由调用方决定是否跳过（本地可用；CI 每次干净仓库会重新生成）
if [[ -f "$OUT_PARAM" && -f "$OUT_BIN" ]]; then
  echo "==> 已检测到模型: $OUT_PARAM / $OUT_BIN"
  ls -la "$OUT_PARAM" "$OUT_BIN"
  exit 0
fi

mkdir -p "$ASSETS_DIR"

echo "==> 安装依赖 (ultralytics / pnnx)"
python3 -m pip install -q --upgrade ultralytics pnnx

echo "==> 导出 ncnn 模型 (ultralytics 内置 pnnx 转换，保留原始 YOLOv8 输出头)"
export PYTHONWARNINGS=ignore
yolo export model="${MODEL}.pt" format=ncnn imgsz="${IMGSZ}" || {
  echo "::error::ultralytics ncnn 导出失败，请检查网络或在本地转换后放入 assets/models"
  exit 1
}

# 产物目录形如: yolov8n_ncnn_model/yolov8n.param 与 .bin
NCNN_DIR=$(find "$ROOT_DIR" -maxdepth 2 -type d -name "${MODEL}_ncnn_model" -print -quit)
if [[ -z "$NCNN_DIR" ]]; then
  NCNN_DIR=$(find "$ROOT_DIR" -maxdepth 2 -type d -name '*_ncnn_model' -print -quit)
fi
if [[ -z "$NCNN_DIR" ]]; then
  echo "::error::未找到导出的 ncnn 模型目录"
  find "$ROOT_DIR" -maxdepth 2 -type d -name '*ncnn*' -print
  exit 1
fi

echo "==> 产物目录: $NCNN_DIR"
# ultralytics 通过 pnnx 导出的产物固定命名为 model.ncnn.param / model.ncnn.bin
SRC_PARAM="$NCNN_DIR/model.ncnn.param"
SRC_BIN="$NCNN_DIR/model.ncnn.bin"
if [[ ! -f "$SRC_PARAM" ]]; then
  # 兼容旧版本 ultralytics 可能输出 ${MODEL}.param
  SRC_PARAM="$NCNN_DIR/${MODEL}.param"
  SRC_BIN="$NCNN_DIR/${MODEL}.bin"
fi
if [[ ! -f "$SRC_PARAM" ]]; then
  echo "::error::未找到 .param 文件"
  ls -la "$NCNN_DIR"
  exit 1
fi
cp "$SRC_PARAM" "$OUT_PARAM"
cp "$SRC_BIN"   "$OUT_BIN"

echo "==> 校验产出 (输入输出 blob 名，供参考)："
grep -E '^Input|[0-9]+ [0-9]+$' "$OUT_PARAM" | tail -n 3 || true

ls -la "$OUT_PARAM" "$OUT_BIN"
echo "==> 完成。APK 构建时会把这两个文件打进 assets/models/。"

# 清理 ultralytics 生成的临时模型目录
rm -rf "$ROOT_DIR/${MODEL}_ncnn_model" || true