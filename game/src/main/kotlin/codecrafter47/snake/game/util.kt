package codecrafter47.snake.game

import codecrafter47.snake.game.Game
import codecrafter47.snake.game.Location
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom

private val colors = listOf(Color.blue, Color.cyan, Color.magenta, Color.orange, Color.pink, Color.red, Color.yellow)

fun randomColor(): Color = colors.get(ThreadLocalRandom.current().nextInt(colors.size()))

fun Game.getRandomLocation(): Location = Location(ThreadLocalRandom.current().nextInt(width), ThreadLocalRandom.current().nextInt(heigth))