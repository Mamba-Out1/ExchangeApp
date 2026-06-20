package com.example.exchangeapp.domain.validation

import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.ItemFormData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 物品表单验证器单元测试
 *
 * 测试各种表单验证场景，包括：
 * - 有效表单数据
 * - 单个字段缺失
 * - 多个字段缺失
 * - 边界值测试
 *
 * **验证需求: Requirements 6.6, 6.8**
 */
class ItemFormValidatorTest {
    
    private val validator = ItemFormValidator()
    
    // 有效的表单数据用于测试
    private val validFormData = ItemFormData(
        name = "二手笔记本电脑",
        description = "配置：i7处理器，16GB内存，512GB SSD，95新",
        price = 2999.0,
        images = listOf("image1.jpg", "image2.jpg"),
        tags = listOf("电子产品", "电脑")
    )
    
    @Test
    fun `validate should return success for valid form data`() {
        // When
        val result = validator.validate(validFormData)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrNull())
    }
    
    @Test
    fun `validateOrThrow should not throw for valid form data`() {
        // When & Then
        validator.validateOrThrow(validFormData) // 不应抛出异常
    }
    
    @Test
    fun `isValid should return true for valid form data`() {
        // When
        val isValid = validator.isValid(validFormData)
        
        // Then
        assertTrue(isValid)
    }
    
    @Test
    fun `validate should fail when name is blank`() {
        // Given
        val formData = validFormData.copy(name = "")
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<ValidationException>(exception)
        assertEquals(listOf("name"), exception.missingFields)
    }
    
    @Test
    fun `validate should fail when name contains only whitespace`() {
        // Given
        val formData = validFormData.copy(name = "   ")
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<ValidationException>(exception)
        assertEquals(listOf("name"), exception.missingFields)
    }
    
    @Test
    fun `validate should fail when description is blank`() {
        // Given
        val formData = validFormData.copy(description = "")
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<ValidationException>(exception)
        assertEquals(listOf("description"), exception.missingFields)
    }
    
    @Test
    fun `validate should fail when price is zero`() {
        // Given
        val formData = validFormData.copy(price = 0.0)
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<ValidationException>(exception)
        assertEquals(listOf("price"), exception.missingFields)
    }
    
    @Test
    fun `validate should fail when price is negative`() {
        // Given
        val formData = validFormData.copy(price = -100.0)
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<ValidationException>(exception)
        assertEquals(listOf("price"), exception.missingFields)
    }
    
    @Test
    fun `validate should fail when images list is empty`() {
        // Given
        val formData = validFormData.copy(images = emptyList())
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<ValidationException>(exception)
        assertEquals(listOf("images"), exception.missingFields)
    }
    
    @Test
    fun `validate should fail when multiple fields are missing`() {
        // Given
        val formData = ItemFormData(
            name = "",
            description = "",
            price = 0.0,
            images = emptyList(),
            tags = emptyList()
        )
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<ValidationException>(exception)
        assertEquals(
            listOf("description", "images", "name", "price"),
            exception.missingFields.sorted()
        )
    }
    
    @Test
    fun `validateOrThrow should throw ValidationException when validation fails`() {
        // Given
        val formData = validFormData.copy(name = "")
        
        // When & Then
        try {
            validator.validateOrThrow(formData)
            throw AssertionError("Expected ValidationException to be thrown")
        } catch (e: ValidationException) {
            // 验证异常类型和内容
            assertEquals(listOf("物品名称"), e.missingFields)
        }
    }
    
    @Test
    fun `isValid should return false when validation fails`() {
        // Given
        val formData = validFormData.copy(description = "")
        
        // When
        val isValid = validator.isValid(formData)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `tags can be empty and still pass validation`() {
        // Given
        val formData = validFormData.copy(tags = emptyList())
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `price can be a small positive value`() {
        // Given
        val formData = validFormData.copy(price = 0.01)
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `single image is sufficient`() {
        // Given
        val formData = validFormData.copy(images = listOf("single-image.jpg"))
        
        // When
        val result = validator.validate(formData)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `ItemFormData isValid method should match validator`() {
        // Given
        val validFormData = ItemFormData(
            name = "测试物品",
            description = "测试描述",
            price = 100.0,
            images = listOf("test.jpg")
        )
        
        val invalidFormData = ItemFormData(
            name = "",
            description = "测试描述",
            price = 100.0,
            images = listOf("test.jpg")
        )
        
        // When & Then
        assertTrue(validFormData.isValid())
        assertFalse(invalidFormData.isValid())
        assertEquals(validator.isValid(validFormData), validFormData.isValid())
        assertEquals(validator.isValid(invalidFormData), invalidFormData.isValid())
    }
}