package com.couchraoke.tv.domain.session

import com.couchraoke.tv.domain.network.protocol.AssignSingerMessage
import com.couchraoke.tv.domain.network.protocol.PlaybackStateMessage
import com.couchraoke.tv.domain.network.protocol.SlotInfo
import com.couchraoke.tv.domain.network.protocol.SlotMap

data class PlaybackReplaySnapshot(
    val assignSinger: AssignSingerMessage,
    val playbackState: PlaybackStateMessage,
)

interface ISessionGate {
    val isLocked: Boolean
    val sessionId: String
    val maxConnections: Int
    val slots: SlotMap
    val inSong: Boolean

    fun getPlaybackReplaySnapshot(clientId: String): PlaybackReplaySnapshot? = null
}
