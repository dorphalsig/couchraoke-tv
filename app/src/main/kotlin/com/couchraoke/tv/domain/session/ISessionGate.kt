package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.network.protocol.SlotInfo
import com.couchraoke.tv.domain.network.protocol.SlotMap

interface ISessionGate {
    val isLocked: Boolean
    val sessionId: String
    val maxConnections: Int
    val slots: SlotMap
    val inSong: Boolean
}
