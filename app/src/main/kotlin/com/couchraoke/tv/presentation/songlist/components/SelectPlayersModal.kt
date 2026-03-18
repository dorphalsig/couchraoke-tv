package com.couchraoke.tv.presentation.songlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.presentation.songlist.Difficulty
import com.couchraoke.tv.presentation.songlist.DuetPart
import com.couchraoke.tv.presentation.songlist.PhoneOption
import com.couchraoke.tv.presentation.songlist.SelectPlayersDialogState
import com.couchraoke.tv.presentation.songlist.SelectPlayersMode

@Suppress("LongMethod", "LongParameterList", "FunctionNaming") // Composable: UI structure requires many parameters
@Composable
fun SelectPlayersModal(
    state: SelectPlayersDialogState,
    onPlayer1Selected: (PhoneOption) -> Unit,
    onPlayer1DifficultySelected: (Difficulty) -> Unit,
    onPlayer2Selected: (PhoneOption?) -> Unit,
    onPlayer2DifficultySelected: (Difficulty) -> Unit,
    onSoloDuetPartSelected: (DuetPart) -> Unit,
    onSwapParts: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.widthIn(min = 480.dp, max = 720.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                // Title
                Text(text = "Select Players", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                // Subtitle from mode
                val subtitle = when (val mode = state.mode) {
                    is SelectPlayersMode.SingleSong ->
                        "${mode.song.title.orEmpty()} — ${mode.song.artist.orEmpty()}"
                    is SelectPlayersMode.Medley -> "Medley — ${mode.count} songs"
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))

                if (state.availablePhones.isEmpty()) {
                    NoPhonesContent(onCancel = onCancel)
                } else {
                    SelectionContent(
                        state = state,
                        onPlayer1Selected = onPlayer1Selected,
                        onPlayer1DifficultySelected = onPlayer1DifficultySelected,
                        onPlayer2Selected = onPlayer2Selected,
                        onPlayer2DifficultySelected = onPlayer2DifficultySelected,
                        onSoloDuetPartSelected = onSoloDuetPartSelected,
                        onSwapParts = onSwapParts,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        OutlinedButton(onClick = onCancel) { Text("Cancel") }
                        Button(
                            onClick = onStart,
                            enabled = !state.isLoading && state.player1Selection != null,
                        ) { Text("Start") }
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun NoPhonesContent(onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "⚠ No phones connected.",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Connect phones in Settings to sing.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { /* Settings > Connect Phones */ }) {
            Text("Open Settings > Connect Phones")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel) { Text("Cancel") }
    }
}

@Suppress("LongMethod", "LongParameterList", "FunctionNaming") // Composable: UI structure requires many parameters
@Composable
private fun SelectionContent(
    state: SelectPlayersDialogState,
    onPlayer1Selected: (PhoneOption) -> Unit,
    onPlayer1DifficultySelected: (Difficulty) -> Unit,
    onPlayer2Selected: (PhoneOption?) -> Unit,
    onPlayer2DifficultySelected: (Difficulty) -> Unit,
    onSoloDuetPartSelected: (DuetPart) -> Unit,
    onSwapParts: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PlayerRow(
            label = "Player 1",
            phones = state.availablePhones,
            selected = state.player1Selection,
            onSelect = onPlayer1Selected,
            difficulty = state.player1Difficulty,
            onDifficultySelect = onPlayer1DifficultySelected,
            enabled = true,
        )

        val showPlayer2 = state.mode is SelectPlayersMode.SingleSong
        if (showPlayer2) {
            val isDuet = (state.mode as SelectPlayersMode.SingleSong).song.isDuet
            PlayerRow(
                label = "Player 2",
                phones = state.availablePhones,
                selected = state.player2Selection,
                onSelect = { onPlayer2Selected(it) },
                difficulty = state.player2Difficulty,
                onDifficultySelect = onPlayer2DifficultySelected,
                enabled = isDuet,
                allowNone = true,
                onSelectNone = { onPlayer2Selected(null) },
            )
            if (isDuet) {
                if (state.player1Selection != null && state.player2Selection != null) {
                    OutlinedButton(onClick = onSwapParts) { Text("Swap Parts") }
                } else if (state.player1Selection != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Solo Part: ", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(8.dp))
                        RadioToggle(
                            label = "P1",
                            selected = state.soloPartSelection == DuetPart.P1,
                            onSelect = { onSoloDuetPartSelected(DuetPart.P1) },
                        )
                        Spacer(Modifier.width(8.dp))
                        RadioToggle(
                            label = "P2",
                            selected = state.soloPartSelection == DuetPart.P2,
                            onSelect = { onSoloDuetPartSelected(DuetPart.P2) },
                        )
                    }
                }
            }
        }
    }
}

@Suppress("LongParameterList", "FunctionNaming") // Composable: UI structure requires many parameters
@Composable
private fun PlayerRow(
    label: String,
    phones: List<PhoneOption>,
    selected: PhoneOption?,
    onSelect: (PhoneOption) -> Unit,
    difficulty: Difficulty,
    onDifficultySelect: (Difficulty) -> Unit,
    enabled: Boolean,
    allowNone: Boolean = false,
    onSelectNone: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SimpleDropdown(
                label = selected?.displayName ?: if (allowNone) "None" else "Select phone",
                items = phones.map { it.displayName },
                enabled = enabled,
                onItemSelected = { idx -> onSelect(phones[idx]) },
                extraItem = if (allowNone) "None" else null,
                onExtraItemSelected = onSelectNone,
                modifier = Modifier.weight(1f),
            )
            SimpleDropdown(
                label = difficulty.name,
                items = Difficulty.entries.map { it.name },
                enabled = enabled && selected != null,
                onItemSelected = { idx -> onDifficultySelect(Difficulty.entries[idx]) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Suppress("LongParameterList", "LongMethod", "FunctionNaming") // Composable: UI structure requires many parameters
@Composable
private fun SimpleDropdown(
    label: String,
    items: List<String>,
    enabled: Boolean,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    extraItem: String? = null,
    onExtraItemSelected: (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(label) }
        if (expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    extraItem?.let {
                        Text(
                            text = it,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onExtraItemSelected?.invoke()
                                    expanded = false
                                }
                                .padding(12.dp),
                        )
                    }
                    items.forEachIndexed { idx, item ->
                        Text(
                            text = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemSelected(idx)
                                    expanded = false
                                }
                                .padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun RadioToggle(label: String, selected: Boolean, onSelect: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), RoundedCornerShape(4.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text = label, color = textColor, style = MaterialTheme.typography.labelMedium)
    }
}
