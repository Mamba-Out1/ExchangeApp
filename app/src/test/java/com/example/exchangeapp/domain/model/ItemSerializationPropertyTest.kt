package com.example.exchangeapp.domain.model

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Property-Based Test for Item Serialization Round-Trip
 * 
 * **Validates: Requirements 12.5**
 * 
 * Property 8: Item序列化Round-Trip属性
 * For any valid Item object, serializing it to JSON and then deserializing back 
 * SHALL produce an Item object that is equal to the original (item == deserialize(serialize(item)))
 */
class ItemSerializationPropertyTest : StringSpec({
    
    "Item serialization round-trip property - any valid Item can be serialized and deserialized preserving equality" {
        checkAll(arbItem()) { item ->
            // Arrange: Generate a random Item
            val json = Json {
                prettyPrint = false
                encodeDefaults = true
            }
            
            // Act: Serialize to JSON and deserialize back
            val serialized = json.encodeToString(item)
            val deserialized = json.decodeFromString<Item>(serialized)
            
            // Assert: Round-trip should preserve equality
            deserialized shouldBe item
        }
    }
    
    "Item with null location serialization round-trip property" {
        checkAll(arbItemWithNullLocation()) { item ->
            // Arrange: Generate Item with null location
            val json = Json {
                prettyPrint = false
                encodeDefaults = true
            }
            
            // Act: Serialize to JSON and deserialize back
            val serialized = json.encodeToString(item)
            val deserialized = json.decodeFromString<Item>(serialized)
            
            // Assert: Round-trip should preserve equality including null location
            deserialized shouldBe item
            deserialized.location shouldBe null
        }
    }
    
    "Item with empty lists serialization round-trip property" {
        checkAll(arbItemWithEmptyLists()) { item ->
            // Arrange: Generate Item with empty images and tags
            val json = Json {
                prettyPrint = false
                encodeDefaults = true
            }
            
            // Act: Serialize to JSON and deserialize back
            val serialized = json.encodeToString(item)
            val deserialized = json.decodeFromString<Item>(serialized)
            
            // Assert: Round-trip should preserve equality including empty lists
            deserialized shouldBe item
            deserialized.images shouldBe emptyList()
            deserialized.tags shouldBe emptyList()
        }
    }
})

// Custom Arbitraries for generating test data

/**
 * Arbitrary generator for Location objects
 */
fun arbLocation(): Arb<Location> = arbitrary {
    Location(
        latitude = Arb.double(-90.0, 90.0).bind(),
        longitude = Arb.double(-180.0, 180.0).bind(),
        address = Arb.string(0..100).orNull().bind()
    )
}

/**
 * Arbitrary generator for ItemStatus enum
 */
fun arbItemStatus(): Arb<ItemStatus> = Arb.enum<ItemStatus>()

/**
 * Arbitrary generator for valid Item objects
 */
fun arbItem(): Arb<Item> = arbitrary {
    Item(
        id = Arb.string(1..50).bind(),
        userId = Arb.string(1..50).bind(),
        name = Arb.string(1..100).bind(),
        description = Arb.string(1..500).bind(),
        estimatedPrice = Arb.double(0.0, 100000.0).bind(),
        images = Arb.list(Arb.string(1..200), 1..9).bind(),
        tags = Arb.list(Arb.string(1..50), 0..5).bind(),
        location = arbLocation().orNull().bind(),
        status = arbItemStatus().bind(),
        createdAt = Arb.long(0L, System.currentTimeMillis()).bind(),
        updatedAt = Arb.long(0L, System.currentTimeMillis()).bind()
    )
}

/**
 * Arbitrary generator for Item objects with null location
 */
fun arbItemWithNullLocation(): Arb<Item> = arbitrary {
    Item(
        id = Arb.string(1..50).bind(),
        userId = Arb.string(1..50).bind(),
        name = Arb.string(1..100).bind(),
        description = Arb.string(1..500).bind(),
        estimatedPrice = Arb.double(0.0, 100000.0).bind(),
        images = Arb.list(Arb.string(1..200), 1..9).bind(),
        tags = Arb.list(Arb.string(1..50), 0..5).bind(),
        location = null,
        status = arbItemStatus().bind(),
        createdAt = Arb.long(0L, System.currentTimeMillis()).bind(),
        updatedAt = Arb.long(0L, System.currentTimeMillis()).bind()
    )
}

/**
 * Arbitrary generator for Item objects with empty lists
 */
fun arbItemWithEmptyLists(): Arb<Item> = arbitrary {
    Item(
        id = Arb.string(1..50).bind(),
        userId = Arb.string(1..50).bind(),
        name = Arb.string(1..100).bind(),
        description = Arb.string(1..500).bind(),
        estimatedPrice = Arb.double(0.0, 100000.0).bind(),
        images = emptyList(),
        tags = emptyList(),
        location = arbLocation().orNull().bind(),
        status = arbItemStatus().bind(),
        createdAt = Arb.long(0L, System.currentTimeMillis()).bind(),
        updatedAt = Arb.long(0L, System.currentTimeMillis()).bind()
    )
}
