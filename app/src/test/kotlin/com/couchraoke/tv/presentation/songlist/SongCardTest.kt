package com.couchraoke.tv.presentation.songlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.couchraoke.tv.domain.library.IndexedSong
import com.couchraoke.tv.fixtures.SoloSingFixtures
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.SongCardCompactImageSize
import com.couchraoke.tv.ui.theme.SongCardImageHeight
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun instrumentalChipUsesManifestFlagNotGenre() {
        val instrumentalByFlag = indexedSong(
            songId = "song-instrumental-flag",
            entry = SoloSingFixtures.songEntry(
                genre = "Pop",
                hasInstrumental = true,
                hasVideo = false,
                videoUrl = null,
            ),
        )
        val instrumentalByGenreOnly = indexedSong(
            songId = "song-instrumental-genre",
            entry = SoloSingFixtures.songEntry(
                genre = "Instrumental",
                hasInstrumental = false,
                hasVideo = false,
                videoUrl = null,
            ),
        )

        setSongCards(instrumentalByFlag, instrumentalByGenreOnly)

        composeRule.onAllNodesWithTag(
            "song-card-tag-${instrumentalByFlag.songId}-I",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onAllNodesWithTag(
            "song-card-tag-${instrumentalByGenreOnly.songId}-I",
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    @Test(timeout = 30_000)
    fun compactArtworkUsesSquareThumbnail() {
        val song = indexedSong(songId = "song-compact-square", entry = SoloSingFixtures.songEntry())

        setSongCards(song, compact = true)

        composeRule.onNodeWithTag("song-card-artwork-${song.songId}", useUnmergedTree = true)
            .assertWidthIsEqualTo(SongCardCompactImageSize)
            .assertHeightIsEqualTo(SongCardCompactImageSize)
    }

    @Test(timeout = 30_000)
    fun tagPriorityShowsFirstThreeOfDuetMedleyRapInstrumentalVideo() {
        val song = indexedSong(
            songId = "song-all-tags",
            entry = SoloSingFixtures.songEntry(
                isDuet = true,
                hasRap = true,
                hasVideo = true,
                hasInstrumental = true,
                canMedley = true,
                medleySource = "tag",
                medleyStartBeat = 1,
                medleyEndBeat = 4,
                videoUrl = SoloSingFixtures.assetUrl("/video.mp4"),
            ),
        )

        setSongCards(song)

        composeRule.onAllNodesWithTag(
            "song-card-tag-${song.songId}-D",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onAllNodesWithTag(
            "song-card-tag-${song.songId}-M",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onAllNodesWithTag(
            "song-card-tag-${song.songId}-R",
            useUnmergedTree = true,
        ).assertCountEquals(1)
        composeRule.onAllNodesWithTag(
            "song-card-tag-${song.songId}-I",
            useUnmergedTree = true,
        ).assertCountEquals(0)
        composeRule.onAllNodesWithTag(
            "song-card-tag-${song.songId}-V",
            useUnmergedTree = true,
        ).assertCountEquals(0)
    }

    private fun setSongCards(vararg songs: IndexedSong, compact: Boolean = false) {
        composeRule.setContent {
            CouchraokeTheme {
                Column(modifier = Modifier.width(320.dp)) {
                    songs.forEach { song ->
                        SongCard(
                            song = song,
                            focusTargets = SongListFocusTargets(
                                search = FocusRequester(),
                                firstCard = FocusRequester(),
                                playMedley = FocusRequester(),
                                randomMedley = FocusRequester(),
                            ),
                            focusRequester = null,
                            useLeftPanelTarget = false,
                            onFocused = {},
                            onSelected = {},
                            cardImageHeight = if (compact) {
                                SongCardCompactImageSize
                            } else {
                                SongCardImageHeight
                            },
                            cardImageWidth = if (compact) SongCardCompactImageSize else null,
                        )
                    }
                }
            }
        }
    }

    private fun indexedSong(songId: String, entry: com.couchraoke.tv.fixtures.SongEntryFixture): IndexedSong =
        SoloSingFixtures.indexedSong(songId = songId, entry = entry)
}
