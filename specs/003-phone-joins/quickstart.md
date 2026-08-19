# Quickstart: Phone Joins

**Feature**: `003-phone-joins` | **Date**: 2026-08-19

How to build, run and prove this slice. Every command is PowerShell — there is no bash in this environment.

---

## Prerequisites

- Android TV device or emulator, API 30+, on the **same LAN** as the machine running the peer. This is not optional: the whole slice is LAN discovery, and an emulator on a NAT'd network will not be found by mDNS.
- The `mockphone` peer checked out with branch `DH1-peer-negative-case-flags` merged, and `uv` on `PATH`.

Verify the peer first — if it is not runnable, every gate in Phase D and F is unprovable:

```powershell
cd 'C:\Users\DSarmie\Github Copilot\mockphone'
uv run mock-phone --help
```

---

## Build

```powershell
cd 'C:\Users\DSarmie\Github Copilot\copilot-worktrees\couchraoke-tv\DH1-special-parakeet'
.\gradlew.bat :app:assembleDebug
```

Expect roughly 25 s incremental, up to 3 min cold.

---

## Per-phase gates

Each phase's gate must pass fresh before the next phase starts. Use `--rerun-tasks` when you need evidence rather than an up-to-date check — a cached green is not evidence.

**Phase A — pure session core**

```powershell
.\gradlew.bat :app:testBranch `
  --src com.couchraoke.tv.domain.session.SessionRoster `
  --src com.couchraoke.tv.domain.session.GamePhaseMachine `
  --src com.couchraoke.tv.domain.session.JoinCodeGenerator `
  --src com.couchraoke.tv.domain.session.ConnectionIdAllocator `
  --test com.couchraoke.tv.domain.session.SessionRosterTest `
  --test com.couchraoke.tv.domain.session.GamePhaseMachineFixtureTest `
  --test com.couchraoke.tv.domain.session.JoinCodeGeneratorTest `
  --test com.couchraoke.tv.domain.session.ConnectionIdAllocatorTest
```

**Phase B — protocol logic**

```powershell
.\gradlew.bat :app:testBranch `
  --src com.couchraoke.tv.domain.control.HandshakeValidator `
  --src com.couchraoke.tv.domain.control.JoinCodeMatcher `
  --src com.couchraoke.tv.domain.control.ControlMessageCodec `
  --test com.couchraoke.tv.domain.control.HandshakeValidatorFixtureTest `
  --test com.couchraoke.tv.domain.control.JoinCodeMatcherTest `
  --test com.couchraoke.tv.domain.control.ControlMessageCodecFixtureTest
```

**Phase C — coordinator**: add `--src …SessionCoordinator` and `--test …SessionCoordinatorTest`.

**Phase D — transport**: the loopback gate, below. The adapters are not `--src` selected; see [research.md](./research.md) R8.

**Phase E — host surface**: add `--src …JoinViewModel --src …QrPayloadEncoder` with their tests plus `JoinOverlayBoundsTest`.

**Phase F**: the full command in [plan.md](./plan.md), then the loopback gate, then a device run.

---

## Loopback gate

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*LoopbackJoinGateTest*"
```

This starts the real Ktor server on an ephemeral port and drives the real `mockphone` process over `127.0.0.1`. It is the only thing that can satisfy SC-003, SC-005 and SC-010, because FR-039 bars an in-process fake from proving the transport works.

**Two peer exit statuses are assertions against us, not diagnostics:**

- **exit 4** — a refusal closed without delivering its reason. Violates FR-016/FR-017.
- **exit 6** — the peer neither got an answer nor a close within its timeout. The 5 s handshake deadline was not enforced (FR-017).

Both must fail the gate. Neither is an acceptable pass.

---

## Manual run against a real phone

1. Install and launch: `.\gradlew.bat :app:installDebug`
2. The song list appears in its empty state with a Join action in the header.
3. Press Join. The overlay shows the QR code and the join code beneath it.
4. From the peer machine:

```powershell
cd 'C:\Users\DSarmie\Github Copilot\mockphone'
uv run mock-phone --join-only --tv-host <tv-ip> --token SWIFT-PANDA
```

Discovery instead of an explicit host proves FR-004 end to end:

```powershell
uv run mock-phone --join-only --token SWIFT-PANDA
```

5. The overlay's connected count increments. Dismiss the overlay — the count is unaffected and no phone is disconnected (FR-033).

**Negative cases**, matching the gate:

```powershell
uv run mock-phone --join-only --token WRONG-WORD              # exit 3, invalid_token
uv run mock-phone --join-only --protocol-version 2 --token …  # exit 3, protocol_mismatch
uv run mock-phone --join-only --malformed-hello clientId --token …
uv run mock-phone --join-only --malformed-hello invalid-json --token …
uv run mock-phone --silent-handshake --join-timeout 10 --token …   # exit 3 within 5 s
```

**Capacity.** Because a disconnected device keeps its roster slot in this slice (FR-023/FR-024, and neither Kick nor song end exists yet), ten *sequential* `--join-only` runs fill the roster — none needs to stay connected:

```powershell
0..9 | ForEach-Object { uv run mock-phone --join-only --client-id "phone-0$_" --token … }
uv run mock-phone --join-only --client-id phone-99 --token …   # exit 3, session_full
uv run mock-phone --join-only --client-id phone-03 --token …   # exit 0 — reclaim at capacity
```

`--hold` is needed only where the *connected count* matters, since that counts live connections rather than roster entries:

```powershell
0..2 | ForEach-Object { Start-Process uv -ArgumentList "run","mock-phone","--join-only","--hold","30","--client-id","hold-0$_","--token","…" }
```

---

## Things that will bite you

- **`-Proborazzi.record=true` does not work in PowerShell.** It splits at the dot and Gradle reports `Task '.record=true' not found`. Use the dot-free alias: `-ProborazziRecord=true`.
- **Do not record a screenshot baseline in this slice.** The gate is in verify mode with no baselines. `plan.md` schedules baselines for Slice 3 and warns the previous ones were captured at twice the intended viewport; recording here would bake in the wrong scale. Layout is proved by `JoinOverlayBoundsTest` instead ([research.md](./research.md) R9).
- **`ACCESS_NETWORK_STATE` must be in the manifest** before `ConnectivityLocalAddressProvider` will return anything. Without it the lookup returns `null` and every session start fails through the FR-028 modal — which looks like a logic bug and is not.
- **Concurrent peers need distinct `--client-id`.** Sharing one makes each reclaim the same roster entry, so the roster never fills and the capacity case silently passes for the wrong reason.
- **A cached green is not evidence.** Use `--rerun-tasks` when reporting a gate.
