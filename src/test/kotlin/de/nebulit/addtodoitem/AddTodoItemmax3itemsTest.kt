package de.nebulit.addtodoitem

import de.nebulit.common.CommandException
import de.nebulit.common.Event
import de.nebulit.common.support.RandomData
import de.nebulit.domain.TodoListAggregate
import de.nebulit.domain.commands.addtodoitem.AddTodoItemCommand
import de.nebulit.events.AddItemEvent
import de.nebulit.events.TodoListCreatedEvent
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656616236755
 *
 * TODO: Enable when slice "Add Todo Item" (index 5) is implemented with max 3 items business rule
 */
@Disabled("Business rule not implemented yet - enable when slice 'Add Todo Item' is done")
class AddTodoItemmax3itemsTest {

  private lateinit var fixture: FixtureConfiguration<TodoListAggregate>

  @BeforeEach
  fun setUp() {
    fixture = AggregateTestFixture(TodoListAggregate::class.java)
  }

  @Test
  fun `Add Todo Itemmax3items Test`() {

    var aggregateId: java.util.UUID = RandomData.newInstance<java.util.UUID> {}

    // GIVEN
    val events = mutableListOf<Event>()

    events.add(
        RandomData.newInstance<TodoListCreatedEvent> {
          name = RandomData.newInstance {}
          id = RandomData.newInstance {}
        })
    events.add(
        RandomData.newInstance<AddItemEvent> {
          name = RandomData.newInstance {}
          listId = RandomData.newInstance {}
          id = RandomData.newInstance {}
        })
    events.add(
        RandomData.newInstance<AddItemEvent> {
          name = RandomData.newInstance {}
          listId = RandomData.newInstance {}
          id = RandomData.newInstance {}
        })
    events.add(
        RandomData.newInstance<AddItemEvent> {
          name = RandomData.newInstance {}
          listId = RandomData.newInstance {}
          id = RandomData.newInstance {}
        })

    // WHEN
    val command =
        AddTodoItemCommand(
            name = RandomData.newInstance {},
            listId = RandomData.newInstance {},
            id = RandomData.newInstance {})

    // THEN
    val expectedEvents = mutableListOf<Event>()

    fixture.given(events).`when`(command).expectException(CommandException::class.java)
  }
}
