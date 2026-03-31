# Contract: Song List Navigation

**Owner**: Feature 007 (Song List UI)
**Consumed by**:
- VR-002 screenshot tests — to define the valid Song List entry paths and post-entry state guarantees
- Future Singing/Results features — to return to the Song List without introducing contradictory state assumptions
- Song List join widget — to obtain the canonical WebSocket join URL for QR encoding

---

## Purpose

Defines the minimal published seam needed for:
1. Song List return-path screenshot coverage (VR-002)
2. QR payload generation for the join widget

This contract is intentionally minimal. It does **not** define callback payloads or result-passing from other screens.

---

## Join endpoint provider

The Song List join widget requires the canonical full WebSocket URL for QR encoding.

```kotlin
fun interface IJoinEndpointProvider {
    /**
     * Returns the canonical WebSocket join URL for the active Song List session.
     * Example: ws://192.168.1.23:8080/?token=ABCD1234
     */
    fun joinUrl(): String
}
```

### Invariants

1. The returned URL MUST be the full WebSocket endpoint URL, not an mDNS service identifier.
2. The returned URL MUST include the raw session token as the `token` query parameter.
3. The returned URL MUST match the currently active TV-host listener address and port.
4. The UI consumes this as a read-only value; it does not derive host, port, or token itself.

---

## Revision 2 notes (2026-03-31)

The QR code and join code are now behind a `[ JOIN ]` button + pairing overlay (modal), rather than always visible in the left panel. The `IJoinEndpointProvider` contract is unchanged — the ViewModel still consumes it to generate the QR payload. The only difference is when the QR is rendered (on overlay open) rather than always.

---

## Non-goals

This contract does **not** define:
- navigation callback interfaces
- result payloads from Singing or Results
- Settings navigation
- device roster management

Those are outside the minimum seam required by feature 007.
