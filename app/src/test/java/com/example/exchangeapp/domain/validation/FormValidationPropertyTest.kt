package com.example.exchangeapp.domain.validation

import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.ItemFormData
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Property-Based Test for Form Validation
 * 
 * **Validates: Requirements 6.6**
 * 
 * Property 7: 表单验证完整性
 * For any item creation form data, the validation logic SHALL reject the form if and only if 
 * any required field (name, description, price, or images list) is null, empty, or invalid, 
 * and SHALL identify all missing fields in the error response
 */
class FormValidationPropertyTest : StringSpec({
    
    // Feature: campus-exchange-app, Property 7: Form validation completeness
    "form validation correctly identifies all missing required fields" {
        checkAll(100, Arb.itemFormData()) { formData ->
            val validator = ItemFormValidator()
            val result = validator.validate(formData)
            
            val expectedErrors = mutableListOf<String>()
            if (formData.name.isBlank()) expectedErrors.add("name")
            if (formData.description.isBlank()) expectedErrors.add("description")
            if (formData.price <= 0) expectedErrors.add("price")
            if (formData.images.isEmpty()) expectedErrors.add("images")
            
            if (expectedErrors.isEmpty()) {
                result.isSuccess shouldBe true
            } else {
                result.isFailure shouldBe true
                val error = result.exceptionOrNull() as? ValidationException
                error shouldNotBe null
                error?.missingFields?.toSet() shouldBe expectedErrors.toSet()
            }
        }
    }
})

/**
 * Arbitrary generator for ItemFormData objects
 * 
 * Generates random form data with various combinations of valid and invalid fields
 * to thoroughly test the validation logic.
 */
fun Arb.Companion.itemFormData() = arbitrary {
    ItemFormData(
        name = Arb.string(0..100).bind(),
        description = Arb.string(0..500).bind(),
        price = Arb.double(-100.0, 10000.0).bind(),
        images = Arb.list(Arb.string(), 0..15).bind(),
        tags = Arb.list(Arb.string(), 0..10).bind()
    )
}