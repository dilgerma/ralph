package de.nebulit.events

import de.nebulit.common.Event

/*
Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997217
*/
data class TodoListCreatedEvent(var name: String, var id: String) : Event
