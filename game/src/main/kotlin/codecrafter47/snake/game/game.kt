package codecrafter47.snake.game

import codecrafter47.snake.game.UserList
import java.awt.Color
import java.awt.Graphics



open class Game(val width: Int, val heigth: Int){

    private val parts = Array(width) {i -> Array<Entity>(heigth) {i -> NullEntity}}

    public open val userList: UserList = UserList()

    public fun contains(location: Location): Boolean = location.x >= 0 && location.x < width && location.y >= 0 && location.y < heigth

    public fun getEntityAt(location: Location): Entity? = if (contains(location)) parts[location.x][location.y] else null

    public open fun setEntityAt(location: Location, entity: Entity): Boolean{
        if(!contains(location))return false
        if(entity == getEntityAt(location))return false
        parts[location.x][location.y] = entity
        return true
    }
}

open class Entity(val color: Color)

object NullEntity: Entity(Color.BLACK)

object EatablePiece: Entity(Color.GREEN)

data class Location(val x: Int, val y: Int){
    fun offset(direction: Direction): Location = when (direction) {
        Direction.UP -> Location(x, y - 1)
        Direction.DOWN -> Location(x, y + 1)
        Direction.LEFT -> Location(x - 1, y)
        Direction.RIGHT -> Location(x + 1, y)
        else -> throw IllegalArgumentException()
    }
}

enum class Direction{
    UP, DOWN, LEFT, RIGHT
}