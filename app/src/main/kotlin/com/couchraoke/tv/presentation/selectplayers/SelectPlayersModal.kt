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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.presentation.common.InterruptionShell
import com.couchraoke.tv.ui.theme.BodySecondary
import com.couchraoke.tv.ui.theme.ButtonLabel
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.FieldLabel
import com.couchraoke.tv.ui.theme.PanelTitle
import com.couchraoke.tv.ui.theme.PrimaryModalPadding
import com.couchraoke.tv.ui.theme.PrimaryModalWidth
import com.couchraoke.tv.ui.theme.RadiusLarge
import com.couchraoke.tv.ui.theme.RadiusMedium
import com.couchraoke.tv.ui.theme.SectionTitle
import com.couchraoke.tv.ui.theme.SelectPlayersActionButtonWidth
import com.couchraoke.tv.ui.theme.Space12
import com.couchraoke.tv.ui.theme.Space24
import com.couchraoke.tv.ui.theme.Space8
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.SurfaceLevel2
import com.couchraoke.tv.ui.theme.TextDisabled
import com.couchraoke.tv.ui.theme.TextPrimary
import com.couchraoke.tv.ui.theme.TextSecondary

private val ModalShape = RoundedCornerShape(RadiusLarge)
private val SectionShape = RoundedCornerShape(RadiusMedium)
private val ModalSurface = SurfaceLevel2
private val SectionSurface = SurfaceLevel1
private val DisabledText = TextDisabled

@Composable
fun SelectPlayersModal(
    state: SelectPlayersState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenJoinQr: () -> Unit = {},
) {
    val noPhoneRecovery = state.noPhoneRecovery
    if (noPhoneRecovery != null) {
        SelectPlayersNoPhoneShell(
            recovery = noPhoneRecovery,
            onOpenJoinQr = onOpenJoinQr,
            onCancel = onCancel,
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.66f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(PrimaryModalWidth),
            shape = ModalShape,
            colors = SurfaceDefaults.colors(containerColor = ModalSurface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PrimaryModalPadding),
                verticalArrangement = Arrangement.spacedBy(Space24),
            ) {
                SelectPlayersHeader(state)
                SelectPlayersBody(
                    state = state,
                    onStart = onStart,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun SelectPlayersHeader(state: SelectPlayersState) {
    Column(verticalArrangement = Arrangement.spacedBy(Space8)) {
        Text(text = state.title, style = SectionTitle, color = TextPrimary)
        Text(text = state.subtitle, style = BodySecondary, color = TextSecondary)
    }
}

@Composable
private fun SelectPlayersBody(
    state: SelectPlayersState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
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
    SelectPlayersActions(canStart = state.canStart, onStart = onStart, onCancel = onCancel)
}

@Composable
private fun SelectPlayersNoPhoneShell(
    recovery: SelectPlayersNoPhoneRecovery,
    onOpenJoinQr: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InterruptionShell(
        title = recovery.title,
        bodyLines = listOf(recovery.body),
        modifier = modifier,
    ) {
        Button(onClick = onOpenJoinQr, modifier = Modifier.width(SelectPlayersActionButtonWidth)) {
            Text(recovery.primaryActionLabel, style = ButtonLabel)
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.width(SelectPlayersActionButtonWidth)) {
            Text("Cancel", style = ButtonLabel)
        }
    }
}

@Composable
private fun SelectPlayersActions(
    canStart: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space24)) {
        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.width(SelectPlayersActionButtonWidth),
        ) {
            Text("Start", style = ButtonLabel)
        }
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.width(SelectPlayersActionButtonWidth),
        ) {
            Text("Cancel", style = ButtonLabel)
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
                .padding(Space24),
            verticalArrangement = Arrangement.spacedBy(Space12),
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
    Row(horizontalArrangement = Arrangement.spacedBy(Space12), verticalAlignment = Alignment.CenterVertically) {
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

@Preview(name = "Select Players No Phones", widthDp = 1920, heightDp = 1080)
@Composable
fun SelectPlayersNoPhonesModalPreview() {
    CouchraokeTheme {
        SelectPlayersModal(
            state = SelectPlayersState(
                title = "SELECT PLAYERS",
                subtitle = "Demo Artist — Demo Song",
                playerOne = PlayerSelectionState(
                    playerId = PlayerId.P1,
                    selectedPhoneId = null,
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
                canStart = false,
                countdownEnabled = true,
                countdownSeconds = 3,
                noPhoneRecovery = SelectPlayersNoPhoneRecovery(),
            ),
            onStart = {},
            onCancel = {},
        )
    }
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
