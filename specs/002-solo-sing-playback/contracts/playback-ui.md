# Contract: Playback UI and LibVLC Seam

## PlaybackIntent

Planned FQCN: `com.couchraoke.tv.presentation.playback.PlaybackIntent`

```kotlin
sealed class PlaybackIntent {
    data class Prepare(
        val audioUrl: String,
        val videoUrl: String?,
        val videoGapSec: Float?,
        val seekToSec: Float
    ) : PlaybackIntent()

    data object Play : PlaybackIntent()
    data object Pause : PlaybackIntent()
    data object Stop : PlaybackIntent()
    data class Seek(val positionMs: Long) : PlaybackIntent()
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
- `Prepared` must be emitted before countdown or live playback.
- `Ready` must be emitted from first audio `LibVlcEvent.Playing`, or fallback after 500ms.
- `Ended` fires when playback reaches `stopAtLyricsTimeMs` and the handle is stopped.
- `Error` triggers return to Song List with blocking error modal.

## LibVlcPlayerHandle

Planned FQCN: `com.couchraoke.tv.presentation.playback.LibVlcPlayerHandle`

```kotlin
interface LibVlcPlayerHandle {
    val timeMs: Long
    val durationMs: Long

    fun prepare(url: String)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()
}
```

Rules:
- UI/playback layer is the only layer that imports LibVLC types.
- Audio handle is authoritative for `currentPositionMs`, `songStartTvMs`, and stop-boundary enforcement.
- Optional video handle is decorative only and must not affect scoring/session state.

## Prepared Duration

- For audio-only playback, `effectivePlaybackDurationMs` is the prepared audio duration.
- For audio + video playback, audio remains master; video is decorative.
- `#START` changes initial seek position only and does not change lyrics-time origin.

## songStartTvMs Capture

1. Register the audio handle event listener before `play()`.
2. Capture `fallbackStartTvMs = System.nanoTime() / 1_000_000` immediately before `play()`.
3. On first audio `Playing`, compute `songStartTvMs = nowTvMs - audioHandle.timeMs`.
4. If no `Playing` event arrives within 500ms, emit `Ready(fallbackStartTvMs)` and log a warning.
5. Coordinator waits for `Ready` before calling `ScoringEngine.setSongStart`.

## Stop Boundary

- UI observes audio `currentPositionMs`.
- When lyrics time reaches `stopAtLyricsTimeMs`, UI calls `LibVlcPlayerHandle.stop()` and emits `PlaybackEvent.Ended`.
- Iteration 1 coordinator returns to Song List on `Ended`.

## Audio Focus

- Request audio focus before `play()`.
- If focus request fails, emit `PlaybackEvent.Error`.
- Transient loss pauses; gain resumes; permanent loss follows playback-error path.
- Abandon audio focus on song end, error exit, quit, or restart.

## Error Modal Source

- Maintain a single-slot last warning/error LibVLC log line, truncated to 120 chars.
- Playback error modal body line 1: `This song can't be played.`
- Body line 2: last log line when present.

## Explicit Non-Scope

- Full Results transition, scoring finalization, live pitch cursor, hit/miss feedback, dual-player lanes, and medley playback.
