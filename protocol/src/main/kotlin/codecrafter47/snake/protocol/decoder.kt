package codecrafter47.snake.protocol

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder

class PacketDecoder: ReplayingDecoder<Void>() {
    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        if (buf.readableBytes() < 8) {
            return
        }

        val lenght = buf.readInt()
        val id = buf.readInt()

        if (buf.readableBytes() < lenght) {
            return
        }

        val deserializer = Protocol.packetIdToDeserializerMap.get(id) ?: throw IllegalArgumentException("Received Packet with an unknown id: id=$id, size=${lenght+8}")
        val packet = buf.deserializer()

        out.add(packet)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        super.exceptionCaught(ctx, cause)
    }
}