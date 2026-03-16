# Contract: SongLibrary Interface

**Package**: `com.couchraoke.tv.domain.library`

## Interface

```kotlin
interface SongLibrary {
    /** Replace all entries for [clientId] with [entries]. If entries is empty, effectively removes the phone. */
    fun addPhone(clientId: String, entries: List<SongEntry>)

    /** Remove all entries attributed to [clientId]. No-op if phone was not in library. */
    fun removePhone(clientId: String)

    /** All songs sorted by Artist → Album → Title (case-insensitive). Album=null sorts as "". */
    fun getSortedSongs(): List<SongEntry>

    /** Look up a single song by its stable songId. Returns null if not found. */
    fun getSongById(songId: String): SongEntry?

    /** All songs from a specific phone. Returns empty list if phone not in library. */
    fun getSongsByPhone(clientId: String): List<SongEntry>
}
```

## Invariants

1. After `addPhone(clientId, entries)`, `getSongsByPhone(clientId).size == entries.size`
2. After `removePhone(clientId)`, `getSongsByPhone(clientId).isEmpty()`
3. `getSortedSongs()` returns songs from all phones combined and sorted
4. `getSongById(entry.songId)` returns that entry (for any entry in the library)
5. Two successive `addPhone(clientId, ...)` calls — only the second call's entries are present

## Contract: SongIndexer

```kotlin
object SongIndexer {
    fun fromParseResult(
        parseResult: ParseResult,
        phoneClientId: String,
        relativeTxtPath: String,
        modifiedTimeMs: Long,
        txtUrl: String,
        audioUrl: String?,
        videoUrl: String?,
        coverUrl: String?,
        backgroundUrl: String?,
        instrumentalUrl: String?,
        vocalsUrl: String?,
    ): SongEntry
}
```

**Invariants**:
- `songId = phoneClientId + "::" + relativeTxtPath`
- `isValid = parseResult.parsedSong.isValid`
- `invalidReasonCode` maps from the first ERROR `DiagnosticCode` per the mapping table in data-model.md
- `canMedley = !isDuet && medleySource == MedleySource.EXPLICIT`

## Contract: SongDiscovery

```kotlin
class SongDiscovery(parser: UsdxParser, indexer: SongIndexer) {
    fun discoverFromDirectory(
        rootDir: Path,
        phoneClientId: String,
        fileResolver: FileResolver,
        readFile: (Path) -> String = { Files.readString(it) },
    ): List<SongEntry>
}
```

**Invariants**:
- Scans all `.txt` files recursively under `rootDir`
- `relativeTxtPath` for each entry is relative to `rootDir`, normalized with `/`, no leading `/`
- Entries are returned in discovery order (undefined, non-sorted)
- The returned list may contain both valid and invalid entries
