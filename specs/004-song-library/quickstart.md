# Quickstart: Song Library (004)

**Branch**: `004-song-library` | **Date**: 2026-03-16

Test scenarios and integration examples for the song-library domain.

---

## Scenario 1 — Library Lifecycle (US1)

```kotlin
val library = DefaultSongLibrary()

// Phone A connects with 3 songs
library.addPhone("phone-a", listOf(entry("phone-a", "a/song1.txt"), entry("phone-a", "a/song2.txt"), entry("phone-a", "a/song3.txt")))

// Phone B connects with 3 songs
library.addPhone("phone-b", listOf(entry("phone-b", "b/song1.txt"), entry("phone-b", "b/song2.txt"), entry("phone-b", "b/song3.txt")))

assert(library.getSortedSongs().size == 6)  // T3.1.1

// Phone A disconnects
library.removePhone("phone-a")
assert(library.getSortedSongs().size == 3)  // T3.1.4 — all Phone A songs gone
assert(library.getSongsByPhone("phone-a").isEmpty())

// Phone B rescans — manifest fetch replaces entries
library.addPhone("phone-b", listOf(entry("phone-b", "b/song1.txt")))
assert(library.getSortedSongs().size == 1)  // T3.1.5 — old entries replaced
```

---

## Scenario 2 — SongId Format (T3.1.2)

```kotlin
val entry = SongEntry(
    songId = "phone-abc::artist/songname/song.txt",
    phoneClientId = "phone-abc",
    relativeTxtPath = "artist/songname/song.txt",
    ...
)
assert(entry.songId == "phone-abc::artist/songname/song.txt")
```

---

## Scenario 3 — Validation via F01 Fixture (T3.2.1–T3.2.9)

```kotlin
// Acceptance test: SongDiscoveryAcceptanceTest
val discovery = SongDiscovery(UsdxParser(), SongIndexer())
val rootDir = Paths.get("src/test/resources/fixtures/library/song_discovery/songs_root")
val fileResolver = RelativeFileResolver(rootDir)

val entries = discovery.discoverFromDirectory(
    rootDir = rootDir,
    phoneClientId = "test-phone",
    fileResolver = fileResolver,
    readFile = { path -> Files.readString(path) },
)

// Assert against expected.discovery.json
val expected = Json.decodeFromString<List<ExpectedEntry>>(
    Files.readString(Paths.get("src/test/resources/fixtures/library/song_discovery/expected.discovery.json"))
)
expected.forEach { exp ->
    val actual = entries.first { it.relativeTxtPath == exp.songTxtRel }
    assertEquals(exp.isValid, actual.isValid)
    assertEquals(exp.invalidReasonCode, actual.invalidReasonCode)
    assertEquals(exp.invalidLineNumber, actual.invalidLineNumber)
    if (exp.artist != null) assertEquals(exp.artist, actual.artist)
    if (exp.resolvedAudioRel != null) /* check SongIndexer set correct audioUrl */
}
```

---

## Scenario 4 — Derived Fields (US3)

```kotlin
val indexer = SongIndexer()

// Duet: canMedley must be false
val duetResult = parser.parse("duet-song", "#TITLE:T\n#ARTIST:A\n#BPM:120\n#AUDIO:a.mp3\nP1\n: 0 4 0 la\nP2\n: 4 4 0 la\nE", hasAudioResolver)
val duetEntry = indexer.fromParseResult(duetResult, "phone", "duet/song.txt", 0L, "http://...", null, null, null, null, null, null)
assertTrue(duetEntry.isDuet)
assertFalse(duetEntry.canMedley)  // T3.3.3

// Non-duet with medley tags: canMedley=true
val medleyResult = parser.parse("medley-song", "#TITLE:T\n#ARTIST:A\n#BPM:120\n#AUDIO:a.mp3\n#MEDLEYSTARTBEAT:10\n#MEDLEYENDBEAT:80\n: 0 4 0 la\nE", hasAudioResolver)
val medleyEntry = indexer.fromParseResult(medleyResult, "phone", "song.txt", 0L, ...)
assertTrue(medleyEntry.canMedley)  // T3.3.4
assertEquals("tag", medleyEntry.medleySource.name.lowercase())
```

---

## Gradle Tasks

```bash
# Unit tests only (fast)
./gradlew :app:libraryUnitTest

# Acceptance tests (reads F01 fixture)
./gradlew :app:libraryAcceptanceTest

# Both
./gradlew :app:libraryTest

# Full suite (no regressions)
./gradlew :app:test
```

---

## Fixture Setup

The F01 fixture from `original_spec/fixtures/F01_song_discovery_validation_acceptance/` is copied to:
```
app/src/test/resources/fixtures/library/song_discovery/
  songs_root/          ← copied from F01/songs_root/
  expected.discovery.json  ← copied from F01/expected.discovery.json
```
