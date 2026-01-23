package de.nebulit.domain.commands.createtodolist

import de.nebulit.common.Command
import org.axonframework.modelling.command.TargetAggregateIdentifier

/*
Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997293
*/
data class CreateTodoListCommand(@TargetAggregateIdentifier var id: String, var name: String) :
    Command
