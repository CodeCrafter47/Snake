package codecrafter47.snake.protocol

import codecrafter47.snake.game.Location
import io.netty.buffer.ByteBuf
import java.util.ArrayList
import java.util.UUID

fun ByteBuf.writeString(str: String) {
    val bytes = str.toByteArray(Charsets.UTF_8)
    writeInt(bytes.size())
    writeBytes(bytes)
}

fun ByteBuf.readString(): String {
    val size = readInt()
    val bytes = ByteArray(size)
    readBytes(bytes)
    return String(bytes, Charsets.UTF_8)
}

fun ByteBuf.writeUUID(uuid: UUID) {
    writeString(uuid.toString())
}

fun ByteBuf.readUUID(): UUID = UUID.fromString(readString())!!

fun ByteBuf.writeLocation(location: Location) {
    writeInt(location.x)
    writeInt(location.y)
}

fun ByteBuf.readLocation(): Location = Location(readInt(), readInt())

fun <T> ByteBuf.writeList(list: List<T>, serialize: ByteBuf.(T) -> Unit) {
    writeInt(list.size())
    list.forEach { serialize(it) }
}

fun <T> ByteBuf.readList(deserialize: ByteBuf.() -> T): List<T> {
    val size = readInt()
    val list: MutableList<T> = ArrayList()
    for (i in 1..size) {
        list.add(deserialize())
    }
    return list
}