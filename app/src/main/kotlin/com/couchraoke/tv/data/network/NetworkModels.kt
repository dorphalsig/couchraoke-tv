package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

@Serializable
data class AssignSingerMessage(
    override val sessionId: String,
    val songInstanceSeq: Long,
    val playerId: PlayerId,
    val difficulty: Difficulty,
    val startMode: StartMode,
    val countdownMs: Int? = null,
    val stopAtLyricsTimeMs: Long,
    val udpPort: Int,
    val songTitle: String,
    val songArtist: String,
) : NetworkMessage {
    init {
        require(songInstanceSeq in UInt32Range)
    }

    override val type: String = "assignSinger"
    override val protocolVersion: Int = 1
}

@Serializable
data class PlaybackStateMessage(
    override val sessionId: String,
    val songInstanceSeq: Long,
    val revision: Long,
    val state: PlaybackNetworkState,
    val lyricsTimeMs: Long,
    val stopAtLyricsTimeMs: Long,
    val countdownRemainingMs: Int? = null,
    val reason: PlaybackStateReason,
    val tsTvMs: Long? = null,
) : NetworkMessage {
    init {
        require(songInstanceSeq in UInt32Range)
    }

    override val type: String = "playbackState"
    override val protocolVersion: Int = 1
}

@Serializable
data class ClockAckMessage(
    val pingId: String,
    val tTvRecvMs: Long,
    val tsTvMs: Long? = null,
) {
    val type: String = "clockAck"
    val protocolVersion: Int = 1
}

data class PongResponse(
    val phoneId: String,
    val pingId: String = "",
    val tTvSendMs: Long = 0L,
    val tPhoneRecvMs: Long = 0L,
    val tPhoneSendMs: Long = 0L,
    val phoneTimeMs: Long = tPhoneSendMs,
    val tvReceiveTimeMs: Long,
    val isValidSample: Boolean,
)

@Serializable
enum class StartMode(val wireValue: String) {
    @SerialName("countdown")
    Countdown("countdown"),

    @SerialName("live")
    Live("live"),
}

@Serializable
enum class PlaybackNetworkState(val wireValue: String) {
    @SerialName("countdown")
    Countdown("countdown"),

    @SerialName("playing")
    Playing("playing"),

    @SerialName("paused")
    Paused("paused"),

    @SerialName("stopped")
    Stopped("stopped"),
}

@Serializable
enum class PlaybackStateReason(val wireValue: String) {
    @SerialName("")
    Unspecified(""),

    @SerialName("user_pause")
    UserPause("user_pause"),

    @SerialName("singer_disconnected")
    SingerDisconnected("singer_disconnected"),

    @SerialName("song_end")
    SongEnd("song_end"),

    @SerialName("user_quit")
    UserQuit("user_quit"),

    @SerialName("restart")
    Restart("restart"),

    @SerialName("segment_transition")
    SegmentTransition("segment_transition"),

    @SerialName("medley_source")
    MedleySource("medley_source"),

    @SerialName("medley_end")
    MedleyEnd("medley_end"),
}

private val UInt32Range = 0L..UInt.MAX_VALUE.toLong()

fun SongEntry.toIndexedSong(phoneClientId: String): IndexedSong = IndexedSong(
    songId = "$phoneClientId::$relativeTxtPath",
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
    hasInstrumental = hasInstrumental,
    canMedley = canMedley,
    medleySource = medleySource,
    medleyStartBeat = medleyStartBeat,
    medleyEndBeat = medleyEndBeat,
    startSec = startSec,
    previewStartSec = previewStartSec,
)
