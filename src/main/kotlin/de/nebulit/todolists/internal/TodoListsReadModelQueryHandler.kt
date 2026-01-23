package de.nebulit.todolists.internal

import de.nebulit.todolists.TodoListsReadModel
import de.nebulit.todolists.TodoListsReadModelQuery
import org.axonframework.queryhandling.QueryHandler
import org.springframework.stereotype.Component

/*
Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997363
*/
@Component
class TodoListsReadModelQueryHandler(private val repository: TodoListsReadModelRepository) {

  @QueryHandler
  fun handleQuery(query: TodoListsReadModelQuery): TodoListsReadModel? {
    return TodoListsReadModel(repository.findAll())
  }
}
