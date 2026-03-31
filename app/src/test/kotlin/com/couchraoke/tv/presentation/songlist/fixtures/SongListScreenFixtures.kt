package com.couchraoke.tv.presentation.songlist.fixtures

import com.couchraoke.tv.domain.library.SongEntry
import com.couchraoke.tv.domain.parser.MedleySource
import com.couchraoke.tv.domain.session.SessionState
import com.couchraoke.tv.presentation.songlist.Difficulty
import com.couchraoke.tv.presentation.songlist.DuetPart
import com.couchraoke.tv.presentation.songlist.ErrorModalState
import com.couchraoke.tv.presentation.songlist.PhoneOption
import com.couchraoke.tv.presentation.songlist.SelectPlayersDialogState
import com.couchraoke.tv.presentation.songlist.SelectPlayersMode
import com.couchraoke.tv.presentation.songlist.SongListUiState

object SongListScreenFixtures {
    private const val FIXED_TOKEN = "TEST-1234"

    fun emptyState() = SongListUiState(
        joinToken = FIXED_TOKEN,
        allSongs = emptyList(),
        filteredSongs = emptyList()
    )

    fun noSongsState() = emptyState()

    fun populatedState() = SongListUiState(
        joinToken = FIXED_TOKEN,
        allSongs = sampleSongs(),
        filteredSongs = sampleSongs()
    )

    fun filteredHitState() = populatedState().copy(
        searchQuery = "rock",
        filteredSongs = sampleSongs().filter { 
            it.title?.contains("rock", ignoreCase = true) == true || 
            it.artist?.contains("rock", ignoreCase = true) == true 
        }
    )

    fun filteredEmptyState() = populatedState().copy(
        searchQuery = "zzz",
        filteredSongs = emptyList()
    )

    fun selectPlayersNonDuetState(): SongListUiState {
        val songs = sampleSongs()
        val song = songs.first { !it.isDuet }
        return populatedState().copy(
            selectPlayersDialog = SelectPlayersDialogState(
                mode = SelectPlayersMode.SingleSong(song),
                song = song,
                availablePhones = listOf(
                    PhoneOption("p1", "Pixel 7"),
                    PhoneOption("p2", "iPhone 15")
                )
            )
        )
    }

    fun selectPlayersDuetBothState(): SongListUiState {
        val songs = sampleSongs()
        val song = songs.first { it.isDuet }
        val p1 = PhoneOption("p1", "Pixel 7")
        val p2 = PhoneOption("p2", "iPhone 15")
        return populatedState().copy(
            selectPlayersDialog = SelectPlayersDialogState(
                mode = SelectPlayersMode.SingleSong(song),
                song = song,
                availablePhones = listOf(p1, p2),
                player1Selection = p1,
                player2Selection = p2,
                player1Difficulty = Difficulty.Hard,
                player2Difficulty = Difficulty.Medium
            )
        )
    }

    fun selectPlayersDuetSoloState(): SongListUiState {
        val songs = sampleSongs()
        val song = songs.first { it.isDuet }
        val p1 = PhoneOption("p1", "Pixel 7")
        return populatedState().copy(
            selectPlayersDialog = SelectPlayersDialogState(
                mode = SelectPlayersMode.SingleSong(song),
                song = song,
                availablePhones = listOf(p1, PhoneOption("p2", "iPhone 15")),
                player1Selection = p1,
                soloPartSelection = DuetPart.P1
            )
        )
    }

    fun noPhonesBlockingState(): SongListUiState {
        val songs = sampleSongs()
        val song = songs.first()
        return populatedState().copy(
            selectPlayersDialog = SelectPlayersDialogState(
                mode = SelectPlayersMode.SingleSong(song),
                song = song,
                availablePhones = emptyList()
            )
        )
    }

    fun errorModalState() = populatedState().copy(
        errorModal = ErrorModalState(
            title = "Connection Error",
            bodyLine1 = "Could not reach the server.",
            bodyLine2 = "Please check your network settings."
        )
    )

    fun medleyVisibleState() = populatedState().copy(
        medleyPlaylist = sampleSongs().take(3)
    )

    fun medleyReorderState() = populatedState().copy(
        medleyPlaylist = sampleSongs().take(3),
        isReorderingMedleyIndex = 1
    )

    private fun sampleSongs(): List<SongEntry> = listOf(
        SongEntry(
            songId = "1",
            phoneClientId = "client1",
            relativeTxtPath = "rock1.txt",
            modifiedTimeMs = 123456789L,
            isValid = true,
            artist = "The Rockers",
            title = "Rock Anthem",
            album = "Classic Rock",
            isDuet = false,
            canMedley = true,
            txtUrl = "http://test.com/rock1.txt"
        ),
        SongEntry(
            songId = "2",
            phoneClientId = "client1",
            relativeTxtPath = "duet1.txt",
            modifiedTimeMs = 123456790L,
            isValid = true,
            artist = "Duo Stars",
            title = "Love Song",
            album = "Romance",
            isDuet = true,
            canMedley = false,
            txtUrl = "http://test.com/duet1.txt"
        ),
        SongEntry(
            songId = "3",
            phoneClientId = "client2",
            relativeTxtPath = "pop1.txt",
            modifiedTimeMs = 123456791L,
            isValid = true,
            artist = "Pop Queen",
            title = "Dance Now",
            album = "Summer Hits",
            isDuet = false,
            canMedley = true,
            txtUrl = "http://test.com/pop1.txt"
        ),
        SongEntry(
            songId = "4",
            phoneClientId = "client2",
            relativeTxtPath = "indie1.txt",
            modifiedTimeMs = 123456792L,
            isValid = true,
            artist = "Indie Band",
            title = "Quiet Night",
            album = "Forest",
            isDuet = false,
            canMedley = true,
            txtUrl = "http://test.com/indie1.txt"
        ),
        SongEntry(
            songId = "5",
            phoneClientId = "client1",
            relativeTxtPath = "rock2.txt",
            modifiedTimeMs = 123456793L,
            isValid = true,
            artist = "The Rockers",
            title = "Hard Rock",
            album = "Classic Rock",
            isDuet = true,
            canMedley = true,
            txtUrl = "http://test.com/rock2.txt"
        ),
        SongEntry(
            songId = "6",
            phoneClientId = "client3",
            relativeTxtPath = "jazz1.txt",
            modifiedTimeMs = 123456794L,
            isValid = true,
            artist = "Jazz Master",
            title = "Blue Note",
            album = "Midnight",
            isDuet = false,
            canMedley = false,
            txtUrl = "http://test.com/jazz1.txt"
        )
    )
}
