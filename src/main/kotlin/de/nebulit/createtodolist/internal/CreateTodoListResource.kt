package de.nebulit.createtodolist.internal

import de.nebulit.domain.commands.createtodolist.CreateTodoListCommand
import java.util.concurrent.CompletableFuture
import mu.KotlinLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class CreateTodoListPayload(var id: String, var name: String)

/*
Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997293
*/
@RestController
class CreateTodoListResource(private var commandGateway: CommandGateway) {

  var logger = KotlinLogging.logger {}

  @CrossOrigin
  @PostMapping("/debug/createtodolist")
  fun processDebugCommand(
      @RequestParam id: String,
      @RequestParam name: String
  ): CompletableFuture<Any> {
    return commandGateway.send(CreateTodoListCommand(id, name))
  }

  @CrossOrigin
  @PostMapping("/createtodolist/{id}")
  fun processCommand(
      @PathVariable("id") id: String,
      @RequestBody payload: CreateTodoListPayload
  ): CompletableFuture<Any> {
    return commandGateway.send(CreateTodoListCommand(id = payload.id, name = payload.name))
  }
}
