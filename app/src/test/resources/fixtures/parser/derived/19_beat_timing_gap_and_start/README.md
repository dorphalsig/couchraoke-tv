# 19 — Beat timing: gap-aware pre-roll and start offset

Purpose: verify that beat cursors correctly handle:
- Negative chart-relative time during the pre-roll gap period (lyricsTimeSec < gapMs/1000)
- Non-zero start offset initialization (startSec > 0)

## Covered scenarios (US1 SC-001)
- Pre-roll: lyricsTimeSec before the gap elapses produces negative highlightTimeSec and a deterministic pre-roll currentBeat
- Start offset: a song with startSec set still correctly aligns gap, beat positions, and mic delay from the offset point
