package codecrafter47.snake.server

import codecrafter47.snake.game.*
import codecrafter47.snake.protocol.*
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadLocalRandom
import kotlin.properties.Delegates

private val aiPlayerNames = listOf("Alice", "Bob", "Thomas", "47", "Spock", "R2D2", "C3PO", "Darth Vader", "anonymous",
        "Einstein", "Java", "Kotlin", "C++", "Python", "HTML", "Vala", "Scala", "Lua", "JavaScript", "C", "C#", "Bot")

private fun getRandomAIName(): String = aiPlayerNames.get(ThreadLocalRandom.current().nextInt(aiPlayerNames.size()))

class Server(val host: String, val port: Int, width: Int, height: Int) : Game(width, height) {
    override val userList: UserList = ServerUserList(this)
    private var channelFuture: ChannelFuture by Delegates.notNull()
    private val clients: MutableMap<UUID, Channel> = Collections.synchronizedMap(LinkedHashMap())
    private val players: MutableSet<Player> = Collections.synchronizedSet(LinkedHashSet())
    private var changes: MutableList<Pair<Location, Color>> = Collections.synchronizedList(ArrayList())

    private val tasks: Queue<() -> Unit> = ConcurrentLinkedQueue()

    override fun setEntityAt(location: Location, entity: Entity): Boolean {
        changes.add(location to entity.color)
        return super.setEntityAt(location, entity)
    }

    public fun update() {
        while (!tasks.isEmpty()) {
            tasks.remove()?.invoke()
        }
        while(players.size() < 35){
            AIPlayer(this, User(UUID.randomUUID(), getRandomAIName(), 1))
        }
        players.toList().forEach { it.update() }
        sendPacketToAll(PacketUpdateEntities(changes.toList()))
        changes.clear()
    }

    public fun start() {
        System.out.println("starting server")
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup).channel(javaClass<NioServerSocketChannel>())
                    .childHandler(object : ChannelInitializer<SocketChannel>() {
                        throws(Exception::class)
                        override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(PacketDecoder(), UserConnection(), PacketEncoder())
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)

            // Bind and start to accept incoming connections.
            channelFuture = b.bind(host, port).sync()

            // Wait until the server socket is closed.
            channelFuture.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
        System.out.println("stopped server")
    }

    public fun stop() {
        channelFuture.channel().close()
    }

    public fun sendPacketToAll(packet: Packet) {
        clients.values().forEach { it.writeAndFlush(packet) }
    }

    inner class UserConnection : ChannelHandlerAdapter() {
        val uuid = UUID.randomUUID()
        var user: User? = null
        var player: Player? = null

        override fun channelActive(ctx: ChannelHandlerContext) {
            clients.put(uuid, ctx.channel())
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg !is Packet) {
                throw IllegalArgumentException("Received data of unknown type ${msg.javaClass.getName()}")
            }
            val channel = ctx.channel()
            when (msg) {
                is PacketJoin -> tasks.add {
                    if (user != null) throw IllegalStateException("User $user already identified himself")
                    val user = User(uuid, msg.name, 1)
                    this.user = user
                    userList.addUser(user)
                    val player = object : Player(this@Server, user) {
                        override fun update() {
                            if (!isAlive) {
                                channel.writeAndFlush(PacketDeath(this@UserConnection.user?.points ?: -1))
                                unregisterPlayer()
                            }
                            super.update()
                            if(isAlive)channel.writeAndFlush(PacketHeadPosition(myParts.get(0)))
                        }
                    }
                    players.add(player)
                    channel.writeAndFlush(PacketInit(width, heigth))
                    channel.writeAndFlush(PacketUpdateEntities((0..width-1).flatMap { x -> (0..heigth-1).map { y -> Location(x, y) } } filter { getEntityAt(it) !is NullEntity } map { it to getEntityAt(it)!!.color}))
                    channel.writeAndFlush(PacketHeadPosition(player.myParts.get(0)))
                    this.player = player
                    userList.getUsers().forEach { channel.writeAndFlush(PacketAddUser(it)) }
                }
                is PacketChangeDirection -> {
                    player?.direction = msg.direction
                }
                else -> throw IllegalArgumentException("Unexpected packet of type ${msg.javaClass.getName()}: $msg")
            }
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            unregisterPlayer()
            clients.remove(uuid)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            super.exceptionCaught(ctx, cause)
        }

        private fun unregisterPlayer() {
            userList.removeUser(uuid)
            var player = this.player
            if (player != null){
                players.remove(player)
                player.kill()
            }
            user = null
            this.player = null
        }
    }

    open class Player(val game: Server, val user: User) {
        val myParts: MutableList<Location> = arrayListOf()
        var direction = Direction.RIGHT
        var isAlive = true
        val PlayerEntity = Entity(user.color)

        init {
            var safeLocation = game.getRandomLocation()
            while (game.getEntityAt(safeLocation) != NullEntity) {
                safeLocation = game.getRandomLocation()
            }
            myParts.add(safeLocation)
            game.setEntityAt(myParts.get(0), PlayerEntity)
        }

        open fun update() {
            if (!isAlive) {
                kill()
                return
            }
            val head = myParts.get(0)
            val newHeadPosition = head.offset(direction)
            if (!game.contains(newHeadPosition)) {
                isAlive = false
                return
            }
            val eatenPiece = game.getEntityAt(newHeadPosition)
            if (eatenPiece != EatablePiece && eatenPiece != NullEntity) {
                isAlive = false
                return
            }
            if (eatenPiece != EatablePiece) {
                val clear = myParts.remove(myParts.size() - 1)
                game.setEntityAt(clear, NullEntity)
            }
            myParts.add(0, newHeadPosition)
            game.setEntityAt(newHeadPosition, PlayerEntity)
            if(eatenPiece == EatablePiece){
                grow()
            }
        }

        fun kill() {
            myParts.forEach { game.setEntityAt(it, NullEntity) }
            myParts.clear()
            isAlive = false
        }

        fun grow(){
            user.points = myParts.size()
            game.sendPacketToAll(PacketUpdateUserPoints(user.uuid, user.points))
        }
    }

    class AIPlayer(game: Server, user: User): Player(game, user){
        init {
            game.userList.addUser(user)
            game.players.add(this)
        }

        override fun update() {
            if(!isAlive){
                game.players.remove(this)
                game.userList.removeUser(user.uuid)
            } else {
                val head = myParts.get(0)
                var possibleDirections: MutableList<Direction> = Direction.values().toArrayList()
                if (head.x == 0) {
                    possibleDirections.remove(Direction.LEFT)
                }
                if (head.y == 0) {
                    possibleDirections.remove(Direction.UP)
                }
                if (head.x == game.width - 1) {
                    possibleDirections.remove(Direction.RIGHT)
                }
                if (head.y == game.heigth - 1) {
                    possibleDirections.remove(Direction.DOWN)
                }
                possibleDirections = possibleDirections.filter { game.getEntityAt(head.offset(it)) == NullEntity || game.getEntityAt(head.offset(it)) == EatablePiece }.toArrayList()
                if (!possibleDirections.contains(direction) && !possibleDirections.isEmpty()) {
                    direction = possibleDirections.get(ThreadLocalRandom.current().nextInt(possibleDirections.size()))
                }
            }
            super.update()
        }
    }
}