package com.couchraoke.tv.presentation.songlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.ui.theme.BodySecondary
import com.couchraoke.tv.ui.theme.PanelTitle
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary

@NoCoverageGenerated
@Composable
internal fun EmptyStateCard(emptyState: SongListEmptyState) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = emptyState.title, style = PanelTitle, color = TextPrimary)
            Text(text = emptyState.body, style = BodySecondary, color = TextSecondary)
        }
    }
}
