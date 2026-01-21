# AUTOMATION Slice Skill

## Overview

An AUTOMATION slice contains processors that react to events and trigger commands automatically. This represents event-driven automation logic - the "glue" between different parts of the system.

Automation slices are typically embedded within STATE_CHANGE slices (as `processors` array), but the automation pattern itself is distinct.

## Input Structure (from config.json)

Automation processors appear in the `processors` array of a slice:

```json
{
  "sliceType": "STATE_CHANGE",
  "title": "slice: <Name>",
  "processors": [
    {
      "id": "...",
      "title": "Admin Assignment",
      "type": "AUTOMATION",
      "fields": [...],
      "aggregate": "default",
      "dependencies": [
        {
          "type": "OUTBOUND",
          "title": "Assign Admin to Organization",
          "elementType": "COMMAND"
        }
      ]
    }
  ]
}
```

## Flow Pattern

```
Source Event → @EventHandler Processor → CommandGateway.send() → Target Command
                     ↓
              (optional) QueryGateway.query() → ReadModel (for data lookup)
```

## Key Characteristics

1. **Event-triggered** - Processors react to events via `@EventHandler`
2. **Command-emitting** - Use `CommandGateway` to send commands
3. **No state mutation** - Processors don't own state, they orchestrate
4. **Replay-safe** - Use `@DisallowReplay` to prevent re-triggering on event replay
5. **Implements Processor interface** - Marker interface for automation components

## Code Generation Steps

### Step 1: Identify Source Event

Look for events that trigger the processor. The processor's fields often match event fields, indicating which event triggers it.

Check dependencies:
- OUTBOUND to COMMAND = the command this processor sends
- INBOUND from EVENT = the event that triggers this processor (if explicitly specified)

If not explicit, look at field matching between processor and events in the slice.

### Step 2: Create Processor Class

**Location:** `src/main/kotlin/de/alex/<module>/<processorname>/internal/<ProcessorName>Processor.kt`

**Basic Template (Event → Command):**
```kotlin
package de.alex.<module>.<processorname>.internal

import de.alex.common.Processor
import de.alex.events.<SourceEvent>Event
import de.alex.<module>.ProcessingGroups
import de.alex.<module>.domain.commands.<commandname>.<TargetCommand>Command
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.DisallowReplay
import org.axonframework.eventhandling.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/*
Boardlink: https://miro.com/app/board/uXjVJzSW8-Y=/?moveToWidget=<processor.id>
*/
@ProcessingGroup(ProcessingGroups.<MODULE>)
@Component
class <ProcessorName>Processor : Processor {
    var logger = KotlinLogging.logger {}

    @Autowired
    lateinit var commandGateway: CommandGateway

    @DisallowReplay
    @EventHandler
    fun on(event: <SourceEvent>Event) {
        commandGateway.send<<TargetCommand>Command>(
            <TargetCommand>Command(
                // Map event fields to command fields
                // Use processor.fields[].mapping to determine field mappings
                aggregateId = event.aggregateId,
                <field> = event.<eventField>
            )
        )
    }
}
```

**Template with MetaData (for lawFirmId context):**
```kotlin
@DisallowReplay
@EventHandler
fun on(event: <SourceEvent>Event) {
    commandGateway.send<<TargetCommand>Command>(
        <TargetCommand>Command(
            aggregateId = event.aggregateId,
            // ... field mappings
        ),
        MetaData.with(LAW_FIRM_ID_HEADER, event.aggregateId)
    )
}
```

Additional imports needed:
```kotlin
import de.alex.auth.LAW_FIRM_ID_HEADER
import org.axonframework.messaging.MetaData
```

**Template with QueryGateway (for read model lookup):**
```kotlin
package de.alex.<module>.<processorname>.internal

import de.alex.common.Processor
import de.alex.events.<SourceEvent>Event
import de.alex.<module>.ProcessingGroups
import de.alex.<module>.domain.commands.<commandname>.<TargetCommand>Command
import de.alex.<module>.<readmodel>.<ReadModel>ReadModelQuery
import de.alex.<module>.<readmodel>.<ReadModel>ReadModel
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.DisallowReplay
import org.axonframework.eventhandling.EventHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@ProcessingGroup(ProcessingGroups.<MODULE>)
@Component
class <ProcessorName>Processor : Processor {
    var logger = KotlinLogging.logger {}

    @Autowired
    lateinit var commandGateway: CommandGateway

    @Autowired
    lateinit var queryGateway: QueryGateway

    @DisallowReplay
    @EventHandler
    fun on(event: <SourceEvent>Event) {
        // Query for additional data if needed
        val readModel = queryGateway.query(
            <ReadModel>ReadModelQuery(event.aggregateId),
            <ReadModel>ReadModel::class.java
        ).join() ?: return

        commandGateway.send<<TargetCommand>Command>(
            <TargetCommand>Command(
                aggregateId = event.aggregateId,
                <field> = readModel.data.<field>
                // Mix event and read model data
            )
        )
    }
}
```

### Step 3: Handle Field Mappings

When processor fields have a `mapping` attribute:
```json
{
  "name": "userId",
  "type": "String",
  "mapping": "owner"
}
```

This means:
- Processor receives field as `userId`
- But it maps from source event's `owner` field
- Command should receive as the command's expected field name

Example:
```kotlin
// Event has: owner: String
// Processor field: userId mapping: owner
// Command expects: userId: String

commandGateway.send(
    SomeCommand(
        userId = event.owner  // Map using the mapping attribute
    )
)
```

### Step 4: Handle Multiple Commands

If processor has multiple OUTBOUND dependencies to different commands, emit multiple commands:

```kotlin
@DisallowReplay
@EventHandler
fun on(event: <SourceEvent>Event) {
    // First command
    commandGateway.send<FirstCommand>(
        FirstCommand(...)
    )

    // Second command (may be conditional)
    if (event.someCondition) {
        commandGateway.send<SecondCommand>(
            SecondCommand(...)
        )
    }
}
```

### Step 5: Add @DisallowReplay Annotation

**CRITICAL:** Always use `@DisallowReplay` on automation event handlers to prevent:
- Re-sending commands during event store replays
- Duplicate side effects
- Inconsistent state during recovery

```kotlin
@DisallowReplay
@EventHandler
fun on(event: SomeEvent) {
    // This won't execute during replays
}
```

Place `@DisallowReplay` either:
1. On the method (for specific handlers)
2. On the class (for all handlers in the processor)

## Processing Group Configuration

Ensure the processor uses the correct processing group for its module:

```kotlin
@ProcessingGroup(ProcessingGroups.<MODULE>)
```

This determines:
- Which event processor handles these events
- Error handling and retry behavior
- Replay behavior

## Error Handling

Processors should handle errors gracefully:

```kotlin
@DisallowReplay
@EventHandler
fun on(event: SomeEvent) {
    try {
        commandGateway.send<SomeCommand>(
            SomeCommand(...)
        )
    } catch (e: Exception) {
        logger.error { "Failed to process event ${event.aggregateId}: ${e.message}" }
        throw e  // Re-throw to trigger DLQ handling
    }
}
```

For non-critical automations, you may catch and log without re-throwing:
```kotlin
@DisallowReplay
@EventHandler
fun on(event: SomeEvent) {
    try {
        commandGateway.send<SomeCommand>(SomeCommand(...))
    } catch (e: Exception) {
        logger.warn { "Non-critical automation failed: ${e.message}" }
        // Don't re-throw - allow processing to continue
    }
}
```

## Example: Processing "Admin Assignment" Processor

**Input:**
```json
{
  "title": "Admin Assignment",
  "type": "AUTOMATION",
  "fields": [
    {"name": "aggregateId", "type": "UUID"},
    {"name": "name", "type": "String"},
    {"name": "userId", "type": "String", "mapping": "owner"},
    {"name": "email", "type": "String"}
  ],
  "dependencies": [
    {
      "type": "OUTBOUND",
      "title": "Assign Admin to Organization",
      "elementType": "COMMAND"
    }
  ]
}
```

**Source Event:** `LawFirmCreatedEvent` (determined by field matching)

**Generated Code:**
```kotlin
package de.alex.lawfirm.assignadmin.internal

import de.alex.auth.LAW_FIRM_ID_HEADER
import de.alex.common.Processor
import de.alex.events.LawFirmCreatedEvent
import de.alex.lawfirm.ProcessingGroups
import de.alex.lawfirm.domain.commands.assignadmin.AssignAdminToOrganizationCommand
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.DisallowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.messaging.MetaData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@ProcessingGroup(ProcessingGroups.LAWFIRM)
@Component
class AdminAssignmentProcessor : Processor {
    var logger = KotlinLogging.logger {}

    @Autowired
    lateinit var commandGateway: CommandGateway

    @DisallowReplay
    @EventHandler
    fun on(event: LawFirmCreatedEvent) {
        commandGateway.send<AssignAdminToOrganizationCommand>(
            AssignAdminToOrganizationCommand(
                event.aggregateId,      // aggregateId
                event.email,            // email
                event.owner             // userId (mapped from owner)
            ),
            MetaData.with(LAW_FIRM_ID_HEADER, event.aggregateId)
        )
    }
}
```

## Scheduled Automations

For time-based automations (like "Auto Deactivate License"), you may need a scheduler in addition to or instead of event handlers:

```kotlin
@Component
class ScheduledAutomationProcessor : Processor {
    @Autowired
    lateinit var commandGateway: CommandGateway

    @Autowired
    lateinit var queryGateway: QueryGateway

    @Scheduled(cron = "0 0 * * * *")  // Every hour
    fun processExpiredItems() {
        val expiredItems = queryGateway.query(
            ExpiredItemsQuery(),
            ExpiredItemsReadModel::class.java
        ).join()

        expiredItems.data.forEach { item ->
            commandGateway.send<DeactivateCommand>(
                DeactivateCommand(item.aggregateId)
            )
        }
    }
}
```

## Testing Automations

**Location:** `src/test/kotlin/de/alex/<module>/<ProcessorName>ProcessorTest.kt`

```kotlin
package de.alex.<module>

import de.alex.BaseIntegrationTest
import de.alex.events.<SourceEvent>Event
import de.alex.<module>.domain.commands.<commandname>.<TargetCommand>Command
import io.mockk.mockk
import io.mockk.verify
import org.axonframework.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test

class <ProcessorName>ProcessorTest : BaseIntegrationTest() {

    @Test
    fun `should send command when event received`() {
        val commandGateway = mockk<CommandGateway>(relaxed = true)
        val processor = <ProcessorName>Processor().apply {
            this.commandGateway = commandGateway
        }

        val event = <SourceEvent>Event(
            aggregateId = UUID.randomUUID(),
            // ... other fields
        )

        processor.on(event)

        verify {
            commandGateway.send<<TargetCommand>Command>(match {
                it.aggregateId == event.aggregateId
                // ... verify field mappings
            })
        }
    }
}
```

## Common Patterns

### Pattern 1: Event Chaining
Processor triggers a command that emits another event, enabling event chains:
```
Event A → Processor A → Command B → Event B → Processor B → Command C → ...
```

### Pattern 2: Data Enrichment
Processor queries read model to get additional data before sending command:
```
Event → Processor → Query ReadModel → Enrich + Send Command
```

### Pattern 3: Conditional Processing
Processor applies business logic to determine if/what command to send:
```kotlin
@EventHandler
fun on(event: SomeEvent) {
    when (event.status) {
        "APPROVED" -> commandGateway.send(ApproveCommand(...))
        "REJECTED" -> commandGateway.send(RejectCommand(...))
        else -> logger.info { "No action for status ${event.status}" }
    }
}
```

### Pattern 4: Multi-Aggregate Coordination
Processor coordinates between different aggregates:
```kotlin
@EventHandler
fun on(event: OrderCreatedEvent) {
    // Update inventory aggregate
    commandGateway.send(ReserveInventoryCommand(event.productId, event.quantity))

    // Notify customer aggregate
    commandGateway.send(NotifyCustomerCommand(event.customerId, event.orderId))
}
```
