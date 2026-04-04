package com.couchraoke.tv.presentation.songlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text

@Suppress("FunctionNaming")
@Composable
fun ActionButtons(
    hasFilteredSongs: Boolean,
    hasFilteredDuets: Boolean,
    onRandomSong: () -> Unit,
    onRandomDuet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onRandomSong,
            enabled = hasFilteredSongs
        ) {
            Text("Random Song")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onRandomDuet,
            enabled = hasFilteredDuets
        ) {
            Text("Random Duet")
        }
    }
}
