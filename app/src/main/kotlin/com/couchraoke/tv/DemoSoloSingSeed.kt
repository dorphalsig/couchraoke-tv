@file:NoCoverageGenerated

package com.couchraoke.tv

import com.couchraoke.quality.NoCoverageGenerated

@NoCoverageGenerated
object DemoSoloSingSeed {
    const val SessionId = "tv-session-001"
    const val SessionToken = "ABCDEFGH"
    const val JoinCode = SessionToken
    const val TvIpAddress = "192.168.1.10"
    const val WebSocketPort = 8080
    const val UdpPort = 29170
    const val PhoneClientId = "phone-client-001"
    const val PhoneConnectionId: UShort = 7u
    const val PhoneDeviceName = "Living Room Phone"
    const val PhoneIpAddress = "192.168.1.23"
    const val PhoneHttpPort = 43210
    const val RelativeTxtPath = "solo/demo-song.txt"
    const val ModifiedTimeMs = 1700000000000
    const val SongTitle = "Demo Song"
    const val SongArtist = "Demo Artist"
    const val SongAlbum = "Demo Album"
    const val SongYear = 2026
    const val SongGenre = "Pop"
    const val StartSec = 1.5f
    const val PreviewStartSec = 12.0f
    const val TxtUrl = "http://$PhoneIpAddress:$PhoneHttpPort/songs/$RelativeTxtPath"
    const val AudioUrl = "http://$PhoneIpAddress:$PhoneHttpPort/songs/solo/demo-song.mp3"
    const val VideoUrl = "http://$PhoneIpAddress:$PhoneHttpPort/songs/solo/demo-song.mp4"
    const val CoverUrl = "http://$PhoneIpAddress:$PhoneHttpPort/covers/demo-song.png"
    const val BackgroundUrl = "http://$PhoneIpAddress:$PhoneHttpPort/backgrounds/demo-song.jpg"
    val StaticSoloChart = """
        #TITLE:$SongTitle
        #ARTIST:$SongArtist
        #BPM:120
        #GAP:0
        #MP3:demo-song.mp3
        #START:$StartSec
        #PREVIEWSTART:$PreviewStartSec
        : 0 4 0 Hel
        : 4 4 2 lo
        - 8
        : 12 4 4 couch
        : 16 4 5 raoke
        E
    """.trimIndent()

    fun manifestJson(): String = """
        {"songs":[{"relativeTxtPath":"$RelativeTxtPath","modifiedTimeMs":$ModifiedTimeMs,"title":"$SongTitle","artist":"$SongArtist","album":"$SongAlbum","year":$SongYear,"genre":"$SongGenre","isDuet":false,"hasRap":false,"hasVideo":true,"hasInstrumental":false,"canMedley":false,"startSec":$StartSec,"previewStartSec":$PreviewStartSec,"txtUrl":"$TxtUrl","audioUrl":"$AudioUrl","videoUrl":"$VideoUrl","coverUrl":"$CoverUrl","backgroundUrl":"$BackgroundUrl"}]}
    """.trimIndent()
}
