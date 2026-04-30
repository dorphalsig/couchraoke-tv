# Data Model: Solo Sing Playback

## ConnectedPhone

Represents one phone connected to the TV host session.

| Field | Type | Rules |
|---|---|---|
| `clientId` | `String` | Stable phone client identifier from hello. |
| `connectionId` | `UShort` | Assigned by TV on successful hello; new value on reconnect. |
| `deviceName` | `String` | Friendly display label for TV UI. |
| `httpPort` | `Int` | Phone HTTP server port for manifest and song assets. |
| `ipAddress` | `String` | LAN address used for HTTP/WebSocket peer communication. |

Relationships:
- A connected phone may provide many `SongEntry` records.
- Only one phone can be selected as singer in Iteration 1.

## SessionState

TV-owned host session state.

| Field | Type | Rules |
|---|---|---|
| `sessionId` | `String` | Identifies the current TV session. |
| `sessionToken` | `String` | Same token displayed as join code, encoded in QR URL, and validated on WebSocket connection. |
| `joinCode` | `String` | Human-enterable representation of `sessionToken`. |
| `connectedPhones` | `List<ConnectedPhone>` | Current roster. |
| `locked` | `Boolean` | Open before song start, locked during active song, open again on error/end/quit. |

State transitions:
- Launch starts a new Open session.
- Song start locks session.
- Playback error, quit, countdown disconnect, or normal song end returns session to Open.

## SongEntry

Manifest element from a phone `/manifest.json` response.

| Field | Type | Rules |
|---|---|---|
| `relativeTxtPath` | `String` | Required, non-empty. |
| `modifiedTimeMs` | `Long` | Required. |
| `title` | `String` | Required. |
| `artist` | `String` | Required. |
| `album` | `String?` | Optional. |
| `year` | `Int?` | Optional. |
| `genre` | `String?` | Optional. |
| `isDuet` | `Boolean` | Duet execution disabled in Iteration 1. |
| `hasRap` | `Boolean` | Display metadata only in Iteration 1. |
| `hasVideo` | `Boolean` | Indicates optional `videoUrl`. |
| `hasInstrumental` | `Boolean` | Chip/settings metadata only; TV plays pre-mixed `audioUrl`. |
| `canMedley` | `Boolean` | Medley execution disabled in Iteration 1. |
| `medleySource` | `String?` | Future medley metadata. |
| `medleyStartBeat` | `Int?` | Future medley metadata. |
| `medleyEndBeat` | `Int?` | Future medley metadata. |
| `startSec` | `Float` | Audio seek start. |
| `previewStartSec` | `Float` | Song List preview start. |
| `txtUrl` | `String` | Required LAN URL for USDX chart bytes. |
| `audioUrl` | `String` | Required LAN URL for pre-mixed audio. |
| `videoUrl` | `String?` | Optional LAN URL for decorative video. |
| `coverUrl` | `String?` | Optional cover image URL. |
| `backgroundUrl` | `String?` | Optional background image URL. |

Validation:
- Required fields missing or malformed cause the manifest entry to be rejected.
- Remote URLs are streamed; TV does not persist assets.

## IndexedSong

Existing TV-side catalog record derived from `SongEntry` plus source phone identity.

| Field | Type | Rules |
|---|---|---|
| `songId` | `String` | TV-generated stable identifier for this manifest item. |
| `phoneClientId` | `String` | Source phone identity. |
| `relativeTxtPath` | `String` | From manifest. |
| `modifiedTimeMs` | `Long` | From manifest. |
| Display and asset fields | As in existing `IndexedSong` | Mirrors existing domain contract. |

Relationships:
- Produced by manifest aggregation.
- Consumed by Song List, Select Players, PlaybackCoordinator, and Singing render model builder.

## PreviewPlayback

Song List screen-scoped playback state for focused-song preview.

| Field | Type | Rules |
|---|---|---|
| `songId` | `String` | Focused song that owns the pending or active preview. |
| `audioUrl` | `String` | Manifest audio URL prepared by the LibVLC preview player. |
| `startPositionMs` | `Long` | Positive `previewStartSec` in ms, otherwise 0. |
| `debounceMs` | `Long` | Preview starts after 500 ms of same-tile focus. |

Rules:
- Screen-scoped to Song List and released on Song List exit.
- Stops on focus changes, grid exit, overlay/modal/settings/singing transitions, or Song List exit.
- Player/HTTP failures are silent.
- Uses TV/system media volume only in Iteration 1.

## SongStartSelection

TV-owned handoff from Select Players to PlaybackCoordinator.

| Field | Type | Rules |
|---|---|---|
| `songId` | `String` | Must resolve to an `IndexedSong`. |
| `playerPhoneId` | `String` | Must resolve to connected phone. |
| `playerId` | `PlayerId` | Always `P1` in Iteration 1. |
| `difficulty` | `Difficulty` | Defaults to Medium; used in assignSinger, not scoring in Iteration 1. |
| `countdownEnabled` | `Boolean` | Defaults to Ready countdown setting. |
| `countdownSeconds` | `Int` | 1-10; default 3. |

## GamePhase

Host-owned song lifecycle for Iteration 1.

| State | Meaning |
|---|---|
| `Idle` | Song List/session open. |
| `Loading` | Manifest/chart/playback preparation in progress. |
| `Countdown` | Countdown visible before playback. |
| `Playing` | Audio playing and static lane/lyrics rendering active. |
| `Paused` | User-initiated pause. |
| `DisconnectPaused` | Required singer disconnected after start. |

Iteration 1 terminal behavior:
- Playback end returns to `Idle`/Song List instead of Results.
- Results remains out of scope until Iteration 2.

## PlaybackIntent

Coordinator-to-playback/UI command.

| Variant | Fields | Rules |
|---|---|---|
| `Prepare` | `audioUrl`, `videoUrl?`, `videoGapSec?`, `seekToSec`, `chartEndLyricsTimeMs?` | Prepares streamed media and reports authoritative audio duration. `chartEndLyricsTimeMs` is parsed `#END` converted to milliseconds when present and positive; otherwise null. |
| `Play` | `stopAtLyricsTimeMs` | Requests playback start/resume with the finalized stop boundary. `Play` MUST NOT be emitted before `Prepared`, clock-sync, `assignSinger`, and playback-state emission complete. |
| `Pause` | none | Pauses current playback. |
| `Stop` | none | Stops playback and tears down active handles. |
| `Seek` | `positionMs` | Used for restart/resume where needed. |
| `PrebufferNext` | `audioUrl`, `videoUrl?`, `videoGapSec?`, `seekToSec` | Medley-only command; inert/no-op in Iteration 1 and wired in Iteration 4. |
| `FadeOut` | `durationSec` | Medley-only command; inert/no-op in Iteration 1 and wired in Iteration 4. |
| `Crossfade` | `fadeOutSec`, `fadeInSec` | Medley-only command; inert/no-op in Iteration 1 and wired in Iteration 4. |

## PlaybackEvent

Playback/UI-to-coordinator event.

| Variant | Fields | Rules |
|---|---|---|
| `Prepared` | `effectivePlaybackDurationMs: Long` | Must arrive before countdown/live playback; sourced from the authoritative audio handle duration. |
| `Ready` | `songStartTvMs: Long` | Captured from first audio Playing event or fallback; never from decorative video. |
| `Error` | `cause: Throwable` | Triggers playback-error path to Song List. |
| `Ended` | none | Triggers Iteration 1 return to Song List. |

## AssignSingerMessage

TV-to-phone selected singer assignment.

Required fields:
- `type="assignSinger"`
- `protocolVersion=1`
- `sessionId`
- `songInstanceSeq`
- `playerId="P1"`
- `difficulty`
- `startMode`: `countdown` or `live`
- `countdownMs` when countdown mode
- `stopAtLyricsTimeMs`
- `udpPort`
- `songTitle`
- `songArtist`

Rules:
- Sent only to selected singer phone.
- Requires a valid clock-sync sample before send/start.
- Uses the finalized `stopAtLyricsTimeMs` computed after `Prepared`: parsed `#END` when present and positive, otherwise the prepared audio duration.
- Serialized JSON MUST include common envelope fields `type` and `protocolVersion`; implementations may model them as constants rather than caller-supplied constructor fields.
- UDP frame behavior remains future scope, but `udpPort` remains part of the contract.

## StartMode

Wire enum used by `AssignSingerMessage`.

| Value | Wire value | Rules |
|---|---|---|
| `Countdown` | `countdown` | Phone waits for `countdownMs` before sending future pitch frames. |
| `Live` | `live` | Phone may begin future pitch-frame behavior immediately. |

Pitch-frame transmission remains out of scope for Iteration 1.

## PlaybackStateMessage

TV-to-phone playback substate.

Required fields:
- `type="playbackState"`
- `protocolVersion=1`
- `sessionId`
- `songInstanceSeq`
- `revision`
- `state`: `countdown`, `playing`, `paused`, or `stopped`
- `lyricsTimeMs`
- `stopAtLyricsTimeMs`
- `countdownRemainingMs` when state is `countdown`; otherwise null or omitted by the wire serializer
- `reason`
- `tsTvMs` optional TV timestamp

Rules:
- Constructed by PlaybackCoordinator only.
- Emitted on playback-bearing game phase transitions.
- Uses the finalized `stopAtLyricsTimeMs` computed after `Prepared`: parsed `#END` when present and positive, otherwise the prepared audio duration.
- Not emitted for `Idle`, `Loading`, or future `Results`.

## SingingRenderModel

Immutable model for the Singing screen.

| Field | Type | Rules |
|---|---|---|
| `songId` | `String` | Matches selected song. |
| `title` | `String` | Top metadata. |
| `artist` | `String` | Top metadata. |
| `lyricsPages` | `List<LyricsPage>` | Current/next sentence paging. |
| `laneNotes` | `List<StaticNoteTarget>` | Static note bars from parsed song file for P1. |
| `startSec` | `Float` | Playback seek start. |
| `stopAtLyricsTimeMs` | `Long` | Authoritative stop boundary. |
| `audioUrl` | `String` | Streamed audio URL. |
| `videoUrl` | `String?` | Optional decorative video URL. |
| `videoGapSec` | `Float?` | Optional video timing offset for decorative video only. |

Rules:
- Built after `txtUrl` fetch and parse, before countdown/live playback.
- Contains no live pitch-frame data.
- Video fields describe optional decorative media; playback timing and stop boundaries remain audio-owned.

## SingingBackground

Presentation state for optional decorative video/static background during Singing.

| Variant | Fields | Rules |
|---|---|---|
| `Static` | `imageUrl?`, `defaultImageKey` | Uses the parsed song `#BACKGROUND` / manifest `backgroundUrl` when present, otherwise the app-shipped default static singing background. |
| `Video` | `videoUrl`, `fallbackImageUrl?`, `defaultImageKey`, `disabledReason?` | Decorative only; falls back to `fallbackImageUrl` when present, otherwise the app-shipped default static singing background. `disabledReason` is set when static admission, video failure, or gameplay-degradation fallback disables video. |

Rules:
- Does not affect audio playback, `Ready`, `Prepared`, scoring, or session state.
- Static admission disables video before creating a decorative handle when video is greater than 720p and hardware decoder support cannot be confirmed.
- Video load or playback errors update only the decorative background path.
- Runtime gameplay-degradation reports may disable decorative video and fall back to static background without affecting audio/playback/session state.
- Iteration 1 does not test dropped decorative-video frames; gameplay degradation checks apply to gameplay signals such as future pitch-frame/render quality, not decorative video frame drops.

## LaneRenderState

Frame state consumed by the static note lane renderer.

| Field | Type | Rules |
|---|---|---|
| `notes` | `List<StaticNoteTarget>` | Immutable note targets. |
| `lyricsTimeMs` | `Long` | Current audio/lyrics time. |
| `visibleWindowMs` | `LongRange` | Current horizontal viewport. |
| `playerId` | `PlayerId` | Always P1 in Iteration 1. |
| `showNoteLines` | `Boolean` | Visual setting; defaults ON. |

Rules:
- No live pitch, no hit/miss state, no scoring state.
- Derived from `SingingRenderModel` plus current playback position.

## UiModalState

Presentation state for modal/overlay surfaces.

Variants:
- `JoinOverlay`
- `SelectPlayers(songId)`
- `NoPhonesConnected`
- `CountdownDisconnect`
- `Pause`
- `RestartConfirm`
- `QuitConfirm`
- `PlaybackError(title, bodyLine1, bodyLine2?)`

Rules:
- Back closes overlays/modals except Singing Back, which opens Pause.
- Destructive confirmations default focus to Cancel.
