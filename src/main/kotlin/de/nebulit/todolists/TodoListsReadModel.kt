package de.nebulit.todolists

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id

class TodoListsReadModelQuery()

/*
Boardlink: https://miro.com/app/board/uXjVJ13p-9M=/?moveToWidget=3458764656615997363
*/
@Entity
class TodoListsReadModelEntity {
  @Id @Column(name = "id") var id: String? = null

  @Column(name = "name") var name: String? = null
}

data class TodoListsReadModel(val data: List<TodoListsReadModelEntity>)
