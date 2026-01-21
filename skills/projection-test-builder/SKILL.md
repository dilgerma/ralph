# Projection Test Builder Skill

## Overview

This skill generates projection integration tests from STATE_VIEW slice specifications. It reads the specification's given/then structure and creates Kotlin tests that verify the read model projection behavior using the Axon Framework event sourcing pattern.

## When to Use

Use this skill when:
- You have a STATE_VIEW slice with specifications defined
- You need to generate integration tests for a read model projector
- The specifications contain `given` (events) and `then` (expected read model state)

## Input Structure

The slice JSON contains specifications with this structure:

```json
{
  "specifications": [
    {
      "title": "spec: cart items with removed item",
      "given": [
        {
          "title": "Cart Created",
          "type": "SPEC_EVENT",
          "fields": [{ "name": "aggregateId", "type": "UUID", "example": "" }]
        },
        {
          "title": "Item Added",
          "type": "SPEC_EVENT",
          "fields": [
            { "name": "aggregateId", "type": "UUID" },
            { "name": "description", "type": "String" },
            { "name": "price", "type": "Double", "example": "9.99" }
          ]
        }
      ],
      "when": [],
      "then": [
        {
          "title": "cart items",
          "type": "SPEC_READMODEL",
          "fields": [
            { "name": "aggregateId", "type": "UUID", "idAttribute": true },
            { "name": "totalPrice", "type": "Double", "example": "19.98" }
          ]
        }
      ],
      "comments": [{ "description": "Read Model should display an empty list" }]
    }
  ]
}
```

## Generation Steps

### Step 1: Identify Test File Location

**Location Pattern:** `src/test/kotlin/de/alex/<module>/<readmodelname>/integration/<ReadModelName>ProjectionTest.kt`

- Extract `<module>` from the readmodel's `domain` or `context` field (lowercase)
- Extract `<readmodelname>` from the readmodel's `title` (convert to lowercase, no spaces)
- Example: "cart items" in "Cart" context → `src/test/kotlin/de/alex/cart/cartitems/integration/CartItemsProjectionTest.kt`

### Step 2: Convert Event Names to Class Names

Transform event titles to Kotlin event class names:
- "Item Added" → `ItemAddedEvent`
- "Cart Created" → `CartCreatedEvent`
- "Item Removed" → `ItemRemovedEvent`

Pattern: Remove spaces, append "Event" suffix.

### Step 3: Determine Aggregate Type

Look at the `aggregate` or `aggregateDependencies` field in the readmodel to identify which aggregate to use:

```json
"aggregate": "Cart",
"aggregateDependencies": ["Cart"]
```

This becomes `CartAggregate` for the repository.

### Step 4: Generate Test Class

**Template:**

```kotlin
/* (C)2024 */
package de.alex.<module>.<readmodelname>.integration

import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModel
import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelQuery
import de.alex.common.support.BaseIntegrationTest
import de.alex.common.support.ProjectionFixtureConfiguration
import de.alex.common.support.RandomData
import de.alex.common.support.awaitUntilAssserted
import de.alex.domain.<Aggregate>Aggregate
import de.alex.events.*
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.modelling.command.Repository
import org.axonframework.queryhandling.QueryGateway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class <ReadModelName>ProjectionTest : BaseIntegrationTest() {
    @Autowired private lateinit var commandGateway: CommandGateway
    @Autowired private lateinit var queryGateway: QueryGateway
    @Autowired private lateinit var repository: Repository<<Aggregate>Aggregate>

    // Generate one test method per specification
}
```

### Step 5: Generate Test Methods

For each specification in `specifications[]`:

```kotlin
@Test
fun `<spec title>`() {
    // Generate UUIDs for id fields
    val aggregateId = UUID.randomUUID()
    // Generate additional UUIDs for other id fields (itemId, productId, etc.)

    var fixture =
        ProjectionFixtureConfiguration.aggregateInstance {
            repository.newInstance { <Aggregate>Aggregate() }
        }

    // Apply events from given[] in index order
    fixture.given(
        *listOf(
            // For each event in given[], sorted by index:
            RandomData.newInstance<<EventName>Event> {
                // Set fields from the spec, using example values or generated UUIDs
                this.aggregateId = aggregateId
                // Map other fields
            },
        ).toTypedArray(),
    )
    fixture.apply()

    // Verify read model state from then[]
    awaitUntilAssserted {
        var readModel =
            queryGateway.query(
                <ReadModelName>ReadModelQuery(aggregateId),
                <ReadModelName>ReadModel::class.java
            )
        // Assert based on then[] specification
        // If comments mention "empty list" → assertThat(readModel.get().data).isEmpty()
        // Otherwise assert expected field values
    }
}
```

## Field Handling Rules

### UUID Fields
- Fields with `idAttribute: true` should use shared UUIDs across related events
- Generate a UUID variable at test start: `val aggregateId = UUID.randomUUID()`
- If the spec has multiple id fields (e.g., `itemId`, `productId`), generate each

### Example Values
- If a field has an `example` value (e.g., `"example": "9.99"`), use that value for assertions
- For assertions, convert the example to the appropriate type:
  - `"9.99"` for Double → `9.99`
  - `"19.98"` for Double → `19.98`

### Event Field Mapping
- Match field names between spec event fields and actual event class fields
- The `mapping` field indicates the source field: `"mapping": "productId"` means map from `productId`

## Assertion Patterns

### Empty List Assertion
When comments contain "empty list" or the then[] result should be empty:
```kotlin
assertThat(readModel.get().data).isEmpty()
```

### List Size Assertion
When multiple items expected:
```kotlin
assertThat(readModel.get().data).hasSize(2)
```

### Single Entity Assertion
When then[] has specific field examples:
```kotlin
assertThat(readModel.get().data.totalPrice).isEqualTo(19.98)
```

### Existence Assertion
Basic non-null check:
```kotlin
assertThat(readModel.get()).isNotNull
```

## Complete Example

**Input Spec:**
```json
{
  "title": "spec: cart items with cleared cart",
  "given": [
    { "title": "Cart Created", "index": 0, "fields": [{"name": "aggregateId", "type": "UUID"}] },
    { "title": "Item Added", "index": 1, "fields": [...] },
    { "title": "Cart Cleared", "index": 2, "fields": [{"name": "aggregateId", "type": "UUID"}] }
  ],
  "then": [
    { "title": "cart items", "type": "SPEC_READMODEL", "fields": [...] }
  ],
  "comments": [{ "description": "Read Model should display an empty list" }]
}
```

**Generated Test:**
```kotlin
@Test
fun `spec cart items with cleared cart`() {
    val aggregateId = UUID.randomUUID()
    val itemId = UUID.randomUUID()
    val productId = UUID.randomUUID()

    var fixture =
        ProjectionFixtureConfiguration.aggregateInstance {
            repository.newInstance { CartAggregate() }
        }

    fixture.given(
        *listOf(
            RandomData.newInstance<CartCreatedEvent> {
                this.aggregateId = aggregateId
            },
            RandomData.newInstance<ItemAddedEvent> {
                this.aggregateId = aggregateId
                this.itemId = itemId
                this.productId = productId
            },
            RandomData.newInstance<CartClearedEvent> {
                this.aggregateId = aggregateId
            },
        ).toTypedArray(),
    )
    fixture.apply()

    awaitUntilAssserted {
        var readModel =
            queryGateway.query(
                CartItemsReadModelQuery(aggregateId),
                CartItemsReadModel::class.java
            )
        assertThat(readModel.get().data).isEmpty()
    }
}
```

## Multiple Specifications Example

When a slice has multiple specifications, generate one test method per spec:

```kotlin
class CartItemsProjectionTest : BaseIntegrationTest() {
    @Autowired private lateinit var commandGateway: CommandGateway
    @Autowired private lateinit var queryGateway: QueryGateway
    @Autowired private lateinit var repository: Repository<CartAggregate>

    @Test
    fun `spec cart items`() {
        // Test for adding items - expects 2 items in list
    }

    @Test
    fun `spec cart items with removed item`() {
        // Test for removing item - expects empty list
    }

    @Test
    fun `spec cart items with cleared cart`() {
        // Test for clearing cart - expects empty list
    }

    @Test
    fun `spec cart items with archived items`() {
        // Test for archiving - expects item removed
    }
}
```

## Special Cases

### Events with Same Name (Multiple Instances)
When the given[] has multiple events with the same title (e.g., two "Item Added" events):
- Use unique UUIDs for distinguishing fields (e.g., different `itemId` for each)
- Keep the same `aggregateId` to link them

```kotlin
val itemId1 = UUID.randomUUID()
val itemId2 = UUID.randomUUID()

fixture.given(
    *listOf(
        RandomData.newInstance<CartCreatedEvent> { this.aggregateId = aggregateId },
        RandomData.newInstance<ItemAddedEvent> {
            this.aggregateId = aggregateId
            this.itemId = itemId1
            this.price = 9.99
        },
        RandomData.newInstance<ItemAddedEvent> {
            this.aggregateId = aggregateId
            this.itemId = itemId2
            this.price = 9.99
        },
    ).toTypedArray(),
)
```

### Delete/Remove Events
When an event removes/archives an item, the subsequent event should reference the same ID:
```kotlin
// Item Added with itemId1
RandomData.newInstance<ItemAddedEvent> {
    this.aggregateId = aggregateId
    this.itemId = itemId1
},
// Item Removed referencing the same itemId1
RandomData.newInstance<ItemRemovedEvent> {
    this.aggregateId = aggregateId
    this.itemId = itemId1
},
```

## Dependencies

Ensure these imports are available:
- `de.alex.common.support.BaseIntegrationTest`
- `de.alex.common.support.ProjectionFixtureConfiguration`
- `de.alex.common.support.RandomData`
- `de.alex.common.support.awaitUntilAssserted`
- Event classes from `de.alex.events.*`
- Aggregate class from `de.alex.domain.*`