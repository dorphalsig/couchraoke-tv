@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.selectplayers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.ui.theme.BodySecondary
import com.couchraoke.tv.ui.theme.ButtonLabel
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.FieldLabel
import com.couchraoke.tv.ui.theme.PanelTitle
import com.couchraoke.tv.ui.theme.SectionTitle
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.TextDisabled
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary

private val ModalShape = RoundedCornerShape(24.dp)
private val SectionShape = RoundedCornerShape(16.dp)
private val ModalSurface = SurfaceLevel2
private val SectionSurface = SurfaceLevel1
private val DisabledText = TextDisabled

@Composable
fun SelectPlayersModal(
    state: SelectPlayersState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.6f),
            shape = ModalShape,
            colors = SurfaceDefaults.colors(containerColor = ModalSurface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.title,
                        style = SectionTitle,
                        color = TextPrimary,
                    )
                    Text(
                        text = state.subtitle,
                        style = BodySecondary,
                        color = TextSecondary,
                    )
                }

                PlayerBlock(
                    title = "Player 1",
                    phoneText = state.playerOne.selectedPhoneId ?: "(none)",
                    difficultyText = state.playerOne.difficulty.displayName(),
                    required = true,
                    enabled = state.playerOne.enabled,
                    showDifficulty = true,
                )
                PlayerBlock(
                    title = "Player 2",
                    phoneText = state.playerTwo.selectedPhoneId ?: "(none)",
                    difficultyText = state.playerTwo.difficulty.displayName(),
                    required = false,
                    enabled = false,
                    showDifficulty = state.showPlayerTwoDifficulty,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onStart, enabled = state.canStart) {
                        Text("Start", style = ButtonLabel)
                    }
                    OutlinedButton(onClick = onCancel) {
                        Text("Cancel", style = ButtonLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBlock(
    title: String,
    phoneText: String,
    difficultyText: String,
    required: Boolean,
    enabled: Boolean,
    showDifficulty: Boolean,
) {
    Surface(shape = SectionShape, colors = SurfaceDefaults.colors(containerColor = SectionSurface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (required) "$title (required)" else title,
                style = PanelTitle,
                color = if (enabled) TextPrimary else DisabledText,
            )
            LabeledValue(label = "Phone", value = phoneText, enabled = enabled)
            if (showDifficulty) {
                LabeledValue(label = "Difficulty", value = difficultyText, enabled = enabled)
            }
            // !enabled && !required: Iteration 3 wires Player 2 duet selection; visible-disabled in Iter 1.
        }
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    enabled: Boolean,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = FieldLabel,
            color = if (enabled) TextSecondary else DisabledText,
        )
        Text(
            text = value,
            style = FieldLabel,
            color = if (enabled) TextPrimary else DisabledText,
        )
    }
}

private fun Difficulty.displayName(): String = when (this) {
    Difficulty.Easy -> "Easy"
    Difficulty.Medium -> "Medium"
    Difficulty.Hard -> "Hard"
}

@Preview(name = "Select Players", widthDp = 1920, heightDp = 1080)
@Composable
fun SelectPlayersModalPreview() {
    CouchraokeTheme {
        SelectPlayersModal(
            state = SelectPlayersState(
                title = "SELECT PLAYERS",
                subtitle = "Demo Artist — Demo Song",
                playerOne = PlayerSelectionState(
                    playerId = PlayerId.P1,
                    selectedPhoneId = "Living Room Phone",
                    difficulty = Difficulty.Medium,
                    enabled = true,
                ),
                playerTwo = PlayerSelectionState(
                    playerId = PlayerId.P2,
                    selectedPhoneId = null,
                    difficulty = Difficulty.Medium,
                    enabled = false,
                ),
                showPlayerTwoDifficulty = false,
                canStart = true,
                countdownEnabled = true,
                countdownSeconds = 3,
            ),
            onStart = {},
            onCancel = {},
        )
    }
}
