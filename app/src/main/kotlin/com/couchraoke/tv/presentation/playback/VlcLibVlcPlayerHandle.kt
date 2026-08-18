package com.couchraoke.tv.presentation.playback

import android.content.Context
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia

class VlcLibVlcPlayerHandle private constructor(
    private val owner: VlcHandleOwner,
    private val disableAudio: Boolean,
) : LibVlcPlayerHandle, VideoSurfaceBinder, PreparedDurationProvider {
    private val mutableEvents = MutableSharedFlow<LibVlcEvent>(extraBufferCapacity = EventBufferCapacity)
    private var currentMedia: VlcMedia? = null
    private val player: VlcPlayer get() = owner.player
    private val mediaFactory: VlcMediaFactory get() = owner.mediaFactory

    constructor(
        context: Context,
        disableAudio: Boolean = false,
    ) : this(AndroidVlcOwner(context), disableAudio)

    internal constructor(
        player: VlcPlayer,
        mediaFactory: VlcMediaFactory,
        disableAudio: Boolean = false,
        releaseOwner: () -> Unit = {},
    ) : this(TestVlcOwner(player, mediaFactory, releaseOwner), disableAudio)

    override val events: SharedFlow<LibVlcEvent> = mutableEvents
    override val timeMs: Long get() = player.timeMs
    override val durationMs: Long? get() = player.durationMs?.takeIf { it > 0L }

    init {
        player.listener = { callback -> mutableEvents.tryEmit(callback.toLibVlcEvent()) }
    }

    override fun prepare(mediaUrl: String, seekToSec: Float) {
        require(mediaUrl.isNotBlank())
        currentMedia?.release()
        val media = mediaFactory.create(mediaUrl)
        if (disableAudio) {
            media.addOption(DisableAudioOption)
        }
        currentMedia = media
        player.setMedia(media)
        media.parse(IMedia.Parse.ParseNetwork, ParseTimeoutMs)
        player.seekTo((seekToSec * 1_000).toLong().coerceAtLeast(0L))
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stop() {
        player.stop()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun setVolume(percent: Int) {
        player.setVolume(percent.coerceIn(0, 100))
    }

    override fun setVideoSurface(holder: SurfaceHolder?) {
        player.setSurface(holder?.surface, holder)
    }

    override fun release() {
        player.listener = null
        currentMedia?.release()
        currentMedia = null
        player.release()
        owner.release()
    }

    private fun VlcPlayerCallback.toLibVlcEvent(): LibVlcEvent = when (this) {
        VlcPlayerCallback.Playing -> LibVlcEvent.Playing
        VlcPlayerCallback.Paused -> LibVlcEvent.Paused
        VlcPlayerCallback.EndReached -> LibVlcEvent.EndReached
        is VlcPlayerCallback.TimeChanged -> LibVlcEvent.TimeChanged(timeMs)
        is VlcPlayerCallback.EncounteredError -> LibVlcEvent.EncounteredError(lastWarningOrError?.take(120))
    }

    private companion object {
        const val DisableAudioOption = ":no-audio"
        const val EventBufferCapacity = 64
        const val ParseTimeoutMs = 5_000L
    }
}

private interface VlcHandleOwner {
    val player: VlcPlayer
    val mediaFactory: VlcMediaFactory
    fun release()
}

private class TestVlcOwner(
    override val player: VlcPlayer,
    override val mediaFactory: VlcMediaFactory,
    private val onRelease: () -> Unit,
) : VlcHandleOwner {
    override fun release() {
        onRelease()
    }
}

internal interface VlcMediaFactory {
    fun create(url: String): VlcMedia
}

internal interface VlcMedia {
    val url: String
    fun addOption(option: String)
    fun parse(flags: Int, timeoutMs: Long): Boolean
    fun release()
}

internal interface VlcPlayer {
    var listener: ((VlcPlayerCallback) -> Unit)?
    val timeMs: Long
    val durationMs: Long?
    fun setMedia(media: VlcMedia)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(percent: Int)
    fun setSurface(surface: Surface?, holder: SurfaceHolder?)
    fun release()
}

internal sealed interface VlcPlayerCallback {
    data object Playing : VlcPlayerCallback
    data object Paused : VlcPlayerCallback
    data object EndReached : VlcPlayerCallback
    data class TimeChanged(val timeMs: Long) : VlcPlayerCallback
    data class EncounteredError(val lastWarningOrError: String?) : VlcPlayerCallback
}

private class AndroidVlcOwner(context: Context) : VlcHandleOwner {
    private val libVlc = LibVLC(context, arrayListOf("--no-drop-late-frames"))
    override val player = AndroidVlcPlayer(MediaPlayer(libVlc))
    override val mediaFactory = AndroidVlcMediaFactory(libVlc)

    override fun release() {
        libVlc.release()
    }
}

private class AndroidVlcMediaFactory(
    private val libVlc: LibVLC,
) : VlcMediaFactory {
    override fun create(url: String): VlcMedia = AndroidVlcMedia(Media(libVlc, Uri.parse(url)), url)
}

private class AndroidVlcMedia(
    val media: Media,
    override val url: String,
) : VlcMedia {
    override fun addOption(option: String) {
        media.addOption(option)
    }

    override fun parse(flags: Int, timeoutMs: Long): Boolean = media.parse(flags)

    override fun release() {
        media.release()
    }
}

private class AndroidVlcPlayer(
    private val mediaPlayer: MediaPlayer,
) : VlcPlayer {
    override var listener: ((VlcPlayerCallback) -> Unit)? = null
        set(value) {
            field = value
            val eventListener = if (value == null) {
                null
            } else {
                MediaPlayer.EventListener { event -> event.toCallback()?.let(value) }
            }
            mediaPlayer.setEventListener(eventListener)
        }

    override val timeMs: Long get() = mediaPlayer.time
    override val durationMs: Long? get() = mediaPlayer.length.takeIf { it > 0L }

    override fun setMedia(media: VlcMedia) {
        require(media is AndroidVlcMedia)
        mediaPlayer.media = media.media
    }

    override fun play() {
        mediaPlayer.play()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun stop() {
        mediaPlayer.stop()
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer.time = positionMs
    }

    override fun setVolume(percent: Int) {
        mediaPlayer.volume = percent
    }

    override fun setSurface(surface: Surface?, holder: SurfaceHolder?) {
        val vout = mediaPlayer.vlcVout
        vout.setVideoSurface(surface, holder)
        if (surface == null) {
            vout.detachViews()
        } else {
            vout.attachViews()
        }
    }

    override fun release() {
        mediaPlayer.setEventListener(null)
        mediaPlayer.release()
    }

    private fun MediaPlayer.Event.toCallback(): VlcPlayerCallback? = when (type) {
        MediaPlayer.Event.Playing -> VlcPlayerCallback.Playing
        MediaPlayer.Event.Paused -> VlcPlayerCallback.Paused
        MediaPlayer.Event.EndReached -> VlcPlayerCallback.EndReached
        MediaPlayer.Event.TimeChanged -> VlcPlayerCallback.TimeChanged(mediaPlayer.time)
        MediaPlayer.Event.EncounteredError -> VlcPlayerCallback.EncounteredError(toString().takeIf { it.isNotBlank() })
        else -> null
    }
}
