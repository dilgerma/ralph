package de.nebulit.todolists

import de.nebulit.common.support.RandomData
import de.nebulit.events.TodoListCreatedEvent
import de.nebulit.todolists.internal.TodoListsReadModelProjector
import de.nebulit.todolists.internal.TodoListsReadModelQueryHandler
import de.nebulit.todolists.internal.TodoListsReadModelRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test for todo lists read model projection
 *
 * Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997464
 */
class TodoListsReadModelProjectorTest {

  private lateinit var repository: TodoListsReadModelRepository
  private lateinit var projector: TodoListsReadModelProjector
  private lateinit var queryHandler: TodoListsReadModelQueryHandler

  @BeforeEach
  fun setUp() {
    repository = mockk()
    projector = TodoListsReadModelProjector(repository)
    queryHandler = TodoListsReadModelQueryHandler(repository)
  }

  @Test
  fun `TodoListCreatedEvent creates new entry in read model`() {
    // GIVEN
    val event =
        TodoListCreatedEvent(id = RandomData.newInstance {}, name = RandomData.newInstance {})
    every { repository.findById(event.id) } returns Optional.empty()
    val entitySlot = slot<TodoListsReadModelEntity>()
    every { repository.save(capture(entitySlot)) } answers { entitySlot.captured }

    // WHEN
    projector.on(event)

    // THEN
    verify { repository.save(any()) }
    val savedEntity = entitySlot.captured
    assertEquals(event.id, savedEntity.id)
    assertEquals(event.name, savedEntity.name)
  }

  @Test
  fun `TodoListCreatedEvent updates existing entry in read model`() {
    // GIVEN
    val existingEntity = TodoListsReadModelEntity().apply { id = "existing-id" }
    val event = TodoListCreatedEvent(id = existingEntity.id!!, name = "Updated Name")
    every { repository.findById(event.id) } returns Optional.of(existingEntity)
    val entitySlot = slot<TodoListsReadModelEntity>()
    every { repository.save(capture(entitySlot)) } answers { entitySlot.captured }

    // WHEN
    projector.on(event)

    // THEN
    verify { repository.save(any()) }
    val savedEntity = entitySlot.captured
    assertEquals(event.id, savedEntity.id)
    assertEquals(event.name, savedEntity.name)
  }

  @Test
  fun `query handler returns all todo lists`() {
    // GIVEN
    val entities =
        listOf(
            TodoListsReadModelEntity().apply {
              id = "list-1"
              name = "Shopping"
            },
            TodoListsReadModelEntity().apply {
              id = "list-2"
              name = "Work"
            })
    every { repository.findAll() } returns entities

    // WHEN
    val result = queryHandler.handleQuery(TodoListsReadModelQuery())

    // THEN
    assertNotNull(result)
    assertEquals(2, result!!.data.size)
    assertEquals("list-1", result.data[0].id)
    assertEquals("Shopping", result.data[0].name)
    assertEquals("list-2", result.data[1].id)
    assertEquals("Work", result.data[1].name)
  }
}
