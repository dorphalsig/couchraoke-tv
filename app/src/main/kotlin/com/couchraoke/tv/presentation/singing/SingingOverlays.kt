@file:NoCoverageGenerated

package com.couchraoke.tv.presentation.singing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.presentation.common.InterruptionShell
import com.couchraoke.tv.ui.theme.BorderFocus
import com.couchraoke.tv.ui.theme.BorderSubtle
import com.couchraoke.tv.ui.theme.ButtonLabel
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import com.couchraoke.tv.ui.theme.FocusBorderWidth
import com.couchraoke.tv.ui.theme.InterruptionActionRowHeight
import com.couchraoke.tv.ui.theme.PrimaryModalPadding
import com.couchraoke.tv.ui.theme.RadiusLarge
import com.couchraoke.tv.ui.theme.SurfaceLevel1
import com.couchraoke.tv.ui.theme.TV_PREVIEW_HEIGHT_DP
import com.couchraoke.tv.ui.theme.TV_PREVIEW_WIDTH_DP
import com.couchraoke.tv.ui.theme.TextPrimary

@Composable
fun SingingOverlays(
    state: SingingUiState,
    actions: SingingOverlayActions,
    modifier: Modifier = Modifier,
) {
    when (state.activeModal) {
        SingingModal.Pause -> PauseOverlay(
            actions = actions,
            modifier = modifier,
        )
        SingingModal.RestartConfirm -> ConfirmationOverlay(
            title = "CONFIRM",
            body = "Restart song?",
            confirmLabel = "OK",
            onConfirm = actions.onRestartConfirmed,
            onCancel = actions.onCancel,
            modifier = modifier,
        )
        SingingModal.QuitConfirm -> ConfirmationOverlay(
            title = "CONFIRM",
            body = "Quit to Song List?",
            confirmLabel = "OK",
            onConfirm = actions.onQuitConfirmed,
            onCancel = actions.onCancel,
            modifier = modifier,
        )
        null -> Unit
    }
}

@Composable
private fun PauseOverlay(
    actions: SingingOverlayActions,
    modifier: Modifier = Modifier,
) {
    val resumeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { resumeFocusRequester.requestFocus() }
    OverlayShell(
        title = "PAUSED",
        body = "Playback is paused.",
        modifier = modifier.testTag("pause-overlay"),
    ) {
        OverlayButton(
            text = "Resume",
            focused = true,
            tag = "pause-resume",
            focusRequester = resumeFocusRequester,
            onClick = actions.onResume,
        )
        OverlayButton(
            text = "Restart Song",
            focused = false,
            tag = "pause-restart",
            onClick = actions.onRestartRequested,
        )
        OverlayButton(
            text = "Quit to Song List",
            focused = false,
            tag = "pause-quit",
            onClick = actions.onQuitRequested,
        )
    }
}

@Composable
private fun ConfirmationOverlay(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cancelFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocusRequester.requestFocus() }
    OverlayShell(title = title, body = body, modifier = modifier.testTag("confirm-overlay")) {
        OverlayButton(
            text = "Cancel",
            focused = true,
            tag = "confirm-cancel",
            focusRequester = cancelFocusRequester,
            onClick = onCancel,
        )
        OverlayButton(text = confirmLabel, focused = false, tag = "confirm-action", onClick = onConfirm)
    }
}

@Composable
private fun OverlayShell(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: RowScopeActions,
) {
    InterruptionShell(
        title = title,
        bodyLines = listOf(body),
        modifier = modifier,
        actions = { actions() },
    )
}

private typealias RowScopeActions = @Composable () -> Unit

data class SingingOverlayActions(
    val onResume: () -> Unit = {},
    val onRestartRequested: () -> Unit = {},
    val onRestartConfirmed: () -> Unit = {},
    val onQuitRequested: () -> Unit = {},
    val onQuitConfirmed: () -> Unit = {},
    val onCancel: () -> Unit = {},
)

@Composable
private fun OverlayButton(
    text: String,
    focused: Boolean,
    tag: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    val focusModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    OutlinedButton(
        onClick = onClick,
        modifier = focusModifier
            .height(InterruptionActionRowHeight)
            .clip(RoundedCornerShape(RadiusLarge))
            .background(SurfaceLevel1)
            .border(
                width = FocusBorderWidth,
                color = if (focused) BorderFocus else BorderSubtle,
                shape = RoundedCornerShape(RadiusLarge),
            )
            .focusProperties { canFocus = true }
            .focusable()
            .padding(horizontal = PrimaryModalPadding)
            .testTag(tag),
    ) {
        Text(text = text, style = ButtonLabel, color = TextPrimary)
    }
}

@Preview(name = "Singing Pause Overlay", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun SingingPauseOverlayPreview() {
    CouchraokeTheme {
        SingingOverlays(SingingUiState(activeModal = SingingModal.Pause), SingingOverlayActions())
    }
}

@Preview(name = "Singing Restart Confirm", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun SingingRestartConfirmPreview() {
    CouchraokeTheme {
        SingingOverlays(SingingUiState(activeModal = SingingModal.RestartConfirm), SingingOverlayActions())
    }
}

@Preview(name = "Singing Quit Confirm", widthDp = TV_PREVIEW_WIDTH_DP, heightDp = TV_PREVIEW_HEIGHT_DP)
@Composable
fun SingingQuitConfirmPreview() {
    CouchraokeTheme {
        SingingOverlays(SingingUiState(activeModal = SingingModal.QuitConfirm), SingingOverlayActions())
    }
}
