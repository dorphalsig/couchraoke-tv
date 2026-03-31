package com.couchraoke.tv.presentation.songlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text

@Suppress("FunctionNaming")
@Composable
fun ActionButtons(
    hasFilteredSongs: Boolean,
    hasFilteredDuets: Boolean,
    hasRandomMedleyEnabled: Boolean,
    onRandomSong: () -> Unit,
    onRandomDuet: () -> Unit,
    onRandomMedley: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onRandomSong,
            enabled = hasFilteredSongs,
            modifier = Modifier.alpha(if (hasFilteredSongs) 1f else 0.38f)
        ) {
            Text("Random Song")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onRandomDuet,
            enabled = hasFilteredDuets,
            modifier = Modifier.alpha(if (hasFilteredDuets) 1f else 0.38f)
        ) {
            Text("Random Duet")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onRandomMedley,
            enabled = hasRandomMedleyEnabled,
            modifier = Modifier.alpha(if (hasRandomMedleyEnabled) 1f else 0.38f)
        ) {
            Text("Sing Random Medley")
        }
    }
}
