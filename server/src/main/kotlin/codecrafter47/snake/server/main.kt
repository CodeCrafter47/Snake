package codecrafter47.snake.server

import codecrafter47.snake.game.EatablePiece
import codecrafter47.snake.game.NullEntity
import codecrafter47.snake.game.getRandomLocation
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val server = Server("0.0.0.0", 31289, 500, 500)

    val executor = Executors.newScheduledThreadPool(4)
    executor.schedule({ server.start() }, 0, TimeUnit.NANOSECONDS)

    executor.scheduleAtFixedRate({ try{server.update()}catch(th: Throwable){th.printStackTrace()} }, 150, 150, TimeUnit.MILLISECONDS)

    executor.scheduleAtFixedRate({
        val location = server.getRandomLocation()
        if(server.getEntityAt(location) == NullEntity){
            server.setEntityAt(location, EatablePiece)
        }
    }, 200, 200, TimeUnit.MILLISECONDS)

    Runtime.getRuntime().addShutdownHook(Thread() {
        server.stop()
        executor.shutdown()
    })
}