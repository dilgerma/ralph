package de.nebulit.todolists.internal

import de.nebulit.todolists.TodoListsReadModel
import de.nebulit.todolists.TodoListsReadModelQuery
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging
import org.axonframework.queryhandling.QueryGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/*
Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997363
*/
@RestController
class TodolistsResource(private var queryGateway: QueryGateway) {

  var logger = KotlinLogging.logger {}

  @CrossOrigin
  @GetMapping("/todolists")
  fun findReadModel(): CompletableFuture<TodoListsReadModel> {
    return queryGateway.query(TodoListsReadModelQuery(), TodoListsReadModel::class.java)
  }
}
