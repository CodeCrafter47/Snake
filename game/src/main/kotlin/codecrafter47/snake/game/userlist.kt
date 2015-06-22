package codecrafter47.snake.game

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

open class UserList() {
    private val users: MutableList<User> = CopyOnWriteArrayList()
    private val uuidToUserMap: MutableMap<UUID, User> = ConcurrentHashMap()

    public fun getUser(uuid: UUID): User? {
        return uuidToUserMap.get(uuid)
    }

    public fun getUsers(): List<User> {
        return users
    }

    public open fun addUser(user: User) {
        users.add(user)
        uuidToUserMap.put(user.uuid, user)
    }

    public open fun removeUser(uuid: UUID): Boolean{
        val user = uuidToUserMap.get(uuid)
        if(user != null){
            users.remove(user)
            uuidToUserMap.remove(user.uuid)
            return true
        }
        return false
    }
}