package codecrafter47.snake.client

import codecrafter47.snake.game.Direction
import codecrafter47.snake.game.Entity
import codecrafter47.snake.game.Game
import codecrafter47.snake.game.Location
import codecrafter47.snake.protocol.*
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.*

class Client {
    var channelFuture: ChannelFuture? = null
    val frame = JFrame("Snake-Client")
    var game = Game(100, 100)
    var selectNamePanel = JPanel()
    var paintLock = Any()
    var gamePanel = object : JPanel(true) {
        override fun paintComponent(g: Graphics) {
            try {
                synchronized(paintLock) {
                    with(g) {

                        fun Entity.render(x: Int, y: Int) {
                            setColor(color)
                            fillRect(x * blockSize + offsetX, y * blockSize + offsetY, blockSize, blockSize)
                        }

                        setColor(Color.DARK_GRAY)
                        fillRect(0, 0, getWidth(), getHeight())

                        for (x in 0..game.width - 1) {
                            for (y in 0..game.heigth - 1) {
                                game.getEntityAt(Location(x, y))?.render(x, y)
                            }
                        }

                        setColor(Color.DARK_GRAY)
                        fillRect(getWidth() - 100, 0, 100, 200)
                        setColor(Color.WHITE)
                        drawString("Top 10", getWidth() - 70, 15)
                        var num = 0
                        game.userList.getUsers() sortDescendingBy { user -> user.points } forEach {
                            setColor(it.color)
                            if (++num <= 10) drawString("${it.name} - ${it.points}", getWidth() - 95, 13 + num * 17)
                        }
                    }
                }
            } catch(th: Throwable) {
                th.printStackTrace()
            }
        }
    }
    var blockSize = 50
    var offsetX = 1
    var offsetY = 1
    val executor = Executors.newScheduledThreadPool(4)
    val input = JTextField("Spieler", 35)

    fun start() {
        with(selectNamePanel) {
            setLayout(FlowLayout())
            add(JLabel("Wähle deinen Namen"))
            add(input)
            val button = JButton("Start")
            button.addActionListener {
                val channel = channelFuture?.channel()
                if (channel == null) {
                    add(JLabel("Connection lost. Please restart the game."))
                    invalidate()
                } else {
                    channel.writeAndFlush(PacketJoin(input.getText()))
                }
            }
            add(button)
        }

        frame.setSize(1000, 600)
        frame.add(selectNamePanel)
        frame.setVisible(true)
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

        frame.addKeyListener(object : KeyListener {
            override fun keyReleased(e: KeyEvent?) {
                // do nothing
            }

            override fun keyPressed(e: KeyEvent) {
                when (e.getKeyCode()) {
                    KeyEvent.VK_UP -> channelFuture?.channel()?.writeAndFlush(PacketChangeDirection(Direction.UP))
                    KeyEvent.VK_DOWN -> channelFuture?.channel()?.writeAndFlush(PacketChangeDirection(Direction.DOWN))
                    KeyEvent.VK_LEFT -> channelFuture?.channel()?.writeAndFlush(PacketChangeDirection(Direction.LEFT))
                    KeyEvent.VK_RIGHT -> channelFuture?.channel()?.writeAndFlush(PacketChangeDirection(Direction.RIGHT))
                }
            }

            override fun keyTyped(e: KeyEvent) {
                // do nothing
            }

        })

        executor.scheduleAtFixedRate({
            try {
                //frame.invalidate()
                frame.repaint()
                //gamePanel.repaint()
            } catch(th: Throwable) {
                th.printStackTrace()
            }
        }, 10, 10, TimeUnit.MILLISECONDS)

        Runtime.getRuntime().addShutdownHook(Thread() {
            channelFuture?.channel()?.close()
            executor.shutdown()
        })

        connect("snake.codecrafter47.dyndns.eu", 31289)
    }

    var pausedPacket: (() -> Unit)? = null

    fun handlePacket(packet: Packet) {
        when (packet) {
            is PacketInit -> {
                game = Game(packet.width, packet.height)
                executor.execute {
                    frame.remove(selectNamePanel)
                    frame.add(gamePanel)
                    //frame.setVisible(false)
                    frame.setVisible(true)
                    frame.requestFocus()
                    frame.invalidate()
                    frame.repaint()
                }
            }
            is PacketHeadPosition -> pausedPacket = {
                val (x, y) = packet.location
                val user = (game.userList.getUsers() filter {user -> user.name == input.getText()}).firstOrNull()
                if(user != null){
                    if(user.points < 10){
                        blockSize = 50
                    } else if(user.points < 50){
                        blockSize = 55 - user.points / 2
                    } else if (user.points < 130){
                        blockSize = 30 - (user.points - 50) / 4
                    } else {
                        blockSize = 10
                    }
                } else {
                    blockSize = 50
                }
                offsetX = -(x * blockSize - gamePanel.getWidth() / 2)
                offsetY = -(y * blockSize - gamePanel.getHeight() / 2)
            }
            is PacketUpdateEntities -> {
                synchronized(paintLock) {
                    pausedPacket?.invoke()
                    packet.update.forEach {
                        game.setEntityAt(it.first, Entity(it.second))
                    }
                }
            }
            is PacketDeath -> {
                executor.execute {
                    frame.getContentPane().removeAll()
                    frame.add(selectNamePanel)
                    selectNamePanel.add(JLabel("You lost. Points: ${packet.finalPoints}"))
                    selectNamePanel.invalidate()
                    frame.invalidate()
                }
            }
            is PacketAddUser -> {
                game.userList.addUser(packet.user)
            }
            is PacketRemoveUser -> {
                game.userList.removeUser(packet.uuid)
            }
            is PacketUpdateUserPoints -> {
                game.userList.getUser(packet.uuid)?.points = packet.points
            }
            else -> throw IllegalArgumentException("Packet of type ${packet.javaClass.getName()} should not be sent to the client")
        }
    }

    fun connect(host: String, port: Int) {
        val workerGroup = NioEventLoopGroup()

        try {
            val b = Bootstrap()
            b.group(workerGroup)
            b.channel(javaClass<NioSocketChannel>())
            b.option(ChannelOption.SO_KEEPALIVE, true)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                throws(Exception::class)
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(PacketDecoder(), object : ChannelHandlerAdapter() {
                        throws(Exception::class)
                        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                            if (msg is Packet) {
                                handlePacket(msg)
                            } else {
                                throw IllegalArgumentException("Received Packet is of unknown type ${msg.javaClass.getName()}")
                            }
                        }

                        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                            cause.printStackTrace()
                            super.exceptionCaught(ctx, cause)
                        }
                    }, PacketEncoder())
                }
            })

            // Start the client.
            val channelFuture = b.connect(host, port).sync()
            this.channelFuture = channelFuture

            // Wait until the connection is closed.
            channelFuture.channel().closeFuture().sync()
            this.channelFuture = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } finally {
            workerGroup.shutdownGracefully()
        }
    }
}