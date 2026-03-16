package com.couchraoke.tv.domain.session

sealed class SessionEvent {
    data class RequiredSingerDisconnected(val clientId: String, val slot: String) : SessionEvent()
    data class SpectatorDisconnected(val clientId: String) : SessionEvent()
    data class PlaybackSourceLost(val clientId: String) : SessionEvent()
    data class PhoneConnected(val clientId: String, val deviceName: String) : SessionEvent()
    data class PhoneReconnected(val clientId: String, val wasSinger: Boolean) : SessionEvent()
    data class PhoneDisconnected(val clientId: String) : SessionEvent()
    data class RosterChanged(val clientId: String) : SessionEvent()
}
