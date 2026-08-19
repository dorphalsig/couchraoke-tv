# Phase 0 Research: Phone Joins

**Feature**: `003-phone-joins` | **Date**: 2026-08-19

Every unknown that blocked the design is resolved below. Each entry states the decision, why it was chosen, and what was rejected.

---

## R1 — QR code generation

**Decision**: `com.google.zxing:core:3.5.3`, encode-only, rendered by our own composable.

`QrPayloadEncoder` (pure) builds the URL string. `QrBitmapRenderer` calls `QRCodeWriter.encode(...)` to get a `BitMatrix` and draws it into a Compose `ImageBitmap`. ZXing's `BitMatrix` is a plain boolean grid with no Android dependency, so the encode step stays testable on the JVM.

**Rationale**: Nothing in the catalogue can produce a QR code, and FR-007/FR-030 require one. `core` is a single ~500 KB pure-Java artifact with no transitive dependencies.

**Alternatives rejected**:
- `journeyapps:zxing-android-embedded` — a scanner. It pulls a camera `Activity`, `CameraX` and a preview surface for a job that never reads a code. Wrong direction and a large surface for nothing.
- `io.github.alexzhirkevich:qrose` — reconsidered late, after it was found already integrated on the superseded `002-solo-sing-playback` branch, and rejected on evidence. QRose is attractive on paper: no dependencies beyond `compose.ui`, vector output rather than a raster bitmap, and a `Painter` straight from composition, which would delete our `BitMatrix` → `ImageBitmap` step. But its public API exposes shapes, colours, logo and error-correction level only — **there is no margin option and no way to read back the module count**. FR-030 and the bounds test both pin the quiet zone in modules, and QRose cannot express either quantity. The prior integration shows the consequence: it approximated with `moduleSizePx = maxOf(1, sizePx / 100)` and a `0.92f` content scale, then asserted `contentBounds.left >= moduleSizePx * 4` against that approximation. For our payload the real code is version 4 (33 modules), so at `sizePx = 400` a module is ~11 px, not 4 — the test passed while the actual quiet zone was roughly 1.4 modules, about a third of the requirement. The test confirmed its own arithmetic rather than the QR, which is precisely the failure mode Principle IV exists to catch.
- Hand-rolling QR encoding — Reed-Solomon error correction and mask-pattern selection are a week of work with a silent failure mode.

**Quiet zone**: FR-030 demands at least four modules. `QRCodeWriter` honours `EncodeHintType.MARGIN`, so we pass `4` explicitly rather than relying on its default, and `BitMatrix.width` gives the module count exactly, so the bounds test asserts a measured margin rather than an assumed one.

---

## R2 — WebSocket control server

**Decision**: Ktor server CIO with the WebSockets plugin. Already in the catalogue and already on the `:app` classpath.

Bind to `0.0.0.0` so every interface accepts (FR-008), one route, token read from the `token` query parameter.

**Rationale**: No new dependency, no new risk. CIO is coroutine-native, so the handshake deadline is a `withTimeout` rather than a thread and a timer. It has no servlet container and no reflection, which keeps startup fast enough for the loopback gate to spin a server per test.

**Alternatives rejected**:
- Ktor Netty — heavier, and its thread model buys throughput this slice will never need.
- Raw `ServerSocket` with a hand-written RFC 6455 implementation — reimplementing framing, masking and close codes when FR-016 depends on close code `1008` being exactly right is gratuitous risk.

**Close semantics**: `CloseReason(CloseReason.Codes.VIOLATED_POLICY, reasonCode)` gives code 1008 with the reason string equal to the refusal's enum value, which is what the peer reports as `closeReason`. The `error` frame is sent first and flushed before `close` is called — a refusal that closes without delivering its reason makes the peer exit 4, which the gate treats as a failure.

---

## R3 — Advertising exactly one IPv4 address

**Decision**: `ConnectivityManager.getActiveNetwork()` → `getLinkProperties()` → the first `linkAddress` whose address is an `Inet4Address` and is neither loopback nor link-local. Requires the `ACCESS_NETWORK_STATE` permission, which the manifest currently lacks and this plan adds.

If there is no active network, or it yields no such address, session start fails through the FR-028 blocking-notice path.

**Rationale**: FR-008 requires the address of the network the TV is *currently using for local traffic* — a single address, not a list. `NetworkInterface.getNetworkInterfaces()` cannot answer that: it enumerates every interface with no notion of which one is active, so a box with both Ethernet and Wi-Fi up returns two candidates with no way to rank them. `getActiveNetwork()` answers exactly the question asked. This matters because SC-002 requires the address the phone dialled to be one the TV is actually reachable on, in a single scan.

**Alternatives rejected**:
- `WifiManager.getConnectionInfo().ipAddress` — returns 0 on Ethernet, which is how most TV boxes are connected.
- Advertising every address and letting the phone try each — FR-008 forbids a list, and it would turn one scan into several attempts.

**Isolation**: the whole lookup is behind `LocalAddressProvider`, so the domain sees `Inet4Address?` and never `ConnectivityManager`.

---

## R4 — mDNS registration

**Decision**: jmDNS 3.6.2 (already in the catalogue), `JmDNS.create(inetAddress, hostName)` bound to the address from R3, registering `ServiceInfo.create("_karaoke._tcp.local.", "KaraokeTV-<noun>", port, 0, 0, mapOf("code" to joinCode, "v" to "1"))`.

**Rationale**: Already a declared dependency, so no governance question. Binding jmDNS to the R3 address rather than the wildcard makes the announcement's address and the QR's address the same value by construction, instead of two lookups that could disagree.

**Alternatives rejected**:
- `NsdManager` — the platform API. Its callback model is awkward to make deterministic, its behaviour varies across vendor Android TV builds, and jmDNS is already a project dependency, so `NsdManager` would add a second discovery mechanism for no gain.

**Instance-name collisions**: FR-004 requires `KaraokeTV-<noun>` to be unique on the LAN. jmDNS renames on conflict by appending a suffix. That would desynchronise the advertised instance name from the join code's noun, so `JmdnsSessionAnnouncer` reads back the registered name and reports it; a mismatch is a session-start failure rather than a silent rename.

---

## R5 — Multicast lock lifetime

**Decision**: `MulticastLease` is a domain port with `acquire()`/`release()`; `WifiMulticastLease` implements it over `WifiManager.createMulticastLock`. It is acquired when the session starts and released when the session ends — owned by the session, not by the activity.

**Rationale**: FR-005 ties the lock to the session's duration. `MainActivity` currently acquires it in `onStart` and releases in `onStop`, which is wrong: an Android TV activity is stopped whenever the user switches inputs or opens the system overlay, and dropping multicast there would make the TV silently undiscoverable while the session is still live and phones are still connected. Readiness item 9 records this; the fix belongs to Phase D.

**Alternatives rejected**:
- Keeping activity scope — breaks FR-005 as described.
- A foreground service — correct for a backgrounded session, but this slice has no requirement to survive the app leaving the foreground, and a service is a second risky new thing.

---

## R6 — Handshake deadline

**Decision**: `withTimeoutOrNull(5.seconds)` around the wait for the first frame. On expiry, send `error` with `invalid_message`, then close 1008 with reason `invalid_message`.

**Rationale**: The pending connection has no device identity, so it holds no roster slot and appears in no list (FR-017). Coroutine timeout ties the deadline to the read rather than to a separate timer that could outlive the connection. The spec's Assumptions section settles the "deliver an error or close silently" question in favour of delivering, which the peer reports as exit 3; a silent close would report exit 4, and the gate treats exit 4 as a violation of FR-016.

**Testing**: `kotlinx-coroutines-test` gives the domain-side deadline test virtual time. The transport-side proof is the peer's `--silent-handshake`, which measures real elapsed time against a real socket.

---

## R7 — Join code word lists

**Decision**: Two bundled Kotlin `List<String>` constants in `JoinCodeGenerator` — roughly 64 adjectives and 64 nouns, all single words, unambiguous when read aloud from across a room.

`JoinCodeGenerator` takes a `Random` in its constructor so tests seed it deterministically. Uniqueness per session (FR-003) is enforced by the generator holding the set of codes it has already issued this process.

**Rationale**: 64 × 64 = 4096 combinations is ample for one TV's lifetime and keeps both halves short enough to read at TV distance. Constants rather than an `assets/` file because a resource read would drag Android's `AssetManager` into a class that is otherwise pure and needs to stay JVM-testable.

**Word selection constraint**: no homophones (`night`/`knight`), no words whose first letter is ambiguous when spoken, and no noun that could collide with another device's mDNS instance name pattern.

---

## R8 — Keeping adapters out of the coverage selection

**Decision**: `KtorControlTransport`, `JmdnsSessionAnnouncer`, `ConnectivityLocalAddressProvider` and `WifiMulticastLease` are never passed to `--src`. Each is a direct translation from a port method to a framework call with no branching beyond null handling.

**Rationale**: The `testBranch` gate enforces 80% line coverage on selected classes. These four cannot reach it from JVM tests without an in-process fake, and FR-039 explicitly bars a fake from satisfying a claim that the transport works. Rather than weaken the gate, the proof moves: the loopback gate exercises all four against a real peer over a real socket. Coverage is relocated, not skipped, and every decision they might otherwise have contained lives in a selected pure class instead.

**Consequence for the design**: this is the reason `HandshakeValidator`, `ControlMessageCodec` and `SessionCoordinator` exist as separate classes rather than as logic inside the Ktor route handler. The coverage gate is what forces the boundary to be real.

---

## R9 — Layout verification without a screenshot baseline

**Decision**: `JoinOverlayBoundsTest` asserts structure numerically using `onNodeWithTag(...).getUnclippedBoundsInRoot()` under the pinned `w960dp-h540dp-land-television-xhdpi-notouch` qualifier. No Roborazzi baseline is recorded in this slice.

Assertions: every expected element is present; nothing extends beyond 960 × 540 dp; the QR occupies between 30% and 55% of the shorter viewport dimension, so it is the dominant element (FR-030); the join code sits directly beneath the QR with no element between them; and no node overlaps the QR's bounds expanded by its quiet zone (FR-031).

**Rationale**: The user asked whether a model could eyeball that layouts hold their proportions and that every element is present. That splits cleanly. Proportion and presence are numeric relationships, and once written as numbers they are deterministic and belong in CI. Visual judgement — does this look right on a TV — is a one-time act at baseline creation, not something to re-run per commit.

The screenshot gate is now correctly in verify mode and has no baselines to compare against. `plan.md` schedules the screenshot gate for Slice 3 and warns that the previous baselines were recorded at twice the intended viewport, so recording one here would bake in the wrong scale before Slice 3 can correct it.

---

## R10 — Session-start failure has no phase to enter

**Decision**: `SessionStartFailure` surfaces as a blocking modal on the song-selection surface. The game phase stays `Open` and no transition is attempted.

**Rationale**: This was inferred during clarification and is now confirmed against the fixture. `fixtures/F22_gamephase_fsm_transitions/expected.transitions.json` lists `Preparing→Error`, `Countdown→Error` and `Live→Error` as the only edges into `Error`. There is no `Open→Error`. Entering `Error` from `Open` would be an invalid transition, and FR-026 requires invalid transitions to be rejected rather than silently applied — so a session-start failure cannot be modelled as a phase change without breaking the state machine. FR-028's modal is the only correct shape.

---

## R11 — The superseded implementation on `002-solo-sing-playback`

**Decision**: Reference only. Slice 1 is implemented fresh against `tasks.md`; no code is carried over, and no dependency choice is inherited from it.

An unmerged branch, `origin/002-solo-sing-playback`, contains roughly 800 lines covering part of this ground: a Ktor CIO WebSocket accepting `?token=`, a `hello` → `sessionState` exchange, four refusal codes, a jmDNS advertiser, a QR renderer and a join overlay. It is not on `master`. Its substantive work is authored 2026-04-29 → 2026-05-05 and was pushed much later, so the recent push date is not evidence of currency — check `%ad`, not `%cd`.

**Rationale**: it predates the current fixture set and diverges from this spec in eight ways, each of which is a gate we would fail:

1. Refusals close the socket with a bare `close()`, so the peer sees a normal closure instead of `1008` plus a reason string (FR-016).
2. There is no `invalid_message` path at all — a malformed `hello` returns null and is reported as `protocol_mismatch` (FR-011, and two F20 cases).
3. Refusal text is generic (`"Protocol version mismatch."`) where F20 pins exact strings.
4. No handshake deadline (FR-017).
5. Disconnect deletes the device rather than keeping its roster entry, and reclaim matches live connections only — which FR-021 forbids outright. US3 is absent.
6. `sessionState` omits `connectedDevices`, implementing the drifted F20 fixture rather than the normative B.2.2 schema. This independently confirms Observation 8 and shows the drift reaching code.
7. The mDNS instance name is `KaraokeTV-<last 4 of code>`, not `KaraokeTV-<noun>`, and the registered name is never read back, so a jmDNS collision rename would go unnoticed (FR-005).
8. DTOs, validation and admission all live inside the Ktor adapter — the arrangement R8's split exists to avoid, since the coverage gate cannot reach it without the fakes FR-039 bars.

Its QR integration was evaluated seriously and rejected on measurement; see R1.

**Worth knowing**: that branch also carries a constitution marked v2.0.0 (amended 2026-08-18) whose Principle IV drops `testBranch` as the single gate. `master` is still v1.0.0. This is the bump Observation 2 records as claimed-but-absent — it exists, but not on the main line. This spec satisfies both versions, so nothing here depends on which one lands.

---



Everything Slice 1 needs is either already in `gradle/libs.versions.toml` or listed in the plan's Dependency Governance table. No `NEEDS CLARIFICATION` remains.

| Need | Status |
|---|---|
| WebSocket server | `ktor-server-cio`, `ktor-server-websockets` — present |
| JSON | `kotlinx-serialization-json` — present |
| mDNS | `jmdns` — present |
| Coroutines | `kotlinx-coroutines-android` — present |
| TV UI | Compose BOM, `androidx-tv-foundation`, `androidx-tv-material` — present |
| UI test | `androidx-compose-ui-test-junit4` — present |
| QR | ZXing `core` — **to add** |
| ViewModel in Compose | `lifecycle-viewmodel-compose` — **to add** |
| Virtual-time tests | `kotlinx-coroutines-test` — **to add** |

## Manifest change

`android.permission.ACCESS_NETWORK_STATE` must be added for R3. `CHANGE_WIFI_MULTICAST_STATE` (R5), `ACCESS_LOCAL_NETWORK` (FR-006) and `INTERNET` are already declared.
