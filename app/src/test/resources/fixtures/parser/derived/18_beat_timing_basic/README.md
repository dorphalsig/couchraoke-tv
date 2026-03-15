# 18 — Beat/time conversion: static BPM

Purpose: verify static-BPM formulas for `CurrentBeat` (highlight) and `CurrentBeatD` (scoring).

Harness assumptions:
- `lyricsTimeSec` samples are the song timeline in seconds where `lyricsTimeSec=0` is audio start.
- No clock-sync offset is involved (pure timing math).

## Files
- `song.txt`: minimal chart providing BPM/GAP.
- `expected.beat_cursors.json`: deterministic expected cursor values at selected `lyricsTimeSec` samples.
- `audio.ogg`: silent audio stub; not used by timing tests directly.

## Covered scenarios
- Baseline beat-cursor conversion at a known playback position (US1 SC-001)
- Input: BPM=120, GAP=2000ms, micDelayMs=100, lyricsTimeSec=5.0
- Expected: currentBeat=24, currentBeatD=22
