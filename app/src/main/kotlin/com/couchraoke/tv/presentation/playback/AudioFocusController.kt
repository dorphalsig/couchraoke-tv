package com.couchraoke.tv.presentation.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.couchraoke.quality.NoCoverageGenerated

interface AudioFocusController {
    fun requestAudioFocus(): Boolean

    fun abandonAudioFocus()
}

enum class AudioFocusChange {
    Gain,
    TransientLoss,
    PermanentLoss,
}

@NoCoverageGenerated
class AndroidAudioFocusController(
    context: Context,
    private val onFocusChange: (AudioFocusChange) -> Unit,
) : AudioFocusController {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setOnAudioFocusChangeListener(::handleFocusChange)
        .build()

    override fun requestAudioFocus(): Boolean =
        audioManager?.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    override fun abandonAudioFocus() {
        audioManager?.abandonAudioFocusRequest(focusRequest)
    }

    private fun handleFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> onFocusChange(AudioFocusChange.Gain)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> onFocusChange(AudioFocusChange.TransientLoss)
            AudioManager.AUDIOFOCUS_LOSS -> onFocusChange(AudioFocusChange.PermanentLoss)
        }
    }
}
