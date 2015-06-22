package codecrafter47.snake.game

import java.awt.Color
import java.util.UUID

data class User(val uuid: UUID, val name: String, var points: Int = 0, var color: Color = randomColor())