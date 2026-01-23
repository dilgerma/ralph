package de.nebulit.createtodolist

import de.nebulit.common.Event
import de.nebulit.common.support.RandomData
import de.nebulit.domain.TodoListAggregate
import de.nebulit.domain.commands.createtodolist.CreateTodoListCommand
import de.nebulit.events.TodoListCreatedEvent
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test for Create Todo List slice
 *
 * Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997335
 */
class CreateTodoListTest {

  private lateinit var fixture: FixtureConfiguration<TodoListAggregate>

  @BeforeEach
  fun setUp() {
    fixture = AggregateTestFixture(TodoListAggregate::class.java)
  }

  @Test
  fun `Create Todo List creates a new todo list`() {
    // GIVEN
    val events = mutableListOf<Event>()

    // WHEN
    val command =
        CreateTodoListCommand(id = RandomData.newInstance {}, name = RandomData.newInstance {})

    // THEN
    val expectedEvents = mutableListOf<Event>()
    expectedEvents.add(TodoListCreatedEvent(id = command.id, name = command.name))

    fixture
        .givenNoPriorActivity()
        .`when`(command)
        .expectSuccessfulHandlerExecution()
        .expectEvents(*expectedEvents.toTypedArray())
  }
}
