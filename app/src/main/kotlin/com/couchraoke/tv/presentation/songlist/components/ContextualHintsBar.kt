package com.couchraoke.tv.presentation.songlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.couchraoke.tv.presentation.songlist.HintMode

@Suppress("FunctionNaming")
@Composable
fun ContextualHintsBar(
    hint: HintMode?,
    modifier: Modifier = Modifier,
) {
    val hintText = when (hint) {
        HintMode.SongTile -> "OK = Sing   Long-Press OK = Add to Medley"
        HintMode.MedleyRow -> "OK = Reorder   Long-Press OK = Delete"
        HintMode.ReorderMode -> "Up/Down = Move   OK = Accept   Back = Cancel"
        null -> null
    }

    if (hintText == null) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = hintText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
