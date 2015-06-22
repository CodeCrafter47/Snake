package codecrafter47.snake.client

import codecrafter47.snake.protocol.PacketDecoder
import codecrafter47.snake.protocol.PacketEncoder
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerAdapter
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

fun main(args: Array<String>) {
    val client = Client()
    client.start()
}