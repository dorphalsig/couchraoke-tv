package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty

data class ConnectedPhone(
    val clientId: String,
    val connectionId: UShort,
    val deviceName: String,
    val httpPort: Int,
    val ipAddress: String,
)

data class SongEntry(
    val relativeTxtPath: String,
    val modifiedTimeMs: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val year: Int?,
    val genre: String?,
    val isDuet: Boolean,
    val hasRap: Boolean,
    val hasVideo: Boolean,
    val hasInstrumental: Boolean,
    val canMedley: Boolean,
    val medleySource: String?,
    val medleyStartBeat: Int?,
    val medleyEndBeat: Int?,
    val startSec: Float,
    val previewStartSec: Float,
    val txtUrl: String,
    val audioUrl: String,
    val videoUrl: String?,
    val coverUrl: String?,
    val backgroundUrl: String?,
)

data class SessionState(
    val sessionId: String,
    val sessionToken: String,
    val joinCode: String,
    val connectedPhones: List<ConnectedPhone>,
    val isLocked: Boolean,
)

sealed interface PhoneEvent {
    val clientId: String

    data class Connected(
        val phone: ConnectedPhone,
    ) : PhoneEvent {
        override val clientId: String = phone.clientId
    }

    data class Disconnected(
        override val clientId: String,
        val wasAssignedSinger: Boolean,
    ) : PhoneEvent

    data class Reconnected(
        val phone: ConnectedPhone,
        val wasAssignedSinger: Boolean,
    ) : PhoneEvent {
        override val clientId: String = phone.clientId
    }
}

sealed interface NetworkMessage {
    val type: String
    val protocolVersion: Int
    val sessionId: String
}

data class AssignSingerMessage(
    override val sessionId: String,
    val songInstanceSeq: Long,
    val playerId: PlayerId,
    val difficulty: Difficulty,
    val startMode: StartMode,
    val countdownMs: Int?,
    val stopAtLyricsTimeMs: Long,
    val udpPort: Int,
    val songTitle: String,
    val songArtist: String,
) : NetworkMessage {
    override val type: String = "assignSinger"
    override val protocolVersion: Int = 1
}

data class PlaybackStateMessage(
    override val sessionId: String,
    val songInstanceSeq: Long,
    val revision: Long,
    val state: PlaybackNetworkState,
    val lyricsTimeMs: Long,
    val stopAtLyricsTimeMs: Long,
    val reason: String,
) : NetworkMessage {
    override val type: String = "playbackState"
    override val protocolVersion: Int = 1
}

data class ClockAckMessage(
    override val sessionId: String,
    val tvTimeMs: Long,
    val phoneTimeMs: Long,
    val roundTripMs: Long,
) : NetworkMessage {
    override val type: String = "clockAck"
    override val protocolVersion: Int = 1
}

data class PongResponse(
    val phoneId: String,
    val phoneTimeMs: Long,
    val tvReceiveTimeMs: Long,
    val isValidSample: Boolean,
)

enum class StartMode(val wireValue: String) {
    Countdown("countdown"),
    Live("live"),
}

enum class PlaybackNetworkState(val wireValue: String) {
    Open("open"),
    Countdown("countdown"),
    Live("live"),
    Paused("paused"),
    Stopped("stopped"),
    Error("error"),
}

fun SongEntry.toIndexedSong(phoneClientId: String): IndexedSong = IndexedSong(
    songId = "$phoneClientId:$relativeTxtPath:$modifiedTimeMs",
    phoneClientId = phoneClientId,
    relativeTxtPath = relativeTxtPath,
    modifiedTimeMs = modifiedTimeMs,
    title = title,
    artist = artist,
    album = album,
    year = year,
    genre = genre,
    txtUrl = txtUrl,
    audioUrl = audioUrl,
    videoUrl = videoUrl,
    coverUrl = coverUrl,
    backgroundUrl = backgroundUrl,
    isDuet = isDuet,
    hasRap = hasRap,
    hasVideo = hasVideo,
    canMedley = canMedley,
    medleySource = medleySource,
    medleyStartBeat = medleyStartBeat,
    medleyEndBeat = medleyEndBeat,
    startSec = startSec,
    previewStartSec = previewStartSec,
)
