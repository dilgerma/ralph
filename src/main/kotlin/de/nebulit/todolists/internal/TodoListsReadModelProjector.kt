package de.nebulit.todolists.internal

import de.nebulit.events.TodoListCreatedEvent
import de.nebulit.todolists.TodoListsReadModelEntity
import org.axonframework.eventhandling.EventHandler
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

interface TodoListsReadModelRepository : JpaRepository<TodoListsReadModelEntity, String>

/*
Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997363
*/
@Component
class TodoListsReadModelProjector(var repository: TodoListsReadModelRepository) {

  @EventHandler
  fun on(event: TodoListCreatedEvent) {
    // throws exception if not available (adjust logic)
    val entity = this.repository.findById(event.id).orElse(TodoListsReadModelEntity())
    entity
        .apply {
          id = event.id
          name = event.name
        }
        .also { this.repository.save(it) }
  }
}
