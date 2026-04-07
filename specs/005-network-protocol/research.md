# Research: Network Protocol (005)

## Phase 0 Resolution Summary

All previously open ambiguities for the updated TV↔phone playback contract are now resolved in the feature spec. This document records the decisions that drive Phase 1 design.

## Product / Protocol Decisions

### Authoritative playback contract
**Decision**: Add a dedicated `playbackState` TV→phone message as the single authoritative source for countdown, playing, paused, stopped, paused-aware playback clock, reconnect sync, and early-stop behavior.
**Rationale**: This removes live progression semantics from `assignSinger` and gives the phone one contract for pause/resume/reconnect/early stop.
**Alternatives considered**: Keep live playback progression inside `assignSinger`; split countdown and playback into separate messages. Both create duplicate sources of truth.

### `assignSinger` scope
**Decision**: Keep `assignSinger` as a static assignment/configuration message only.
**Rationale**: Assignment fields change rarely; playback state changes frequently. Separating them reduces message ambiguity and reconnect complexity.
**Alternatives considered**: Keep countdown/pre-roll hints in `assignSinger`. Rejected because countdown ownership is now explicitly centralized in `playbackState`.

### Reconnect and playback revision
**Decision**: Reconnect re-sends the latest `playbackState` for the current `songInstanceSeq` without incrementing `revision`.
**Rationale**: `revision` tracks authoritative playback contract changes, not transport recovery. Reconnect alone should not fabricate a new playback transition.
**Alternatives considered**: Increment revision on every reconnect; increment conditionally on reconnect. Rejected because both blur the distinction between playback changes and delivery retries.

### `playbackState.reason` semantics
**Decision**: `reason` remains an optional open-ended hint rather than a closed enum.
**Rationale**: The phone can use known values when useful while remaining forward-compatible with new reasons.
**Alternatives considered**: Closed enum; fixed core plus extensible values. Rejected because the current feature only needs informational semantics, not exhaustive branching.

### Assignment stop point
**Decision**: Replace phone-facing `endTimeTvMs` semantics with `stopAtLyricsTimeMs` in song time.
**Rationale**: The phone now reasons about assignment completion in paused-aware lyrics time rather than absolute TV-wall-clock progression.
**Alternatives considered**: Keep `endTimeTvMs` for reconnect math. Rejected because it conflicts with the new paused-aware playback contract.

### Microphone sensitivity ownership
**Decision**: Remove `thresholdIndex` from `assignSinger`; microphone sensitivity is phone-managed.
**Rationale**: Sensitivity tuning now lives on the phone and is no longer part of the TV-assignment contract.
**Alternatives considered**: Keep `thresholdIndex` for backwards compatibility. Rejected because the spec now explicitly moves ownership to the phone.

## Technology Decisions

### WebSocket control channel
**Decision**: Continue using Ktor CIO with `ktor-server-websockets`.
**Rationale**: Already implemented, constitution-approved, and sufficient for small LAN JSON control messages.
**Alternatives considered**: Netty, raw sockets. Rejected by stack and complexity constraints.

### JSON serialization
**Decision**: Continue using `kotlinx-serialization-json` with explicit message data classes.
**Rationale**: Already implemented, constitution-approved, and compatible with forward-tolerant decoding for unknown message types.
**Alternatives considered**: Gson, Moshi. Rejected by constitution.

### UDP pitch transport
**Decision**: Keep fixed 16-byte UDP pitch frames over `DatagramSocket`.
**Rationale**: Required by the original protocol and constitution; already implemented and tested.
**Alternatives considered**: Alternate transport or variable-size payloads. Rejected by protocol contract.

### Session and reconnect authority
**Decision**: Keep sender-ID reassignment on reconnect, while replaying the latest `assignSinger` + `playbackState` snapshot.
**Rationale**: This preserves current connection identity guarantees and cleanly rehydrates the phone after reconnect.
**Alternatives considered**: Reuse sender IDs across reconnects. Rejected because current connection identity is per-connection, not per-device-session.
