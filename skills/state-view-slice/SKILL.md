# STATE_VIEW Slice Skill

## Overview

A STATE_VIEW slice represents a read-only projection/view of data. It does NOT contain commands - only read models that are populated by events from other slices. This is the "Query" side of CQRS.

## Input Structure (from config.json)

```json
{
  "sliceType": "STATE_VIEW",
  "title": "slice: <Name>",
  "context": "<BoundedContext>",
  "commands": [],  // Always empty for STATE_VIEW
  "events": [],    // Always empty for STATE_VIEW
  "readmodels": [...],
  "screens": [...],
  "specifications": [...]
}
```

## Key Characteristics

1. **No commands** - STATE_VIEW slices only display data
2. **No events owned** - Events come from other STATE_CHANGE slices
3. **ReadModels reference external events** - Dependencies with INBOUND type point to events
4. **Screens display data** - May link to commands in other slices

## Flow Pattern

```
Events (from STATE_CHANGE slices) → @EventHandler Projector → ReadModel Entity → @QueryHandler → Screen
```

## Code Generation Steps

### Step 1: Identify Source Events

Look at the readmodel's dependencies with `type: "INBOUND"` and `elementType: "EVENT"`:

```json
"dependencies": [
  {
    "type": "INBOUND",
    "title": "Law Firm Created",
    "elementType": "EVENT"
  },
  {
    "type": "INBOUND",
    "title": "Law Firm Updated",
    "elementType": "EVENT"
  }
]
```

These events must already exist (from STATE_CHANGE slices).

### Step 2: Determine Persistence Strategy

Before creating entities, determine which persistence pattern to use based on the readmodel's fields:

**Decision Criteria:**
| Scenario | Pattern | When to Use |
|----------|---------|-------------|
| Single `idAttribute: true` field | Single Key | Entity represents one unique aggregate/record |
| Multiple `idAttribute: true` fields | Multi Key (Composite) | Entity represents a combination (e.g., assignment, membership) |
| `listElement: true` on readmodel | Multi Key (Composite) | ReadModel returns a list of related entities |
| Has parent/child relationship | Multi Key (Composite) | Entity belongs to another entity (e.g., person-to-case) |

---

## Persistence Pattern A: Single Key

Use when the readmodel has exactly ONE `idAttribute: true` field (typically `aggregateId` or similar).

### A.1: Create Single-Key Entity

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/<ReadModelName>ReadModel.kt`

```kotlin
package de.alex.<module>.<readmodelname>

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

data class <ReadModelName>ReadModelQuery(val aggregateId: UUID)

@Table(name = "<table_name>_read_model_entity", schema = "public")
@Entity
class <ReadModelName>ReadModelEntity {
    @Id
    @Column(name = "<id_field_name>")
    var <idFieldName>: UUID? = null

    // Add fields from readmodel definition (non-id fields)
    @Column(name = "<field_name>")
    var <fieldName>: <Type>? = null

    // Common: lawfirmId for multi-tenancy
    @Column(name = "lawfirm_id")
    var lawfirmId: UUID? = null
}

data class <ReadModelName>ReadModel(val data: <ReadModelName>ReadModelEntity)
```

**Real Example (CaseDetailsReadModel):**
```kotlin
package de.alex.cases.casedetails

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

data class CaseDetailsReadModelQuery(val aggregateId: UUID)

@Table(name = "case_details_read_model_entity", schema = "public")
@Entity
class CaseDetailsReadModelEntity {
    @Id @Column(name = "caseId") var caseId: UUID? = null
    @Column(name = "ablagenummer") var ablagenummer: String? = null
    @Column(name = "description") var description: String? = null
    @Column(name = "disputeAmount") var disputeAmount: BigDecimal? = null
    @Column(name = "active_case") var activeCase: Boolean? = null
    @Column(name = "lawfirm_id") var lawfirmId: UUID? = null
}

data class CaseDetailsReadModel(val data: CaseDetailsReadModelEntity)
```

### A.2: Create Single-Key Repository

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/internal/<ReadModelName>ReadModelRepository.kt` (or inline in Projector)

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface <ReadModelName>ReadModelRepository : JpaRepository<<ReadModelName>ReadModelEntity, UUID> {
    // Optional: query methods for filtering
    fun findByLawfirmId(lawfirmId: UUID): List<<ReadModelName>ReadModelEntity>
}
```

### A.3: Create Single-Key Projector

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.ProcessingGroups
import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelEntity
import de.alex.events.*
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

interface <ReadModelName>ReadModelRepository : JpaRepository<<ReadModelName>ReadModelEntity, UUID>

@ProcessingGroup(ProcessingGroups.<MODULE>)
@Component
class <ReadModelName>ReadModelProjector(var repository: <ReadModelName>ReadModelRepository) {

    @EventHandler
    fun on(event: <CreatedOrUpdatedEvent>) {
        // findById returns Optional, use orElse for upsert pattern
        val entity = repository.findById(event.aggregateId).orElse(<ReadModelName>ReadModelEntity())
        entity.apply {
            <idFieldName> = event.aggregateId
            <fieldName> = event.<eventFieldName>
        }.also { repository.save(it) }
    }

    @EventHandler
    fun on(event: <DeleteEvent>) {
        // Simple delete by primary key
        repository.deleteById(event.aggregateId)
    }
}
```

### A.4: Create Single-Key QueryHandler

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModel
import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class <ReadModelName>ReadModelQueryHandler(
    private val repository: <ReadModelName>ReadModelRepository
) {
    @QueryHandler
    fun handleQuery(query: <ReadModelName>ReadModelQuery): <ReadModelName>ReadModel? {
        val entity = repository.findByIdOrNull(query.aggregateId) ?: return null
        return <ReadModelName>ReadModel(entity)
    }
}
```

### A.5: Single-Key Database Migration

```sql
CREATE TABLE IF NOT EXISTS public.<table_name>_read_model_entity (
    "<id_column>" UUID NOT NULL,
    "<field_column>" <SQL_TYPE>,
    "lawfirm_id" UUID,
    PRIMARY KEY ("<id_column>")
);

CREATE INDEX IF NOT EXISTS idx_<table>_lawfirmid ON public.<table_name>_read_model_entity("lawfirm_id");
```

---

## Persistence Pattern B: Multi Key (Composite Key)

Use when the readmodel has MULTIPLE `idAttribute: true` fields or represents a many-to-many/parent-child relationship.

### B.1: Choose Key Class Style

There are two common approaches for composite keys:

**Option 1: Simple class with @NoArgConstructor** (preferred for simple cases)
```kotlin
import de.alex.common.NoArgConstructor

@NoArgConstructor
class <ReadModelName>Key(
    var <firstIdField>: UUID,
    var <secondIdField>: UUID
)
```

**Option 2: @Embeddable data class** (when using embedded queries)
```kotlin
import jakarta.persistence.Embeddable

@Embeddable
data class <ReadModelName>Key(
    var <firstIdField>: UUID,
    var <secondIdField>: <Type>  // Can be UUID, String, etc.
)
```

### B.2: Create Multi-Key Entity

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/<ReadModelName>ReadModel.kt`

```kotlin
package de.alex.<module>.<readmodelname>

import de.alex.common.NoArgConstructor
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.util.UUID

class <ReadModelName>ReadModelQuery()  // Often empty for list queries

@NoArgConstructor
class <ReadModelName>Key(
    var <firstIdField>: UUID,
    var <secondIdField>: UUID
)

@IdClass(<ReadModelName>Key::class)
@Table(name = "<table_name>_read_model_entity", schema = "public")
@Entity
class <ReadModelName>ReadModelEntity {
    @Id @Column(name = "<first_id>")
    var <firstIdField>: UUID? = null

    @Id @Column(name = "<second_id>")
    var <secondIdField>: UUID? = null

    // Add remaining non-id fields
    @Column(name = "<field_name>")
    var <fieldName>: <Type>? = null

    @Column(name = "lawfirmId")
    var lawfirmId: UUID? = null
}

// For composite keys, ReadModel typically wraps a List
data class <ReadModelName>ReadModel(val data: List<<ReadModelName>ReadModelEntity>)
```

**Real Example (CaseAssignmentsReadModel):**
```kotlin
package de.alex.cases.caseassignments

import de.alex.common.NoArgConstructor
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.util.UUID

class CaseAssignmentsReadModelQuery()

@NoArgConstructor
class CaseAssignmentsKey(var caseId: UUID, var assignedPersonId: UUID)

@IdClass(CaseAssignmentsKey::class)
@Table(name = "case_assignments_read_model_entity", schema = "public")
@Entity
class CaseAssignmentsReadModelEntity {
    @Id @Column(name = "caseId") var caseId: UUID? = null
    @Id @Column(name = "assigned_person") var assignedPersonId: UUID? = null
    @Column(name = "assigned_role") var assignedRole: String? = null
    @Column(name = "lawfirmId") var lawfirmId: UUID? = null
}

data class CaseAssignmentsReadModel(val data: List<CaseAssignmentsReadModelEntity>)
```

**Real Example with @Embeddable (TaskReadModel):**
```kotlin
package de.alex.tasks.tasks

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.util.UUID

@Embeddable
data class TaskReadModelKey(var taskId: UUID, var caseId: UUID)

@IdClass(TaskReadModelKey::class)
@Table(name = "tasks_read_model_entity", schema = "public")
@Entity
class TaskReadModelEntity {
    @Id @Column(name = "taskId") var taskId: UUID? = null
    @Id @Column(name = "caseId") var caseId: UUID? = null

    @Column(name = "title") var title: String? = null
    @Column(name = "lawfirm_id") var lawFirmId: UUID? = null
}

data class TaskReadModel(val data: TaskReadModelEntity)
```

### B.3: Create Multi-Key Repository

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.<readmodelname>.<ReadModelName>Key
import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface <ReadModelName>ReadModelRepository :
    JpaRepository<<ReadModelName>ReadModelEntity, <ReadModelName>Key> {

    // Query by first part of composite key
    fun findBy<FirstIdField>(<firstIdField>: UUID): List<<ReadModelName>ReadModelEntity>

    // Query by second part of composite key
    fun findBy<SecondIdField>(<secondIdField>: UUID): List<<ReadModelName>ReadModelEntity>

    // Query by lawfirmId for multi-tenancy
    fun findByLawfirmId(lawfirmId: UUID): List<<ReadModelName>ReadModelEntity>
}
```

### B.4: Create Multi-Key Projector

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.ProcessingGroups
import de.alex.<module>.<readmodelname>.<ReadModelName>Key
import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelEntity
import de.alex.events.*
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.util.UUID

interface <ReadModelName>ReadModelRepository :
    JpaRepository<<ReadModelName>ReadModelEntity, <ReadModelName>Key>

@ProcessingGroup(ProcessingGroups.<MODULE>)
@Component
class <ReadModelName>ReadModelProjector(var repository: <ReadModelName>ReadModelRepository) {

    @EventHandler
    fun on(event: <AddedEvent>) {
        // Construct composite key for lookup
        val key = <ReadModelName>Key(event.<firstIdField>, event.<secondIdField>)
        val entity = repository.findById(key).orElse(<ReadModelName>ReadModelEntity())

        entity.apply {
            <firstIdField> = event.<firstIdField>
            <secondIdField> = event.<secondIdField>
            <fieldName> = event.<eventFieldName>
            lawfirmId = event.lawFirmId
        }.also { repository.save(it) }
    }

    @EventHandler
    fun on(event: <RemovedEvent>) {
        // Delete using composite key
        val key = <ReadModelName>Key(event.<firstIdField>, event.<secondIdField>)
        repository.deleteById(key)
    }
}
```

**Real Example (CaseAssignmentsProjector):**
```kotlin
@ProcessingGroup(ProcessingGroups.CASES)
@Component
class CaseAssignmentsReadModelProjector(var repository: CaseAssignmentsReadModelRepository) {

    @EventHandler
    fun on(event: CasePersonDeletedEvent) {
        repository.deleteById(CaseAssignmentsKey(event.aggregateId, event.personId))
    }

    @EventHandler
    fun on(event: CasePersonAddedEvent) {
        val entity = repository
            .findById(CaseAssignmentsKey(event.aggregateId, event.personId))
            .orElse(CaseAssignmentsReadModelEntity())
        entity.apply {
            caseId = event.aggregateId
            assignedPersonId = event.personId
            assignedRole = event.role
            lawfirmId = event.lawFirmId
        }.also { repository.save(it) }
    }
}
```

### B.5: Create Multi-Key QueryHandler

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModel
import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class <ReadModelName>ReadModelQueryHandler(
    private val repository: <ReadModelName>ReadModelRepository
) {
    @QueryHandler
    fun handleQuery(query: <ReadModelName>ReadModelQuery): <ReadModelName>ReadModel {
        // For composite keys, typically return a list
        val entities = repository.findBy<FirstIdField>(query.<firstIdField>)
        return <ReadModelName>ReadModel(entities)
    }
}
```

### B.6: Multi-Key Database Migration

```sql
CREATE TABLE IF NOT EXISTS public.<table_name>_read_model_entity (
    "<first_id_column>" UUID NOT NULL,
    "<second_id_column>" UUID NOT NULL,
    "<field_column>" <SQL_TYPE>,
    "lawfirmId" UUID,
    PRIMARY KEY ("<first_id_column>", "<second_id_column>")
);

-- Indexes for querying by individual parts of composite key
CREATE INDEX IF NOT EXISTS idx_<table>_<first_id> ON public.<table_name>_read_model_entity("<first_id_column>");
CREATE INDEX IF NOT EXISTS idx_<table>_<second_id> ON public.<table_name>_read_model_entity("<second_id_column>");
CREATE INDEX IF NOT EXISTS idx_<table>_lawfirmid ON public.<table_name>_read_model_entity("lawfirmId");
```

---

## Field Mapping Rules

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
| idAttribute: true | Use as @Id field |

### Step 3: Create Repository

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/internal/<ReadModelName>ReadModelRepository.kt`

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface <ReadModelName>ReadModelRepository : JpaRepository<<ReadModelName>ReadModelEntity, UUID> {
    // Add custom query methods based on screen requirements
    fun findByLawfirmId(lawfirmId: UUID): List<<ReadModelName>ReadModelEntity>
    fun findByAggregateId(aggregateId: UUID): <ReadModelName>ReadModelEntity?
}
```

### Step 4: Create Projector (EventHandler)

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/internal/<ReadModelName>ReadModelProjector.kt`

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.ProcessingGroups
import de.alex.events.*  // Import source events
import org.axonframework.eventhandling.EventHandler
import org.axonframework.config.ProcessingGroup
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@ProcessingGroup(ProcessingGroups.<MODULE>)
@Component
class <ReadModelName>ReadModelProjector(
    private val repository: <ReadModelName>ReadModelRepository
) {

    @EventHandler
    fun on(event: <SourceEvent1>Event) {
        val entity = repository.findByIdOrNull(event.aggregateId)
            ?: <ReadModelName>ReadModelEntity()

        entity.apply {
            aggregateId = event.aggregateId
            // Map event fields to entity fields
            // Use field.mapping if specified to map different field names
            <fieldName> = event.<eventFieldName>
        }.also { repository.save(it) }
    }

    @EventHandler
    fun on(event: <SourceEvent2>Event) {
        val entity = repository.findByIdOrNull(event.aggregateId)
            ?: return  // Or create new if appropriate

        entity.apply {
            // Update only the fields this event provides
            <fieldName> = event.<eventFieldName>
        }.also { repository.save(it) }
    }

    // Handle delete events if applicable
    @EventHandler
    fun on(event: <DeleteEvent>Event) {
        repository.deleteById(event.aggregateId)
    }
}
```

### Step 5: Create Query

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/<ReadModelName>ReadModelQuery.kt`

```kotlin
package de.alex.<module>.<readmodelname>

import java.util.UUID

data class <ReadModelName>ReadModelQuery(
    val aggregateId: UUID? = null,
    val lawFirmId: UUID? = null
    // Add query parameters based on how screens need to filter data
)
```

### Step 6: Create QueryHandler

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/internal/<ReadModelName>ReadModelQueryHandler.kt`

```kotlin
package de.alex.<module>.<readmodelname>.internal

import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModel
import de.alex.<module>.<readmodelname>.<ReadModelName>ReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

@Component
class <ReadModelName>ReadModelQueryHandler(
    private val repository: <ReadModelName>ReadModelRepository
) {

    @QueryHandler
    fun handleQuery(query: <ReadModelName>ReadModelQuery): <ReadModelName>ReadModel? {
        // For single entity lookup
        val entity = repository.findByIdOrNull(query.aggregateId) ?: return null
        return <ReadModelName>ReadModel(entity)

        // OR for list queries
        // return <ReadModelName>ReadModel(repository.findByLawfirmId(query.lawFirmId))
    }
}
```

### Step 7: Create Database Migration

**Location:** `src/main/resources/db/migration/V<version>__create_<table_name>.sql`

```sql
CREATE TABLE IF NOT EXISTS public.<table_name> (
    "aggregateid" UUID NOT NULL,
    -- Add columns for each field in readmodel
    "<column_name>" <SQL_TYPE>,
    PRIMARY KEY ("aggregateid")
    -- Or for composite key:
    -- PRIMARY KEY ("aggregateid", "<secondary_id>")
);

-- Add indexes for frequently queried fields
CREATE INDEX IF NOT EXISTS idx_<table>_lawfirmid ON public.<table_name>("lawfirmid");
```

### Step 8: Add API Endpoint (if apiEndpoint specified)

If the readmodel has an `apiEndpoint` field, create a REST controller:

**Location:** `src/main/kotlin/de/alex/<module>/<readmodelname>/<ReadModelName>Controller.kt`

```kotlin
package de.alex.<module>.<readmodelname>

import org.axonframework.queryhandling.QueryGateway
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("<apiEndpoint base>")
class <ReadModelName>Controller(
    private val queryGateway: QueryGateway
) {
    @GetMapping("/{aggregateId}")
    fun get(@PathVariable aggregateId: UUID): <ReadModelName>ReadModel? {
        return queryGateway.query(
            <ReadModelName>ReadModelQuery(aggregateId),
            <ReadModelName>ReadModel::class.java
        ).join()
    }
}
```

## Specifications (Test Cases)

STATE_VIEW specifications use Given/Then format (no When since no commands):

**Location:** `src/test/kotlin/de/alex/<module>/<ReadModelName>ProjectionTest.kt`

```kotlin
package de.alex.<module>

import de.alex.BaseIntegrationTest
import de.alex.ProjectionFixtureConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class <ReadModelName>ProjectionTest : BaseIntegrationTest() {

    @Autowired
    lateinit var projectionFixture: ProjectionFixtureConfiguration

    @Autowired
    lateinit var repository: <ReadModelName>ReadModelRepository

    @Test
    fun `<spec title>`() {
        // Given: Apply events from specification
        projectionFixture.apply(<SourceEvent>(...))  // From spec.given

        // Then: Verify read model state
        awaitUntilAsserted {
            val result = repository.findById(<id>).orElse(null)
            assertThat(result).isNotNull
            assertThat(result.<field>).isEqualTo(<expectedValue>)  // From spec.then
        }
    }
}
```

## Example: Processing "slice: Law Firm Details for Auth"

**Input:**
```json
{
  "sliceType": "STATE_VIEW",
  "title": "slice: Law Firm Details for Auth",
  "readmodels": [{
    "title": "Law Firm Details for Auth",
    "fields": [
      {"name": "aggregateId", "type": "UUID", "idAttribute": true},
      {"name": "name", "type": "String"}
    ],
    "dependencies": [
      {"type": "INBOUND", "title": "Law Firm Created", "elementType": "EVENT"},
      {"type": "INBOUND", "title": "Law Firm Updated", "elementType": "EVENT"}
    ]
  }],
  "screens": [...]
}
```

**Generated Files:**
1. `src/main/kotlin/de/alex/lawfirm/lawfirmdetailsforauth/LawFirmDetailsForAuthReadModel.kt` (Entity + DTO)
2. `src/main/kotlin/de/alex/lawfirm/lawfirmdetailsforauth/internal/LawFirmDetailsForAuthReadModelRepository.kt`
3. `src/main/kotlin/de/alex/lawfirm/lawfirmdetailsforauth/internal/LawFirmDetailsForAuthReadModelProjector.kt`
4. `src/main/kotlin/de/alex/lawfirm/lawfirmdetailsforauth/LawFirmDetailsForAuthReadModelQuery.kt`
5. `src/main/kotlin/de/alex/lawfirm/lawfirmdetailsforauth/internal/LawFirmDetailsForAuthReadModelQueryHandler.kt`
6. `src/main/resources/db/migration/V<n>__create_law_firm_details_for_auth.sql`

## Field Mapping Notes

When a readmodel field has a `mapping` attribute:
```json
{
  "name": "lawfirmId",
  "type": "UUID",
  "mapping": "aggregateId"
}
```

This means the `lawfirmId` field in the readmodel should be populated from the `aggregateId` field in the source event.

## Screen Dependencies

Screens in STATE_VIEW slices typically:
1. Have INBOUND dependencies from ReadModels (display the data)
2. May have OUTBOUND dependencies to Commands (from other STATE_CHANGE slices for edit functionality)

This separation keeps the query side clean while allowing screens to trigger write operations.
