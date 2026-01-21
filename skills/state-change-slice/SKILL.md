# STATE_CHANGE Slice Skill

## Overview

A STATE_CHANGE slice represents a business operation that modifies aggregate state through commands and events. This is the core CQRS/Event Sourcing pattern.

## Input Structure (from config.json)

```json
{
  "sliceType": "STATE_CHANGE",
  "title": "slice: <Name>",
  "context": "<BoundedContext>",
  "commands": [...],
  "events": [...],
  "readmodels": [...],
  "screens": [...],
  "processors": [...],
  "specifications": [...]
}
```

## Flow Pattern

```
Screen (UI) → Command → Aggregate (@CommandHandler) → Event → ReadModel (via @EventHandler Projector)
                                                    ↓
                                              Automation (if processors exist)
```

## Code Generation Steps

### Step 1: Create Command

**Location:** `src/main/kotlin/de/alex/<module>/domain/commands/<commandname>/<CommandName>Command.kt`

**Template:**
```kotlin
package de.alex.<module>.domain.commands.<commandname>

import de.alex.common.Command
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.UUID

data class <CommandName>Command(
    @TargetAggregateIdentifier var aggregateId: UUID,
    // Add fields from slice command definition
    // Map types: UUID, String, Date → LocalDate, Integer → Int, Boolean, Decimal → BigDecimal
    // Handle cardinality: Single → regular type, Multiple → List<type>
) : Command
```

**Field Mapping Rules:**
| Slice Type | Kotlin Type |
|------------|-------------|
| UUID | UUID |
| String | String |
| Date | LocalDate |
| Integer | Int |
| Boolean | Boolean |
| Decimal | BigDecimal |
| Multiple cardinality | List<T> |
| optional: true | Nullable (T?) |

### Step 2: Create Event(s)

**Location:** `src/main/kotlin/de/alex/events/<EventName>Event.kt`

**Template:**
```kotlin
package de.alex.events

import de.alex.common.Event
import java.util.UUID

data class <EventName>Event(
    var aggregateId: UUID,
    // Add fields from slice event definition
    // Fields typically mirror command fields
) : Event
```

### Step 3: Add CommandHandler to Aggregate

**Location:** `src/main/kotlin/de/alex/domain/<Aggregate>Aggregate.kt`

**Template for existing aggregate:**
```kotlin
@CommandHandler
fun handle(command: <CommandName>Command): CommandResult {
    // Business validation logic
    AggregateLifecycle.apply(
        <EventName>Event(
            command.aggregateId,
            // Map command fields to event fields
        )
    )
    return CommandResult(command.aggregateId.toString(), AggregateLifecycle.getVersion())
}
```

**Template for new aggregate creation (when createsAggregate: true):**
```kotlin
@CreationPolicy(AggregateCreationPolicy.ALWAYS)
@CommandHandler
fun handle(command: <CommandName>Command): CommandResult {
    AggregateLifecycle.apply(
        <EventName>Event(
            command.aggregateId,
            // Map command fields to event fields
        )
    )
    return CommandResult(command.aggregateId.toString(), AggregateLifecycle.getVersion())
}
```

### Step 4: Add EventSourcingHandler to Aggregate

```kotlin
@EventSourcingHandler
fun on(event: <EventName>Event) {
    // Update aggregate state from event
    this.aggregateId = event.aggregateId
    // Update other state fields
}
```

### Step 5: Create/Update Read Model Entity (if readmodels exist)

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/<ReadModelName>ReadModel.kt`

**Template:**
```kotlin
package de.alex.<module>.<readmodelname>

import jakarta.persistence.*
import java.util.UUID
import com.github.f4b6a3.ulid.UlidCreator
import de.alex.common.NoArgConstructor

@NoArgConstructor
class <ReadModelName>Key(val aggregateId: UUID, val <secondaryId>: UUID)

@IdClass(<ReadModelName>Key::class)
@Table(name = "<read_model_table_name>", schema = "public")
@Entity
class <ReadModelName>ReadModelEntity {
    @Id @Column(name = "aggregateId")
    lateinit var aggregateId: UUID

    @Id @Column(name = "<secondaryId>")
    lateinit var <secondaryId>: UUID

    // Add fields from readmodel definition
    @Column(name = "<field_name>")
    var <fieldName>: <Type>? = null
}

data class <ReadModelName>ReadModel(val data: List<<ReadModelName>ReadModelEntity>)
// Or for single entity: data class <ReadModelName>ReadModel(val data: <ReadModelName>ReadModelEntity)
```

### Step 6: Create Projector

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/internal/<ReadModelName>ReadModelProjector.kt`

**Template:**
```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.ProcessingGroups
import de.alex.events.<EventName>Event
import org.axonframework.eventhandling.EventHandler
import org.axonframework.config.ProcessingGroup
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

interface <ReadModelName>ReadModelRepository : JpaRepository<<ReadModelName>ReadModelEntity, <KeyType>>

@ProcessingGroup(ProcessingGroups.<MODULE>)
@Component
class <ReadModelName>ReadModelProjector(var repository: <ReadModelName>ReadModelRepository) {

    @EventHandler
    fun on(event: <EventName>Event) {
        val entity = this.repository.findByIdOrNull(<key>) ?: <ReadModelName>ReadModelEntity()
        entity.apply {
            // Map event fields to entity fields
            aggregateId = event.aggregateId
            // ...
        }.also { this.repository.save(it) }
    }
}
```

### Step 7: Create Query and QueryHandler

**Query Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/<ReadModelName>ReadModelQuery.kt`

```kotlin
package de.alex.<module>.<readmodelname>

import java.util.UUID

data class <ReadModelName>ReadModelQuery(
    val aggregateId: UUID
    // Add query parameters as needed
)
```

**QueryHandler Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/internal/<ReadModelName>ReadModelQueryHandler.kt`

```kotlin
package de.alex.<module>.<readmodelname>.internal

import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class <ReadModelName>ReadModelQueryHandler(
    private val repository: <ReadModelName>ReadModelRepository
) {
    @QueryHandler
    fun handleQuery(query: <ReadModelName>ReadModelQuery): <ReadModelName>ReadModel? {
        return <ReadModelName>ReadModel(repository.findBy<criteria>(query.<param>))
    }
}
```

### Step 8: Create Database Migration

**Location:** `src/main/resources/db/migration/V<version>__<description>.sql`

```sql
CREATE TABLE IF NOT EXISTS public.<table_name> (
    "aggregateid" UUID NOT NULL,
    -- Add columns for each field
    "<column_name>" <SQL_TYPE>,
    PRIMARY KEY ("aggregateid")
);
```

**SQL Type Mapping:**
| Kotlin Type | SQL Type |
|-------------|----------|
| UUID | UUID |
| String | VARCHAR(255) or TEXT |
| LocalDate | DATE |
| Int | INTEGER |
| Boolean | BOOLEAN |
| BigDecimal | DECIMAL(19,4) |

## Specifications (Test Cases)

If the slice contains specifications, create aggregate tests:

**Location:** `src/test/kotlin/de/alex/<module>/<CommandName>Test.kt`

```kotlin
package de.alex.<module>

import de.alex.domain.<Aggregate>Aggregate
import org.axonframework.test.aggregate.AggregateTestFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class <CommandName>Test {
    private lateinit var fixture: AggregateTestFixture<<Aggregate>Aggregate>

    @BeforeEach
    fun setUp() {
        fixture = AggregateTestFixture(<Aggregate>Aggregate::class.java)
    }

    @Test
    fun `<spec title>`() {
        fixture
            .given(<GivenEvent>(...))  // From spec.given
            .`when`(<Command>(...))     // From spec.when
            .expectSuccessfulHandlerExecution()
            .expectEvents(<ExpectedEvent>(...))  // From spec.then
    }
}
```

## Example: Processing "slice: Assign Clerk to Case"

**Input:**
```json
{
  "sliceType": "STATE_CHANGE",
  "title": "slice: Assign Clerk to Case",
  "commands": [{
    "title": "Assign Clerk to Case",
    "aggregate": "Case",
    "fields": [
      {"name": "caseId", "type": "UUID", "idAttribute": true},
      {"name": "clerkId", "type": "UUID"},
      {"name": "hourlyRate", "type": "Decimal"}
    ]
  }],
  "events": [{
    "title": "Clerk Assigned to Case",
    "fields": [...]
  }]
}
```

**Generated Files:**
1. `src/main/kotlin/de/alex/cases/domain/commands/assignclerktocase/AssignClerkToCaseCommand.kt`
2. `src/main/kotlin/de/alex/events/ClerkAssignedToCaseEvent.kt`
3. Update `src/main/kotlin/de/alex/domain/CaseAggregate.kt` with handlers

## Processing Group Configuration

Ensure the module has a ProcessingGroups file:

**Location:** `src/main/kotlin/de/alex/<module>/ProcessingGroups.kt`

```kotlin
package de.alex.<module>

class ProcessingGroups {
    companion object {
        const val <MODULE> = "<MODULE>"
    }
}
```