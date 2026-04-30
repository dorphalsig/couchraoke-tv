# Contract: Playback UI and LibVLC Seam

## PlaybackIntent

Planned FQCN: `com.couchraoke.tv.presentation.playback.PlaybackIntent`

```kotlin
sealed class PlaybackIntent {
    data class Prepare(
        val audioUrl: String,
        val videoUrl: String?,
        val videoGapSec: Float?,
        val seekToSec: Float,
        val chartEndLyricsTimeMs: Long?
    ) : PlaybackIntent()

    data class Play(
        val stopAtLyricsTimeMs: Long
    ) : PlaybackIntent()
    data object Pause : PlaybackIntent()
    data object Stop : PlaybackIntent()
    data class Seek(val positionMs: Long) : PlaybackIntent()

    data class PrebufferNext(
        val audioUrl: String,
        val videoUrl: String? = null,
        val videoGapSec: Float? = null,
        val seekToSec: Float
    ) : PlaybackIntent()
    data class FadeOut(val durationSec: Float) : PlaybackIntent()
    data class Crossfade(val fadeOutSec: Float, val fadeInSec: Float) : PlaybackIntent()
}
```

Producer: PlaybackCoordinator.  
Consumer: Singing playback controller / ViewModel.

## PlaybackEvent

Planned FQCN: `com.couchraoke.tv.presentation.playback.PlaybackEvent`

```kotlin
sealed class PlaybackEvent {
    data class Prepared(val effectivePlaybackDurationMs: Long) : PlaybackEvent()
    data class Ready(val songStartTvMs: Long) : PlaybackEvent()
    data class Error(val cause: Throwable) : PlaybackEvent()
    data object Ended : PlaybackEvent()
}
```

Producer: playback UI/controller.  
Consumer: PlaybackCoordinator.

Rules:
- `Prepared` must be emitted after `Prepare` and before countdown or live playback, and is sourced from the authoritative audio handle duration.
- `Ready` must be emitted from first audio `LibVlcEvent.Playing`, or fallback after 500ms; decorative video never emits `Ready`.
- `Ended` fires when playback reaches the finalized `stopAtLyricsTimeMs` carried by `Play`, and the authoritative audio handle is stopped.
- `Error` from audio playback triggers return to Song List with blocking error modal; decorative video errors only trigger static-background fallback.

## LibVlcPlayerHandle

Planned FQCN: `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle`

```kotlin
interface LibVlcPlayerHandle {
    val timeMs: Long
    val durationMs: Long?

    fun setEventListener(listener: (LibVlcEvent) -> Unit)
    fun prepare(url: String)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()
}

sealed class LibVlcEvent {
    data object Prepared : LibVlcEvent()
    data object Playing : LibVlcEvent()
    data object Paused : LibVlcEvent()
    data object Stopped : LibVlcEvent()
    data object EndReached : LibVlcEvent()
    data class EncounteredError(val lastWarningOrError: String?) : LibVlcEvent()
}
```

Rules:
- UI/playback layer is the only layer that imports LibVLC types.
- One handle wraps one LibVLC MediaPlayer.
- The audio handle is authoritative for `Prepared`, `currentPositionMs`, `songStartTvMs`, `Ready`, and stop-boundary enforcement.
- Optional video handle is decorative only, configured without audio, and must not affect scoring/session state.
- LibVLC adapter owns warning/error log capture and maps the latest warning/error line, truncated to 120 chars, into `LibVlcEvent.EncounteredError(lastWarningOrError)`.

## Song List Preview Playback

Planned FQCN: `com.couchraoke.tv.presentation.playback.SongPreviewController`

```kotlin
interface SongPreviewController {
    fun preparePreview(audioUrl: String, startPositionMs: Long)
    fun play()
    fun stop()
    fun release()
}
```

Rules:
- Producer: Song List screen/ViewModel focus state.
- Consumer: screen-scoped LibVLC preview player.
- Preview starts only after the same song tile remains focused for 500 ms.
- Preview prepares the manifest `audioUrl` and seeks to `previewStartSec` when positive; otherwise it seeks to 0 seconds.
- Preview plays until stopped; there is no fixed 10-second limit.
- Preview stops immediately when focus changes, focus leaves the grid, overlay/modal/settings/singing opens, or Song List exits.
- Preview HTTP/player failures are suppressed silently.
- Preview audibility follows TV/system media volume only in Iteration 1.
- LibVLC is used rather than Media3 so preview shares Singing playback's broad codec behavior for older USDX libraries.

## Prepared Duration

- `Prepare.chartEndLyricsTimeMs` is the parsed `#END` value converted to milliseconds when present and positive; otherwise it is null.
- For audio-only playback, `effectivePlaybackDurationMs` is the prepared audio handle duration.
- For audio + video playback, audio remains master and `effectivePlaybackDurationMs` is still the prepared audio handle duration; video is decorative.
- `#START` changes initial seek position only and does not change lyrics-time origin.
- Coordinator finalizes `stopAtLyricsTimeMs` as `chartEndLyricsTimeMs ?: effectivePlaybackDurationMs` after `Prepared`.
- If `chartEndLyricsTimeMs` is null and the audio handle cannot expose a usable duration, preparation fails through `PlaybackEvent.Error` instead of guessing the stop boundary.

## songStartTvMs Capture

1. Register the audio handle event listener before `play()`.
2. Capture `fallbackStartTvMs = System.nanoTime() / 1_000_000` immediately before `play()`.
3. On first audio `Playing`, compute `songStartTvMs = nowTvMs - audioHandle.timeMs`.
4. If no `Playing` event arrives within 500ms, emit `Ready(fallbackStartTvMs)` and log a warning.
5. Coordinator waits for `Ready` before calling `ScoringEngine.setSongStart`.

## Stop Boundary

- `Play(stopAtLyricsTimeMs)` carries the finalized stop boundary and is the first intent that allows playback to start.
- UI observes audio `currentPositionMs` after `Play`.
- When lyrics time reaches the finalized `stopAtLyricsTimeMs`, UI calls `LibVlcPlayerHandle.stop()` on the audio handle, stops/releases any decorative video handle, and emits `PlaybackEvent.Ended`.
- Iteration 1 coordinator returns to Song List on `Ended`.

## Optional Video Admission, Degradation, and Fallback

- Optional video is admitted only when load-time capability checks allow it.
- Static admission gate: if video is greater than 720p and hardware decoder support cannot be confirmed using Android codec capability inspection (`MediaCodecList` / `MediaCodecInfo`), Singing starts with audio plus static background instead of creating the video handle.
- Runtime gameplay-degradation gate: when gameplay code reports sustained degradation severe enough to protect the singing experience, playback disables decorative video for the current song, falls back to static background, and keeps audio/playback/session state running.
- Video load or playback failure disables decorative video for the current song and falls back to static background.
- Iteration 1 does not test dropped decorative-video frames; gameplay degradation checks apply to gameplay signals such as future pitch-frame/render quality, not decorative video frame drops.
- Video load, playback failure, or gameplay-degradation fallback never emits blocking playback errors and never changes audio/session state.

## Audio Focus

- Request audio focus before `play()`.
- If focus request fails, emit `PlaybackEvent.Error`.
- Transient loss pauses; gain resumes; permanent loss follows playback-error path.
- Abandon audio focus on song end, error exit, quit, or restart.

## Error Modal Source

- Maintain a single-slot last warning/error LibVLC log line, truncated to 120 chars.
- Audio-handle `LibVlcEvent.EncounteredError` emits `PlaybackEvent.Error` with that diagnostic payload.
- Playback error modal body line 1: `This song can't be played.`
- Body line 2: last log line when present.
- Decorative video-handle `LibVlcEvent.EncounteredError` logs and falls back to static background without showing the blocking modal.

## Explicit Non-Scope

- Full Results transition, scoring finalization, live pitch cursor, hit/miss feedback, dual-player lanes, and medley playback.
