# Contract: Singing Rendering

## SingingRenderModelBuilder

Planned FQCN: `com.couchraoke.tv.presentation.singing.SingingRenderModelBuilder`

```kotlin
interface SingingRenderModelBuilder {
    fun build(
        song: IndexedSong,
        parsedSong: ParsedSong,
        playerId: PlayerId
    ): SingingRenderModel
}
```

Producer: presentation/domain adapter.  
Consumer: SingingViewModel and SingingScreen.

## SingingRenderModel

```kotlin
data class SingingRenderModel(
    val songId: String,
    val title: String,
    val artist: String,
    val lyricsPages: List<LyricsPage>,
    val laneNotes: List<StaticNoteTarget>,
    val startSec: Float,
    val stopAtLyricsTimeMs: Long,
    val audioUrl: String,
    val videoUrl: String?,
    val videoGapSec: Float?
)
```

Rules:
- Built after `txtUrl` fetch and parse.
- Uses P1 track only in Iteration 1.
- Contains static note targets from the song file.
- Contains no live pitch-frame data and no scoring state.

## LyricsPage

```kotlin
data class LyricsPage(
    val currentLine: String,
    val nextLine: String,
    val startBeat: Int,
    val nextStartBeat: Int?,
    val syllables: List<LyricSyllable>
)
```

Rules:
- Singing screen shows exactly current and next line.
- Page advances when lyrics beat position reaches first note of the next sentence.
- During instrumental gaps, completed current sentence remains highlighted; no blank pre-page.

## StaticNoteTarget

```kotlin
data class StaticNoteTarget(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val pitch: Int,
    val text: String?,
    val noteType: NoteType
)
```

Rules:
- Derived from parsed chart timing and P1 note events.
- Freestyle/rap/golden note visuals may use existing note metadata, but scoring remains out of scope.
- Values are immutable for the song.

## LaneRenderState

```kotlin
data class LaneRenderState(
    val notes: List<StaticNoteTarget>,
    val lyricsTimeMs: Long,
    val visibleWindowMs: LongRange,
    val playerId: PlayerId,
    val showNoteLines: Boolean
)
```

Rules:
- Derived from `SingingRenderModel` and audio position.
- No live pitch, no hit/miss state, no jitter buffer state, no score state.

## PitchLaneRenderer

Planned FQCN: `com.couchraoke.tv.presentation.singing.PitchLaneRenderer`

```kotlin
interface PitchLaneRenderer {
    fun drawPitchLane(canvas: Canvas, viewport: Rect, state: LaneRenderState)
}
```

Rules:
- Draws a single centered P1 lane for Iteration 1.
- Draws static note targets and current timing window only.
- Must avoid per-frame allocations in hot paths where practical.
- Must not use live glow/shadow/blur.

## Singing Screen UI Requirements

- One centered lane band at `SingingSingleLaneHeight`.
- Score component renders constant `00000`.
- Current and next lyrics lines only.
- Elapsed time uses `MM:SS`.
- Back opens Pause overlay.
- Ended playback returns to Song List.

## Explicit Non-Scope

- Live pitch from pitch frames.
- Pitch cursor driven by phone input.
- Hit/miss feedback.
- Score calculation or score updates.
- Results screen.
- Two-player lane layout and medley header behavior.
