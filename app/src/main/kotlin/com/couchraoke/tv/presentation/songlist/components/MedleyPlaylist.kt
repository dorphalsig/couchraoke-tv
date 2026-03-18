package com.couchraoke.tv.presentation.songlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.domain.library.SongEntry

@Suppress("LongMethod", "LongParameterList", "FunctionNaming") // Composable: UI structure is inherently verbose
@Composable
fun MedleyPlaylist(
    songs: List<SongEntry>,
    reorderingIndex: Int?,
    onRowClick: (Int) -> Unit,
    onRowLongClick: (Int) -> Unit,
    onReorderUp: (Int) -> Unit,
    onReorderDown: (Int) -> Unit,
    onReorderCancel: () -> Unit,
    onPlayMedley: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Medley Playlist (${songs.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Gray.copy(alpha = 0.1f))
        ) {
            itemsIndexed(songs) { index, song ->
                val isReordering = reorderingIndex == index
                Surface(
                    onClick = { onRowClick(index) },
                    onLongClick = { onRowLongClick(index) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .background(
                                if (isReordering) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${song.artist}  ${song.title}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (isReordering) {
                            Row {
                                IconButton(onClick = { onReorderUp(index) }) {
                                    Text("↑")
                                }
                                IconButton(onClick = { onReorderDown(index) }) {
                                    Text("↓")
                                }
                                IconButton(onClick = { onReorderCancel() }) {
                                    Text("✕")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPlayMedley,
            enabled = songs.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Play Medley")
        }
    }
}
