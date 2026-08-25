package com.couchraoke.tv.presentation.songlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.ui.theme.CouchraokeTheme

// Stable Compose test tags for SongListScreen so later tests (T061) can assert by tag.
const val SONG_LIST_HEADER_JOIN_ACTION_TAG = "song_list_header_join_action"
const val SONG_LIST_EMPTY_STATE_TAG = "song_list_empty_state"

/**
 * Song-selection shell (FR-029). No library exists in this slice, so the surface always
 * renders its empty state; the header Join action is the only interactive element and opens
 * the join overlay (wired by a later slice/task, not this one).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SongListScreen(onJoinClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Song List")
                Button(
                    onClick = onJoinClick,
                    modifier = Modifier.testTag(SONG_LIST_HEADER_JOIN_ACTION_TAG),
                ) {
                    Text(text = "Join")
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(SONG_LIST_EMPTY_STATE_TAG),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "No songs yet")
            }
        }
    }
}

@Preview(name = "Song List - Empty", widthDp = 960, heightDp = 540)
@Composable
fun SongListScreenEmptyPreview() {
    CouchraokeTheme {
        SongListScreen(onJoinClick = {})
    }
}
