package codecrafter47.snake.server

import codecrafter47.snake.game.User
import codecrafter47.snake.game.UserList
import codecrafter47.snake.protocol.PacketAddUser
import codecrafter47.snake.protocol.PacketRemoveUser
import java.util.UUID

class ServerUserList(val server: Server): UserList() {
    override fun addUser(user: User) {
        super.addUser(user)
        server.sendPacketToAll(PacketAddUser(user))
    }

    override fun removeUser(uuid: UUID): Boolean {
        val success = super.removeUser(uuid)
        if(success){
            server.sendPacketToAll(PacketRemoveUser(uuid))
        }
        return success
    }
}