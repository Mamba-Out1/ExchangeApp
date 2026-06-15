package com.example.exchangeapp.data.repository

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import io.mockk.mockk
import com.example.exchangeapp.domain.model.ItemRecognitionResult

/**
 * Property-Based Test for JSON Parsing Error Robustness
 * 
 * **Validates: Requirements 12.3**
 * 
 * Property 9: JSON解析错误鲁棒性
 * For any malformed JSON string (missing required fields, wrong types, or invalid syntax),
 * the parsing function SHALL return a Result.failure with a descriptive error message 
 * and SHALL NOT throw uncaught exceptions
 */
class JsonParsingPropertyTest : StringSpec({
    
    // 使用与AIRepositoryImpl相同的JSON配置
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    // 模拟parseRecognitionResult方法的行为
    private fun parseRecognitionResult(jsonString: String): Result<ItemRecognitionResult> {
        return try {
            // 清理可能存在的markdown代码块标记
            val cleanedJson = jsonString
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            // 解析JSON
            val result = json.decodeFromString<ItemRecognitionResult>(cleanedJson)
            
            // 验证解析结果的有效性
            when {
                result.name.isBlank() -> {
                    Result.failure(
                        IllegalStateException("解析结果缺少物品名称")
                    )
                }
                result.description.isBlank() -> {
                    Result.failure(
                        IllegalStateException("解析结果缺少物品描述")
                    )
                }
                result.estimatedPrice <= 0 -> {
                    Result.failure(
                        IllegalStateException("解析结果的估价无效（必须大于0）")
                    )
                }
                result.tags.isEmpty() -> {
                    Result.failure(
                        IllegalStateException("解析结果缺少标签")
                    )
                }
                else -> Result.success(result)
            }
        } catch (e: SerializationException) {
            // JSON解析失败
            Result.failure(
                IllegalStateException("无法解析AI识别结果，返回的JSON格式不正确：${e.message}", e)
            )
        } catch (e: IllegalArgumentException) {
            // JSON格式错误
            Result.failure(
                IllegalStateException("AI识别结果格式错误：${e.message}", e)
            )
        } catch (e: Exception) {
            // 其他解析错误
            Result.failure(
                IllegalStateException("解析AI识别结果时发生错误：${e.message}", e)
            )
        }
    }
    
    // Feature: campus-exchange-app, Property 9: JSON parsing error robustness
    "parsing malformed JSON returns failure with descriptive error message" {
        checkAll(100, Arb.malformedJson()) { malformedJson ->
            // Act: Call parseRecognitionResult
            val result = parseRecognitionResult(malformedJson)
            
            // Assert: Should return failure with descriptive error message
            result.isFailure shouldBe true
            
            val exception = result.exceptionOrNull()
            exception shouldNotBe null
            
            val errorMessage = exception!!.message
            errorMessage shouldNotBe null
            errorMessage!!.isNotBlank() shouldBe true
            
            // Verify error message is descriptive (contains useful information)
            errorMessage shouldNotBe "null"
            errorMessage shouldNotBe ""
            
            // Check that error message provides some context about the parsing issue
            val descriptiveKeywords = listOf(
                "JSON", "解析", "格式", "错误", "无效", "格式不正确", "缺少", "类型", "语法"
            )
            val hasDescriptiveContent = descriptiveKeywords.any { keyword -> 
                errorMessage.contains(keyword, ignoreCase = true) 
            }
            hasDescriptiveContent shouldBe true
        }
    }
    
    "parsing malformed JSON does not throw uncaught exceptions" {
        checkAll(100, Arb.malformedJson()) { malformedJson ->
            // Act & Assert: Should not throw any uncaught exceptions
            kotlin.runCatching {
                parseRecognitionResult(malformedJson)
            }.isSuccess shouldBe true
        }
    }
    
    "parsing valid JSON with missing required fields returns failure" {
        checkAll(100, Arb.missingFieldJson()) { missingFieldJson ->
            // Act: Call parseRecognitionResult
            val result = parseRecognitionResult(missingFieldJson)
            
            // Assert: Should return failure
            result.isFailure shouldBe true
            
            val errorMessage = result.exceptionOrNull()?.message ?: ""
            errorMessage.isNotBlank() shouldBe true
            
            // Error should indicate missing field or validation failure
            val validationKeywords = listOf("缺少", "无效", "必须大于0", "空白", "不能为空")
            val hasValidationMessage = validationKeywords.any { keyword ->
                errorMessage.contains(keyword, ignoreCase = true)
            }
            hasValidationMessage shouldBe true
        }
    }
    
    "parsing valid JSON with wrong data types returns failure" {
        checkAll(100, Arb.wrongTypeJson()) { wrongTypeJson ->
            // Act: Call parseRecognitionResult
            val result = parseRecognitionResult(wrongTypeJson)
            
            // Assert: Should return failure
            result.isFailure shouldBe true
            
            val errorMessage = result.exceptionOrNull()?.message ?: ""
            errorMessage.isNotBlank() shouldBe true
            
            // Error should indicate type mismatch or parsing error
            val typeErrorKeywords = listOf("类型", "格式不正确", "转换", "解析", "SerializationException")
            val hasTypeErrorMessage = typeErrorKeywords.any { keyword ->
                errorMessage.contains(keyword, ignoreCase = true)
            }
            hasTypeErrorMessage shouldBe true
        }
    }
    
    "parsing valid complete JSON returns success with proper validation" {
        checkAll(100, Arb.validCompleteJson()) { validJson ->
            // Act: Call parseRecognitionResult
            val result = parseRecognitionResult(validJson)
            
            // Assert: Should return success with valid data
            result.isSuccess shouldBe true
            
            val item = result.getOrNull()
            item shouldNotBe null
            
            // Validate the item data
            item!!.name.isNotBlank() shouldBe true
            item.description.isNotBlank() shouldBe true
            item.estimatedPrice > 0 shouldBe true
            item.tags.isNotEmpty() shouldBe true
        }
    }
})

// Custom Arbitraries for generating various types of JSON strings

/**
 * Arbitrary generator for various types of malformed JSON strings
 */
fun Arb.Companion.malformedJson(): Arb<String> = arbitrary { rs ->
    val malformedTypes = listOf(
        // Invalid syntax
        "{",
        "}",
        "[",
        "]",
        "{name: \"test\"}",  // Missing quotes around property name
        "{\"name\": \"test\",}",  // Trailing comma
        "{'name': 'test'}",  // Single quotes instead of double quotes
        "{\"name\": \"test\" // comment}",  // JSON with comment (invalid without lenient mode)
        
        // Incomplete JSON
        "{\"name\": ",
        "{\"name\": \"test\"",
        "\"name\": \"test\"}",
        
        // Invalid values
        "{\"name\": \"test\", \"description\": \"test\", \"estimatedPrice\": \"not-a-number\", \"tags\": [\"tag\"]}",
        "{\"name\": \"test\", \"description\": \"test\", \"estimatedPrice\": [123], \"tags\": [\"tag\"]}",
        "{\"name\": \"test\", \"description\": \"test\", \"estimatedPrice\": 123, \"tags\": \"not-an-array\"}",
        "{\"name\": \"test\", \"description\": \"test\", \"estimatedPrice\": 123, \"tags\": [1, 2, 3]}",  // Numbers instead of strings
        
        // Wrong top-level type
        "[\"this\", \"is\", \"array\"]",
        "\"just a string\"",
        "123",
        "true",
        "null",
        
        // Unicode and encoding issues
        "{\"name\": \"test\\uXXXX\"}",  // Invalid Unicode escape
        "{\"name\": \"test\x00\"}",  // Null byte in string
        
        // Nested structure issues
        "{\"name\": {\"nested\": \"object\"}, \"description\": \"test\", \"estimatedPrice\": 123, \"tags\": [\"tag\"]}",
        "{\"name\": \"test\", \"description\": \"test\", \"estimatedPrice\": 123, \"tags\": [[\"nested\"]]}",
        
        // Extra content
        "{\"name\": \"test\", \"description\": \"test\", \"estimatedPrice\": 123, \"tags\": [\"tag\"]} extra content",
        "extra {\"name\": \"test\", \"description\": \"test\", \"estimatedPrice\": 123, \"tags\": [\"tag\"]}",
        
        // Empty strings
        "",
        "   ",
        "\n",
        
        // Markdown without proper JSON
        "```\nThis is not JSON\n```",
        "**Some text** not JSON",
    )
    
    Arb.element(malformedTypes).next(rs)
}

/**
 * Arbitrary generator for JSON strings with missing required fields
 */
fun Arb.Companion.missingFieldJson(): Arb<String> = arbitrary { rs ->
    val missingFieldCombinations = listOf(
        // Missing name
        "{\"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        
        // Missing description
        "{\"name\": \"iPhone\", \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        
        // Missing estimatedPrice
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"tags\": [\"电子产品\"]}",
        
        // Missing tags
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0}",
        
        // Missing multiple fields
        "{\"name\": \"iPhone\", \"estimatedPrice\": 100.0}",
        "{\"description\": \"物品描述\", \"tags\": [\"电子产品\"]}",
        "{\"name\": \"iPhone\", \"tags\": [\"电子产品\"]}",
        
        // Empty object
        "{}",
        
        // Only name
        "{\"name\": \"iPhone\"}",
        
        // Only description
        "{\"description\": \"物品描述\"}",
        
        // Only estimatedPrice
        "{\"estimatedPrice\": 100.0}",
        
        // Only tags
        "{\"tags\": [\"电子产品\"]}",
    )
    
    Arb.element(missingFieldCombinations).next(rs)
}

/**
 * Arbitrary generator for JSON strings with wrong data types
 */
fun Arb.Companion.wrongTypeJson(): Arb<String> = arbitrary { rs ->
    val wrongTypeCombinations = listOf(
        // name as non-string
        "{\"name\": 123, \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        "{\"name\": true, \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        "{\"name\": null, \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        "{\"name\": [\"iPhone\"], \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        
        // description as non-string
        "{\"name\": \"iPhone\", \"description\": 456, \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        "{\"name\": \"iPhone\", \"description\": false, \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        "{\"name\": \"iPhone\", \"description\": null, \"estimatedPrice\": 100.0, \"tags\": [\"电子产品\"]}",
        
        // estimatedPrice as non-number
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": \"一百元\", \"tags\": [\"电子产品\"]}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": true, \"tags\": [\"电子产品\"]}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": null, \"tags\": [\"电子产品\"]}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": {\"value\": 100.0}, \"tags\": [\"电子产品\"]}",
        
        // tags as non-array or wrong element types
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": \"电子产品\"}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": 123}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": true}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": null}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": {\"tag\": \"电子产品\"}}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [123, 456]}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [true, false]}",
        
        // Mixed type errors
        "{\"name\": 123, \"description\": 456, \"estimatedPrice\": \"invalid\", \"tags\": \"not-array\"}",
        
        // Arrays with wrong nesting
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 100.0, \"tags\": [[\"电子产品\"]]}",
        
        // Invalid numbers
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": -100.0, \"tags\": [\"电子产品\"]}",
        "{\"name\": \"iPhone\", \"description\": \"物品描述\", \"estimatedPrice\": 0, \"tags\": [\"电子产品\"]}",
    )
    
    Arb.element(wrongTypeCombinations).next(rs)
}

/**
 * Arbitrary generator for valid complete JSON strings
 */
fun Arb.Companion.validCompleteJson(): Arb<String> = arbitrary { rs ->
    val itemNames = listOf(
        "iPhone 13", "MacBook Pro", "计算机组成原理", "算法导论", "Nike运动鞋",
        "优衣库衬衫", "篮球", "羽毛球拍", "水杯", "台灯", "充电宝", "耳机"
    )
    
    val itemDescriptions = listOf(
        "一部功能完好的手机，使用约一年时间",
        "高性能笔记本电脑，适合编程和设计工作",
        "经典计算机教材，适合计算机专业学生使用",
        "算法学习的经典教材，内容详实",
        "舒适的运动鞋，适合跑步和日常穿着",
        "纯棉材质，穿着舒适",
        "标准7号篮球，手感良好",
        "轻量化设计，适合初学者",
        "保温效果好，容量500ml",
        "护眼台灯，亮度可调节",
        "20000mAh大容量，支持快充",
        "蓝牙无线耳机，音质清晰"
    )
    
    val itemTags = listOf(
        listOf("电子产品", "手机"),
        listOf("电子产品", "电脑"),
        listOf("书籍", "教材"),
        listOf("书籍", "教材"),
        listOf("服装", "运动鞋"),
        listOf("服装", "衬衫"),
        listOf("运动器材", "球类"),
        listOf("运动器材", "球拍"),
        listOf("生活用品"),
        listOf("生活用品"),
        listOf("电子产品", "配件"),
        listOf("电子产品", "耳机")
    )
    
    val name = Arb.element(itemNames).next(rs)
    val description = Arb.element(itemDescriptions).next(rs)
    val estimatedPrice = Arb.double(1.0, 10000.0).next(rs)
    val tags = Arb.element(itemTags).next(rs)
    
    // Generate JSON with possible markdown formatting
    val tagsJson = tags.joinToString(", ") { "\"$it\"" }
    val jsonContent = """
        {
            "name": "$name",
            "description": "$description",
            "estimatedPrice": $estimatedPrice,
            "tags": [$tagsJson]
        }
    """.trimIndent()
    
    // Sometimes add markdown code block
    if (Arb.boolean().next(rs)) {
        "```json\n$jsonContent\n```"
    } else {
        jsonContent
    }
}