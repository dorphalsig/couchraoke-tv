# 20 — Beat timing boundary cases: late-frame rejection and note membership

Purpose: verify receiver-side pitch-frame selection and late-arrival rejection:
- Select the most recent frame where `tvTimeMs <= detectionTimeTvMs`
- If the newest qualifying frame is stale by >120ms, treat pitch as `toneValid=false`
- Explicitly drop frames where `latenessMs > 450`

Platform scope: TV-side (Android only).

Assumes `clockOffsetMs=0` so `tvTimeMs` values map directly to TV time.

## Files
- `pitchFrames.jsonl`: 5 frames used as the jitter buffer seed
- `expected.selection.json`: expected selection result per `tvNowMs` sample

## Covered scenarios (US2 SC-002, SC-004)
- 6.1 Most recent frame at or before `tvNowMs` is selected
- 6.2 Newer frame within staleness window is selected
- 6.3 All eligible frames stale (>120ms) → `toneValid=false`
- 6.4 Frame arrives late (`latenessMs > 450`) → explicitly dropped
- 6.5 Out-of-order seq (decreasing) dropped
- 6.6 `tvTimeMs` regression > 200ms → dropped
- 6.7 `tvTimeMs` regression ≤ 200ms → accepted
