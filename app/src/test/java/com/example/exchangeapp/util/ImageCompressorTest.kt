package com.example.exchangeapp.util

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [ImageCompressor]单元测试。
 *
 * 在JVM单元测试中Android图形API返回默认值（无法真正解码位图），
 * 因此这里聚焦于不依赖真实解码的安全路径：空输入与无法解码的输入应回退到原始字节，
 * 保证压缩不会劣化或丢失数据。
 */
class ImageCompressorTest {

    @Test
    fun `empty input returns empty bytes`() {
        val empty = ByteArray(0)
        val result = ImageCompressor.compress(empty)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `undecodable input falls back to original bytes`() {
        // 非图片字节无法被解码（边界尺寸为0），应原样返回
        val garbage = "not-an-image".toByteArray()
        val result = ImageCompressor.compress(garbage)
        assertArrayEquals(garbage, result)
    }

    @Test
    fun `default quality and dimension are within valid ranges`() {
        assertTrue(ImageCompressor.DEFAULT_QUALITY in 1..100)
        assertTrue(ImageCompressor.DEFAULT_MAX_DIMENSION > 0)
    }
}
