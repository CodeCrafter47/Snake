package codecrafter47.snake.protocol

import codecrafter47.snake.game.Direction
import codecrafter47.snake.game.Location
import codecrafter47.snake.game.User
import io.netty.buffer.ByteBuf
import java.awt.Color
import java.util.HashMap
import java.util.UUID


object Protocol {

    val packetIdToClassMap: MutableMap<Int, Class<out Packet>> = HashMap()
    val packetIdToDeserializerMap: MutableMap<Int, ByteBuf.() -> Packet> = HashMap()
    val packetClassToIdMap: MutableMap<Class<out Packet>, Int> = HashMap()

    private fun register(id: Int, clazz: Class<out Packet>, deserializer: ByteBuf.() -> Packet) {
        packetIdToClassMap.put(id, clazz)
        packetIdToDeserializerMap.put(id, deserializer)
        packetClassToIdMap.put(clazz, id)
    }

    init {
        register(1, javaClass<PacketJoin>(), { PacketJoin(readString()) })
        register(2, javaClass<PacketAddUser>(), { PacketAddUser(User(uuid = readUUID(), name = readString(), color = Color(readInt()))) })
        register(3, javaClass<PacketRemoveUser>(), { PacketRemoveUser(readUUID()) })
        register(4, javaClass<PacketChangeDirection>(), { PacketChangeDirection(Direction.values()[readInt()]) })
        register(5, javaClass<PacketDeath>(), { PacketDeath(readInt()) })
        register(6, javaClass<PacketHeadPosition>(), { PacketHeadPosition(readLocation()) })
        register(7, javaClass<PacketUpdateEntities>(), { PacketUpdateEntities(readList { readLocation() to Color(readInt()) }) })
        register(8, javaClass<PacketInit>(), { PacketInit(readInt(), readInt()) })
        register(9, javaClass<PacketUpdateUserPoints>(), { PacketUpdateUserPoints(readUUID(), readInt()) })
    }
}

abstract class Packet {
    abstract fun write(buf: ByteBuf)
}

class PacketJoin(val name: String) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeString(name)
    }
}

class PacketAddUser(val user: User) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeUUID(user.uuid)
        buf.writeString(user.name)
        buf.writeInt(user.color.getRGB())
    }
}

class PacketRemoveUser(val uuid: UUID) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeUUID(uuid)
    }
}

class PacketChangeDirection(val direction: Direction) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeInt(direction.ordinal())
    }
}

class PacketDeath(val finalPoints: Int) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeInt(finalPoints)
    }
}

class PacketHeadPosition(val location: Location) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeLocation(location)
    }
}

class PacketUpdateEntities(val update: List<Pair<Location, Color>>) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeList(update) {
            writeLocation(it.first)
            writeInt(it.second.getRGB())
        }
    }
}

class PacketInit(val width: Int, val height: Int) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeInt(width)
        buf.writeInt(height)
    }
}

class PacketUpdateUserPoints(val uuid: UUID, val points: Int) : Packet() {
    override fun write(buf: ByteBuf) {
        buf.writeUUID(uuid)
        buf.writeInt(points)
    }
}