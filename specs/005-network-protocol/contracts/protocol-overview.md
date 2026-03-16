# Protocol Contracts: Network Protocol (005)

Full JSON schemas are normative in `original_spec/tv_couchraoke_spec.md` Appendix B.
This file summarises the TV-side contracts and any implementation constraints.

## Transport Channels

| Channel | Direction | Format | Port |
|---|---|---|---|
| WebSocket | bidirectional control | JSON | configured at session start |
| UDP | phone → TV | 16-byte binary (little-endian) | bound at session start, stable for session |
| HTTP GET `/manifest.json` | TV pulls from phone | JSON array of `SongEntry` | phone's `httpPort` from `hello` |

## Control Message Contract

- Every message: `type` (string) + `protocolVersion` (int, must be `1`) required
- Unknown `type` values → ignore + warn (except during handshake → fatal)
- `additionalProperties: false` per Appendix B schemas

## Handshake Sequence (normative)

```
Phone → TV:  hello          (clientId, deviceName, appVersion, protocolVersion, httpPort, capabilities)
TV checks:   token valid? protocolVersion==1? httpPort present? slots available? isLocked==false?
  ✗ any →    TV sends error(code=...) and closes
  ✓ all →    TV assigns connectionId (uint16, incrementing from 1)
             TV sends sessionState (with connectionId)
             TV fetches GET /manifest.json from phone
             TV calls SongLibrary.addPhone(clientId, entries)
             TV starts clock sync (5 exchanges × 100ms)
```

## Pitch Frame Wire Format

```
Offset  Size  Type     Field
  0      4    uint32   seq              (little-endian)
  4      4    int32    tvTimeMs         (little-endian)
  8      4    uint32   songInstanceSeq  (little-endian)
 12      1    uint8    playerId         (0=P1, 1=P2)
 13      1    uint8    midiNote         (0..127; 255=unvoiced)
 14      2    uint16   connectionId     (little-endian)
Total: 16 bytes exactly. Any other length → silently drop.
```

## Clock Sync Sequence

```
TV → Phone:    ping  { pingId, tTvSendMs }
Phone → TV:    pong  { pingId, tTvSendMs, tPhoneRecvMs, tPhoneSendMs }
TV → Phone:    clockAck { pingId, tTvRecvMs }

Phone computes: clockOffsetMs = ((tPhoneRecvMs - tTvSendMs) + (tPhoneSendMs - tTvRecvMs)) / 2
TV just sends clockAck immediately on pong receipt.
```

## Error Codes

| Code | Trigger |
|---|---|
| `invalid_token` | Missing or wrong join code |
| `protocol_mismatch` | `protocolVersion != 1` |
| `session_full` | Active connections ≥ `maxConnections` (= 10; T8.3.7) |
| `session_locked` | `ISessionGate.isLocked == true` (owned by feature 006) |

## Manifest Fetch

```
GET http://<phone-ip>:<httpPort>/manifest.json
Response: 200 OK, Content-Type: application/json, Cache-Control: no-cache
Body: JSON array of SongEntry (Appendix B.2.9)
On failure: retain prior catalog + show error toast
```
