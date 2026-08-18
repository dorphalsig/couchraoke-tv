package com.couchraoke.tv.fixtures

import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty

object SoloSingFixtures {
    const val SessionId: String = "tv-session-001"
    const val SessionToken: String = "ABCDEFGH"
    const val JoinCode: String = SessionToken
    const val TvIpAddress: String = "192.168.1.10"
    const val WebSocketPort: Int = 8080
    const val UdpPort: Int = 29170
    const val PhoneClientId: String = "phone-client-001"
    const val PhoneConnectionId: UShort = 7u
    const val PhoneDeviceName: String = "Living Room Phone"
    const val PhoneIpAddress: String = "192.168.1.23"
    const val PhoneHttpPort: Int = 43210
    const val LoopbackHost: String = "127.0.0.1"
    const val ReconnectedPhoneConnectionId: UShort = 9u
    const val ReconnectedPhoneDeviceName: String = "Reconnected Phone"
    const val ReconnectedPhoneHttpPort: Int = PhoneHttpPort + 1
    const val SongId: String = "phone-client-001::solo/demo-song.txt"
    const val RelativeTxtPath: String = "solo/demo-song.txt"
    const val ModifiedTimeMs: Long = 1_700_000_000_000L
    const val SongTitle: String = "Demo Song"
    const val SongArtist: String = "Demo Artist"
    const val SongAlbum: String = "Demo Album"
    const val SongYear: Int = 2026
    const val SongGenre: String = "Pop"
    const val StartSec: Float = 1.5f
    const val PreviewStartSec: Float = 12.0f
    const val StopAtLyricsTimeMs: Long = 18_000L
    const val ShortPreparedDurationMs: Long = 12_000L
    const val PreparedDurationMs: Long = 120_000L
    const val PreparedStateDurationMs: Long = 10_000L
    const val ReportedPlaybackDurationMs: Long = 123_000L
    const val FallbackPlaybackDurationMs: Long = 180_000L
    const val ReadySongStartTvMs: Long = 123_456L
    const val PlaybackClockMs: Long = 1_000L
    const val PlaybackReadyClockMs: Long = 10_000L
    const val PlaybackFallbackStartMs: Long = 5_000L
    const val PlaybackFallbackReadyMs: Long = 5_500L
    const val SeekPositionMs: Long = 42_000L
    const val RestartSeekPositionMs: Long = 1_500L
    const val AuthoritativeAudioTimeMs: Long = 1_250L
    const val InitialVideoTimeMs: Long = 100L
    const val SyncedVideoTimeMs: Long = 5_350L
    const val ClockAckTvTimeMs: Long = 1_000L
    const val ClockAckPhoneTimeMs: Long = 456L
    const val ClockAckRoundTripMs: Long = 20L
    const val PongPhoneTimeMs: Long = 1L
    const val PongTvReceiveTimeMs: Long = 2L
    const val LatestLibVlcWarning: String = "latest libvlc warning"
    const val LastWarningLine: String = "last warning"
    const val VideoFailureMessage: String = "video failed"
    const val SongInstanceSeq: Long = 1L
    val DefaultPlayerId: PlayerId = PlayerId.P1
    val DefaultDifficulty: Difficulty = Difficulty.Medium

    const val PlaybackAudioUrl: String = "http://phone/audio.mp3"
    const val PlaybackVideoUrl: String = "http://phone/video.mp4"
    const val PlaybackAviVideoUrl: String = "http://phone/video.avi"
    const val NextPreviewAudioUrl: String = "next.mp3"

    fun longWarning(length: Int = 140): String = "w".repeat(length)

    fun connectedPhone(
        clientId: String = PhoneClientId,
        connectionId: UShort = PhoneConnectionId,
        deviceName: String = PhoneDeviceName,
        httpPort: Int = PhoneHttpPort,
        ipAddress: String = PhoneIpAddress,
    ): ConnectedPhoneFixture = ConnectedPhoneFixture(
        clientId = clientId,
        connectionId = connectionId,
        deviceName = deviceName,
        httpPort = httpPort,
        ipAddress = ipAddress,
    )

    fun sessionState(
        sessionId: String = SessionId,
        sessionToken: String = SessionToken,
        joinCode: String = JoinCode,
        connectedPhones: List<ConnectedPhoneFixture> = listOf(connectedPhone()),
        locked: Boolean = false,
    ): SessionStateFixture = SessionStateFixture(
        sessionId = sessionId,
        sessionToken = sessionToken,
        joinCode = joinCode,
        connectedPhones = connectedPhones,
        locked = locked,
    )

    fun songEntry(
        relativeTxtPath: String = RelativeTxtPath,
        modifiedTimeMs: Long = ModifiedTimeMs,
        title: String = SongTitle,
        artist: String = SongArtist,
        album: String? = SongAlbum,
        year: Int? = SongYear,
        genre: String? = SongGenre,
        isDuet: Boolean = false,
        hasRap: Boolean = false,
        hasVideo: Boolean = true,
        hasInstrumental: Boolean = false,
        canMedley: Boolean = false,
        medleySource: String? = null,
        medleyStartBeat: Int? = null,
        medleyEndBeat: Int? = null,
        startSec: Float = StartSec,
        previewStartSec: Float = PreviewStartSec,
        txtUrl: String = assetUrl("/songs/$relativeTxtPath"),
        audioUrl: String = assetUrl("/songs/solo/demo-song.mp3"),
        videoUrl: String? = assetUrl("/songs/solo/demo-song.mp4"),
        coverUrl: String? = assetUrl("/covers/demo-song.png"),
        backgroundUrl: String? = assetUrl("/backgrounds/demo-song.jpg"),
    ): SongEntryFixture = SongEntryFixture(
        relativeTxtPath = relativeTxtPath,
        modifiedTimeMs = modifiedTimeMs,
        title = title,
        artist = artist,
        album = album,
        year = year,
        genre = genre,
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
        txtUrl = txtUrl,
        audioUrl = audioUrl,
        videoUrl = videoUrl,
        coverUrl = coverUrl,
        backgroundUrl = backgroundUrl,
    )

    fun indexedSong(
        songId: String = SongId,
        phoneClientId: String = PhoneClientId,
        entry: SongEntryFixture = songEntry(),
    ): IndexedSong = IndexedSong(
        songId = songId,
        phoneClientId = phoneClientId,
        relativeTxtPath = entry.relativeTxtPath,
        modifiedTimeMs = entry.modifiedTimeMs,
        title = entry.title,
        artist = entry.artist,
        album = entry.album,
        year = entry.year,
        genre = entry.genre,
        txtUrl = entry.txtUrl,
        audioUrl = entry.audioUrl,
        videoUrl = entry.videoUrl,
        coverUrl = entry.coverUrl,
        backgroundUrl = entry.backgroundUrl,
        isDuet = entry.isDuet,
        hasRap = entry.hasRap,
        hasVideo = entry.hasVideo,
        hasInstrumental = entry.hasInstrumental,
        canMedley = entry.canMedley,
        medleySource = entry.medleySource,
        medleyStartBeat = entry.medleyStartBeat,
        medleyEndBeat = entry.medleyEndBeat,
        startSec = entry.startSec,
        previewStartSec = entry.previewStartSec,
    )

    fun songStartSelection(
        songId: String = SongId,
        playerPhoneId: String = PhoneClientId,
        playerId: PlayerId = PlayerId.P1,
        difficulty: Difficulty = Difficulty.Medium,
        countdownEnabled: Boolean = true,
        countdownSeconds: Int = 3,
    ): SongStartSelectionFixture = SongStartSelectionFixture(
        songId = songId,
        playerPhoneId = playerPhoneId,
        playerId = playerId,
        difficulty = difficulty,
        countdownEnabled = countdownEnabled,
        countdownSeconds = countdownSeconds,
    )

    fun playbackPreparation(
        audioUrl: String = assetUrl("/songs/solo/demo-song.mp3"),
        videoUrl: String? = assetUrl("/songs/solo/demo-song.mp4"),
        videoGapSec: Float? = 0.25f,
        seekToSec: Float = StartSec,
    ): PlaybackPreparationFixture = PlaybackPreparationFixture(
        audioUrl = audioUrl,
        videoUrl = videoUrl,
        videoGapSec = videoGapSec,
        seekToSec = seekToSec,
    )

    fun assignSinger(
        sessionId: String = SessionId,
        songInstanceSeq: Long = SongInstanceSeq,
        playerId: PlayerId = PlayerId.P1,
        difficulty: Difficulty = Difficulty.Medium,
        startMode: String = "countdown",
        countdownMs: Int? = 3_000,
        stopAtLyricsTimeMs: Long = StopAtLyricsTimeMs,
        udpPort: Int = UdpPort,
        songTitle: String = SongTitle,
        songArtist: String = SongArtist,
    ): AssignSingerFixture = AssignSingerFixture(
        sessionId = sessionId,
        songInstanceSeq = songInstanceSeq,
        playerId = playerId,
        difficulty = difficulty,
        startMode = startMode,
        countdownMs = countdownMs,
        stopAtLyricsTimeMs = stopAtLyricsTimeMs,
        udpPort = udpPort,
        songTitle = songTitle,
        songArtist = songArtist,
    )

    fun playbackState(
        sessionId: String = SessionId,
        songInstanceSeq: Long = SongInstanceSeq,
        revision: Long = 1L,
        state: String = "countdown",
        lyricsTimeMs: Long = 0L,
        stopAtLyricsTimeMs: Long = StopAtLyricsTimeMs,
        reason: String = "song_start",
    ): PlaybackStateFixture = PlaybackStateFixture(
        sessionId = sessionId,
        songInstanceSeq = songInstanceSeq,
        revision = revision,
        state = state,
        lyricsTimeMs = lyricsTimeMs,
        stopAtLyricsTimeMs = stopAtLyricsTimeMs,
        reason = reason,
    )

    fun joinQrPayload(
        host: String = TvIpAddress,
        port: Int = WebSocketPort,
        token: String = SessionToken,
    ): String = "ws://$host:$port/?token=$token"

    fun manifestJson(entry: SongEntryFixture = songEntry()): String = """
        {"songs":[${manifestSongJson(entry)}]}
    """.trimIndent()

    fun manifestSongJson(entry: SongEntryFixture = songEntry()): String = """
        {"relativeTxtPath":"${entry.relativeTxtPath}","modifiedTimeMs":${entry.modifiedTimeMs},"title":"${entry.title}","artist":"${entry.artist}","album":"${entry.album}","year":${entry.year},"genre":"${entry.genre}","isDuet":${entry.isDuet},"hasRap":${entry.hasRap},"hasVideo":${entry.hasVideo},"hasInstrumental":${entry.hasInstrumental},"canMedley":${entry.canMedley},"startSec":${entry.startSec},"previewStartSec":${entry.previewStartSec},"txtUrl":"${entry.txtUrl}","audioUrl":"${entry.audioUrl}","videoUrl":"${entry.videoUrl}","coverUrl":"${entry.coverUrl}","backgroundUrl":"${entry.backgroundUrl}"}
    """.trimIndent()

    fun assetUrl(path: String, host: String = PhoneIpAddress, port: Int = PhoneHttpPort): String {
        val normalizedPath = if (path.startsWith('/')) path else "/$path"
        return "http://$host:$port$normalizedPath"
    }
}

data class ConnectedPhoneFixture(
    val clientId: String,
    val connectionId: UShort,
    val deviceName: String,
    val httpPort: Int,
    val ipAddress: String,
)

data class SessionStateFixture(
    val sessionId: String,
    val sessionToken: String,
    val joinCode: String,
    val connectedPhones: List<ConnectedPhoneFixture>,
    val locked: Boolean,
)

data class SongEntryFixture(
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

data class SongStartSelectionFixture(
    val songId: String,
    val playerPhoneId: String,
    val playerId: PlayerId,
    val difficulty: Difficulty,
    val countdownEnabled: Boolean,
    val countdownSeconds: Int,
)

data class PlaybackPreparationFixture(
    val audioUrl: String,
    val videoUrl: String?,
    val videoGapSec: Float?,
    val seekToSec: Float,
)

data class AssignSingerFixture(
    val type: String = "assignSinger",
    val protocolVersion: Int = 1,
    val sessionId: String,
    val songInstanceSeq: Long,
    val playerId: PlayerId,
    val difficulty: Difficulty,
    val startMode: String,
    val countdownMs: Int?,
    val stopAtLyricsTimeMs: Long,
    val udpPort: Int,
    val songTitle: String,
    val songArtist: String,
)

data class PlaybackStateFixture(
    val type: String = "playbackState",
    val protocolVersion: Int = 1,
    val sessionId: String,
    val songInstanceSeq: Long,
    val revision: Long,
    val state: String,
    val lyricsTimeMs: Long,
    val stopAtLyricsTimeMs: Long,
    val reason: String,
)
