package com.couchraoke.tv.domain.session

class ConnectionRegistry {

    data class Connection(
        val clientId: String,
        val deviceName: String,
        val connectionId: UShort,
        val httpPort: Int,
        val phoneIp: String,
        val playerSlot: String?,
    )

    private val connections = mutableMapOf<String, Connection>()
    private var counter: UShort = 0u

    val size: Int get() = connections.size

    fun register(
        clientId: String,
        deviceName: String,
        httpPort: Int,
        phoneIp: String,
        playerSlot: String? = null,
    ): UShort {
        connections.remove(clientId)
        counter = (counter + 1u).toUShort()
        connections[clientId] = Connection(clientId, deviceName, counter, httpPort, phoneIp, playerSlot)
        return counter
    }

    fun deregister(clientId: String) {
        connections.remove(clientId)
    }

    fun getByClientId(clientId: String): Connection? = connections[clientId]

    fun getByConnectionId(id: UShort): Connection? =
        connections.values.firstOrNull { it.connectionId == id }
}
