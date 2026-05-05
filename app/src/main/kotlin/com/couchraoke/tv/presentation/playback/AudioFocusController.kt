package com.couchraoke.tv.presentation.playback

interface AudioFocusController {
    fun requestAudioFocus(): Boolean

    fun abandonAudioFocus()
}

enum class AudioFocusChange {
    Gain,
    TransientLoss,
    PermanentLoss,
}

object AlwaysGrantedAudioFocusController : AudioFocusController {
    override fun requestAudioFocus(): Boolean = true

    override fun abandonAudioFocus() = Unit
}
