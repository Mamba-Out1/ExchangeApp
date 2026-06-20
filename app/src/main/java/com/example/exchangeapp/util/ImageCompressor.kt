package com.example.exchangeapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * 图片压缩工具。
 *
 * 在物品图片上传前对图片进行降采样与JPEG质量压缩，减小Base64编码后的体积，
 * 从而降低本地存储占用与AI识别接口的传输成本 (Requirements 6.2, 6.3)。
 *
 * 该工具仅依赖Android图形API，对外暴露纯字节数组的输入/输出，便于复用与测试。
 */
object ImageCompressor {

    /** 压缩后图片的最大边长（像素）。 */
    const val DEFAULT_MAX_DIMENSION = 1024

    /** JPEG压缩质量 (0-100)。 */
    const val DEFAULT_QUALITY = 80

    /**
     * 压缩图片字节。
     *
     * 步骤:
     * 1. 仅读取图片边界以计算降采样比例，避免一次性解码大图导致OOM。
     * 2. 按[maxDimension]降采样解码，再等比缩放到目标边长以内。
     * 3. 以JPEG格式按[quality]压缩输出。
     *
     * 若解码失败或压缩后体积反而更大，则返回原始字节，保证不会劣化结果。
     *
     * @param imageBytes 原始图片字节。
     * @param maxDimension 压缩后最长边的像素上限。
     * @param quality JPEG压缩质量 (1-100)。
     * @return 压缩后的图片字节；无法压缩时返回原始字节。
     */
    fun compress(
        imageBytes: ByteArray,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        quality: Int = DEFAULT_QUALITY
    ): ByteArray {
        if (imageBytes.isEmpty()) return imageBytes

        // 1. 读取边界尺寸
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOptions)
        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return imageBytes

        // 2. 降采样解码
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(srcWidth, srcHeight, maxDimension)
        }
        val decoded = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions)
            ?: return imageBytes

        // 3. 等比缩放至目标边长以内
        val scaled = scaleToMaxDimension(decoded, maxDimension)

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), output)

        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()

        val result = output.toByteArray()
        // 压缩后若为空或反而更大，回退到原始字节
        return if (result.isNotEmpty() && result.size < imageBytes.size) result else imageBytes
    }

    /**
     * 计算[BitmapFactory.Options.inSampleSize]，使解码后尺寸尽量接近但不小于目标边长。
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val largestSide = maxOf(width, height)
        if (largestSide > maxDimension) {
            var halfSide = largestSide / 2
            while (halfSide / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * 将位图等比缩放到最长边不超过[maxDimension]。若已满足条件则原样返回。
     */
    private fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largestSide = maxOf(width, height)
        if (largestSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / largestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
