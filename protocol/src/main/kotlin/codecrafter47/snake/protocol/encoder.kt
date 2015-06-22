package codecrafter47.snake.protocol

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerAdapter
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise

class PacketEncoder: ChannelHandlerAdapter() {

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if(msg is Packet) {
            val buf = ctx.alloc().buffer()
            msg.write(buf)
            val out = ctx.alloc().buffer(buf.writerIndex() + 8)
            out.writeInt(buf.writerIndex())
            out.writeInt(Protocol.packetClassToIdMap.get(msg.javaClass) ?: throw IllegalArgumentException("Unknown packet type ${msg.javaClass.getName()} of $msg"))
            out.writeBytes(buf)
            buf.release()
            ctx.write(out, promise)
        } else {
            super.write(ctx, msg, promise)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        super.exceptionCaught(ctx, cause)
    }
}