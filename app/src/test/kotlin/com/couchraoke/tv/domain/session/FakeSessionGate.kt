package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.network.protocol.SlotInfo
import com.couchraoke.tv.domain.network.protocol.SlotMap

class FakeSessionGate(
    override var isLocked: Boolean = false,
    override var sessionId: String = "test-session",
    override var maxConnections: Int = 10,
    override var slots: SlotMap = SlotMap(
        P1 = SlotInfo(connected = false, deviceName = ""),
        P2 = SlotInfo(connected = false, deviceName = "")
    ),
    override var inSong: Boolean = false,
) : ISessionGate, ISessionCallbacks {
    override fun onPhoneConnected(clientId: String, deviceName: String, connectionId: UShort) = Unit
    override fun onPhoneDisconnected(clientId: String) = Unit
    override fun onPhoneReconnected(clientId: String, newConnectionId: UShort) = Unit
}
