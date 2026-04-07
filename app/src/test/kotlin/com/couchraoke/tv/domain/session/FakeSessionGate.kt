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
) : ISessionGate {
    val playbackReplaySnapshots = mutableMapOf<String, PlaybackReplaySnapshot>()

    override fun getPlaybackReplaySnapshot(clientId: String): PlaybackReplaySnapshot? =
        playbackReplaySnapshots[clientId]
}
