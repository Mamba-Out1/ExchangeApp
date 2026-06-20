package com.example.exchangeapp.data.repository

import com.example.exchangeapp.data.remote.api.OpenAIApiService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.element
import io.kotest.property.checkAll
import io.mockk.mockk

/**
 * Feature: campus-exchange-app
 * Property 9: JSON解析错误鲁棒性 (JSON parsing error robustness)
 *
 * For any malformed JSON string (missing required fields, wrong types, or invalid
 * syntax), the parsing function SHALL return a Result.failure with a descriptive
 * error message and SHALL NOT throw uncaught exceptions.
 *
 * Validates: Requirements 12.3
 *
 * 被测函数为 AIRepositoryImpl.parseRecognitionResult,它负责将 OpenAI API 返回的
 * 文本内容解析为 ItemRecognitionResult。
 */
class AIRepositoryJsonParsingPropertyTest : StringSpec({

    // parseRecognitionResult 不依赖 apiService / apiKey,这里用 relaxed mock 即可。
    val repository = AIRepositoryImpl(
        apiService = mockk<OpenAIApiService>(relaxed = true),
        apiKey = "test-api-key"
    )

    // Property 9: JSON parsing error robustness
    // Validates: Requirements 12.3
    "parsing malformed JSON returns failure with a descriptive message and never throws" {
        checkAll(200, Arb.malformedRecognitionJson()) { malformedJson ->
            // parseRecognitionResult 必须不抛出未捕获异常。如果它抛出,checkAll 会失败并报告反例。
            val result = repository.parseRecognitionResult(malformedJson)

            // 对于畸形 JSON,必须返回 Result.failure
            result.isFailure shouldBe true

            // 失败必须携带一个非空、描述性的错误信息
            val exception = result.exceptionOrNull()
            exception shouldNotBe null
            exception!!.message shouldNotBe null
            exception.message!!.isNotBlank() shouldBe true
        }
    }

    // 具体示例(单元测试),覆盖每一类畸形输入,作为属性测试的补充。
    "missing required fields produce failure" {
        repository.parseRecognitionResult("""{"name": "test"}""").isFailure shouldBe true
    }

    "wrong top-level type produces failure" {
        repository.parseRecognitionResult("""["a", "b", "c"]""").isFailure shouldBe true
    }

    "invalid syntax produces failure" {
        repository.parseRecognitionResult("""{"name": "test", """).isFailure shouldBe true
    }

    "empty string produces failure" {
        repository.parseRecognitionResult("").isFailure shouldBe true
    }

    "valid JSON failing semantic validation produces failure (non-positive price)" {
        val json = """{"name":"书","description":"一本好书","estimatedPrice":0,"tags":["书籍"]}"""
        repository.parseRecognitionResult(json).isFailure shouldBe true
    }
})

/**
 * 生成保证为"畸形"的 JSON 字符串,覆盖三类问题:
 * 1. 缺少必需字段
 * 2. 字段类型错误 / null 值
 * 3. 无效语法 / 错误的顶层结构
 *
 * 每个样本对于 ItemRecognitionResult 而言都必然导致解析或语义校验失败。
 */
private fun Arb.Companion.malformedRecognitionJson() = arbitrary {
    val malformedSamples = listOf(
        // --- 缺少必需字段 ---
        """{"name": "test"}""",
        """{"description": "缺少name"}""",
        """{"name": "test", "description": "缺少价格和标签"}""",
        """{"estimatedPrice": 100.0, "tags": ["书籍"]}""",
        """{}""",

        // --- 字段类型错误 / null 值 ---
        """{"name": null, "description": "d", "estimatedPrice": 10.0, "tags": ["书籍"]}""",
        """{"name": "n", "description": null, "estimatedPrice": 10.0, "tags": ["书籍"]}""",
        """{"name": "n", "description": "d", "estimatedPrice": "不是数字", "tags": ["书籍"]}""",
        """{"name": "n", "description": "d", "estimatedPrice": 10.0, "tags": "不是数组"}""",
        """{"name": "n", "description": "d", "estimatedPrice": null, "tags": ["书籍"]}""",

        // --- 语义无效(空字符串 / 非正价格 / 空标签) ---
        """{"name": "", "description": "d", "estimatedPrice": 10.0, "tags": ["书籍"]}""",
        """{"name": "n", "description": "", "estimatedPrice": 10.0, "tags": ["书籍"]}""",
        """{"name": "n", "description": "d", "estimatedPrice": -5.0, "tags": ["书籍"]}""",
        """{"name": "n", "description": "d", "estimatedPrice": 0.0, "tags": ["书籍"]}""",
        """{"name": "n", "description": "d", "estimatedPrice": 10.0, "tags": []}""",

        // --- 无效语法 / 错误的顶层结构 ---
        """{""",
        """{"name": "test", """,
        """name: test""",
        """["this", "is", "an", "array"]""",
        """"just a string"""",
        """123""",
        """not json at all"""
    )
    Arb.element(malformedSamples).bind()
}
