package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.network.protocol.SlotInfo
import com.couchraoke.tv.domain.network.protocol.SlotMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class Session(
    private val connectionCloser: IConnectionCloser? = null,
) : ISessionGate, ISessionCallbacks {

    var state: SessionState = SessionState.Open
        private set

    private val token: String = SessionToken.generate()
    private val displayNames: MutableMap<String, String> = mutableMapOf()
    private val assignedSlots: MutableMap<String, String> = mutableMapOf()
    private var activeSourceClientId: String? = null
    private val connectedClientIds: MutableSet<String> = mutableSetOf()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    // ISessionGate
    override val isLocked: Boolean get() = state == SessionState.Locked
    override val sessionId: String get() = token
    override val maxConnections: Int = MAX_CONNECTIONS
    override val inSong: Boolean get() = state == SessionState.Locked
    override val slots: SlotMap get() = SlotMap(
        P1 = slotInfoFor("P1"),
        P2 = slotInfoFor("P2"),
    )

    private fun slotInfoFor(slot: String): SlotInfo {
        val clientId = assignedSlots.entries.firstOrNull { it.value == slot }?.key
        return SlotInfo(
            connected = clientId != null && connectedClientIds.contains(clientId),
            deviceName = clientId?.let { displayNames[it] } ?: "",
        )
    }

    // ISessionCallbacks
    override fun onPhoneConnected(clientId: String, deviceName: String, connectionId: UShort) {
        connectedClientIds.add(clientId)
        if (!displayNames.containsKey(clientId)) {
            displayNames[clientId] = deviceName
        }
        _events.tryEmit(SessionEvent.PhoneConnected(clientId, deviceName))
    }

    override fun onPhoneDisconnected(clientId: String) {
        connectedClientIds.remove(clientId)
        val slot = assignedSlots[clientId]
        if (slot != null && state == SessionState.Locked) {
            _events.tryEmit(SessionEvent.RequiredSingerDisconnected(clientId, slot))
        } else {
            _events.tryEmit(SessionEvent.SpectatorDisconnected(clientId))
        }
        if (clientId == activeSourceClientId) {
            _events.tryEmit(SessionEvent.PlaybackSourceLost(clientId))
        }
        _events.tryEmit(SessionEvent.PhoneDisconnected(clientId))
    }

    override fun onPhoneReconnected(clientId: String, newConnectionId: UShort) {
        connectedClientIds.add(clientId)
        val wasSinger = assignedSlots.containsKey(clientId)
        _events.tryEmit(SessionEvent.PhoneReconnected(clientId, wasSinger))
    }

    fun lockForSong(
        p1ClientId: String,
        p2ClientId: String? = null,
        sourceClientId: String? = null,
    ) {
        if (state != SessionState.Open) return
        state = SessionState.Locked
        assignedSlots[p1ClientId] = "P1"
        if (p2ClientId != null) assignedSlots[p2ClientId] = "P2"
        activeSourceClientId = sourceClientId
    }

    fun unlockAfterSong() {
        if (state != SessionState.Locked) return
        state = SessionState.Open
        assignedSlots.clear()
        activeSourceClientId = null
    }

    fun endSession() {
        if (state == SessionState.Ended) return
        state = SessionState.Ended
        val allClients = displayNames.keys.toList()
        for (clientId in allClients) {
            connectionCloser?.closeConnection(clientId)
            _events.tryEmit(SessionEvent.PhoneDisconnected(clientId))
        }
        displayNames.clear()
        assignedSlots.clear()
        activeSourceClientId = null
        connectedClientIds.clear()
    }

    fun rename(clientId: String, newName: String) {
        if (!displayNames.containsKey(clientId)) return
        displayNames[clientId] = newName
        _events.tryEmit(SessionEvent.RosterChanged(clientId))
    }

    fun kick(clientId: String) {
        if (!displayNames.containsKey(clientId)) return
        connectionCloser?.closeConnection(clientId)
        connectedClientIds.remove(clientId)
        assignedSlots.remove(clientId)
    }

    fun forget(clientId: String) {
        kick(clientId)
        displayNames.remove(clientId)
    }

    fun releaseSlot(clientId: String) {
        assignedSlots.remove(clientId)
    }

    companion object {
        private const val MAX_CONNECTIONS = 10
    }
}
