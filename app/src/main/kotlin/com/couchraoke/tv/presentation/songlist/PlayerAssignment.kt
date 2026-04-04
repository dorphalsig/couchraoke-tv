package com.couchraoke.tv.presentation.songlist

import android.os.Parcelable
import com.couchraoke.tv.domain.library.SongEntry
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class PlayerAssignment(
    val mode: @RawValue SelectPlayersMode,
    val song: @RawValue SongEntry?, // non-null iff mode=SingleSong
    val medleyPlaylist: List<@RawValue SongEntry>?, // non-null iff mode=Medley
    val player1: @RawValue PhoneOption,
    val player1Difficulty: Difficulty,
    val player2: @RawValue PhoneOption?,
    val player2Difficulty: Difficulty?,
    val player1Part: DuetPart,
    val player2Part: DuetPart?,
) : Parcelable
