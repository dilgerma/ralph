package de.nebulit.domain

import de.nebulit.domain.commands.addtodoitem.AddTodoItemCommand
import de.nebulit.domain.commands.createtodolist.CreateTodoListCommand
import de.nebulit.events.AddItemEvent
import de.nebulit.events.TodoListCreatedEvent
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateCreationPolicy
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.CreationPolicy
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class TodoListAggregate {

  @AggregateIdentifier var id: String? = null

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
  fun handle(command: CreateTodoListCommand) {
    AggregateLifecycle.apply(TodoListCreatedEvent(id = command.id, name = command.name))
  }

  @EventSourcingHandler
  fun on(event: TodoListCreatedEvent) {
    // handle event
    id = event.id
  }

  /*
  //AI-TODO:

  # Spec Start
  Title: spec: Add Todo Item - max 3 items
  ### Given (Events):
    * 'Todo List created' (SPEC_EVENT)
  Fields:
   - name:
   - id:
    * 'Add Item' (SPEC_EVENT)
  Fields:
   - name:
   - listId:
   - id:
    * 'Add Item' (SPEC_EVENT)
  Fields:
   - name:
   - listId:
   - id:
    * 'Add Item' (SPEC_EVENT)
  Fields:
   - name:
   - listId:
   - id:
  ### When (Command):
    * 'Add Todo Item' (SPEC_COMMAND)
  Fields:
   - name:
   - listId:
   - id:
  ### Then:
    * 'only 3 items allowed' (SPEC_ERROR)
  # Spec End

  # Spec Start
  Title: spec: Add Todo Item - scenario
  ### Given (Events):
    * 'Todo List created' (SPEC_EVENT)
  Fields:
   - name:
   - id:
    * 'Add Item' (SPEC_EVENT)
  Fields:
   - name:
   - listId:
   - id:
  ### When (Command):
    * 'Add Todo Item' (SPEC_COMMAND)
  Fields:
   - name:
   - listId:
   - id:
  ### Then:
    * 'Add Item' (SPEC_EVENT)
  Fields:
   - name:
   - listId:
   - id:
  # Spec End
          */

  @CommandHandler
  fun handle(command: AddTodoItemCommand) {

    AggregateLifecycle.apply(
        AddItemEvent(name = command.name, listId = command.listId, id = command.id))
  }

  @EventSourcingHandler
  fun on(event: AddItemEvent) {
    // handle event
    id = event.id
  }
}
