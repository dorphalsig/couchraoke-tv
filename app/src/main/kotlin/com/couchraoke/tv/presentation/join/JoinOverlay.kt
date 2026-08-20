package com.couchraoke.tv.presentation.join

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.presentation.qr.QrCode
import com.couchraoke.tv.ui.theme.CouchraokeTheme

// Stable Compose test tags for JoinOverlay so T061 (JoinOverlayBoundsTest) can assert bounds by tag.
const val JOIN_OVERLAY_ROOT_TAG = "join_overlay_root"
const val JOIN_OVERLAY_QR_TAG = "join_overlay_qr"
const val JOIN_OVERLAY_CODE_TAG = "join_overlay_code"
const val JOIN_OVERLAY_CONNECTED_COUNT_TAG = "join_overlay_connected_count"
const val JOIN_OVERLAY_DISMISS_ACTION_TAG = "join_overlay_dismiss_action"

/** The QR code's share of the shorter viewport dimension (research.md R9's 30%-55% band). */
private const val QR_SHARE_OF_SHORTER_SIDE = 0.42f
private const val ENTRANCE_DURATION_MS = 220
private const val ENTRANCE_INITIAL_SCALE = 0.92f
private const val SCRIM_ALPHA = 0.72f

/**
 * T041: the join modal over [com.couchraoke.tv.presentation.songlist.SongListScreen] (FR-030).
 * The QR code is the dominant element with the join code directly beneath it and nothing else
 * placed between them or over the code's quiet zone -- a plain vertical [Column] guarantees
 * that ordering and non-overlap by construction, so no measurement is needed to keep the
 * promise. The only motion is a single fade/scale-fade of the whole modal shell on entrance
 * (FR-031); nothing here loops or re-triggers once shown.
 *
 * Deliberately a plain composable rather than a platform `Dialog`: androidx.tv.material3
 * 1.0.1 does not offer a TV-focus-aware dialog, so this overlay is composed directly above
 * [com.couchraoke.tv.presentation.songlist.SongListScreen] in the same composition, keeping it
 * in one semantics tree for [JOIN_OVERLAY_ROOT_TAG] and the tags below.
 *
 * This composable takes plain state and callbacks, not a [JoinViewModel] -- mirroring
 * `SongListScreen(onJoinClick: () -> Unit)` -- so it stays previewable and testable without a
 * live coordinator; wiring it to a real [JoinViewModel] is composition-root work for a later
 * task (T036).
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun JoinOverlay(uiState: JoinUiState, onDismissRequest: () -> Unit, modifier: Modifier = Modifier) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .testTag(JOIN_OVERLAY_ROOT_TAG),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(ENTRANCE_DURATION_MS)) +
                scaleIn(initialScale = ENTRANCE_INITIAL_SCALE, animationSpec = tween(ENTRANCE_DURATION_MS)),
        ) {
            Surface {
                BoxWithConstraints(modifier = Modifier.padding(32.dp)) {
                    val qrSize = min(maxWidth, maxHeight) * QR_SHARE_OF_SHORTER_SIDE
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        QrCode(
                            payload = uiState.qrPayload,
                            modifier = Modifier
                                .size(qrSize)
                                .testTag(JOIN_OVERLAY_QR_TAG),
                        )
                        Text(text = uiState.joinCodeDisplay, modifier = Modifier.testTag(JOIN_OVERLAY_CODE_TAG))
                        Text(
                            text = connectedCountLabel(uiState.connectedCount),
                            modifier = Modifier.testTag(JOIN_OVERLAY_CONNECTED_COUNT_TAG),
                        )
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier.testTag(JOIN_OVERLAY_DISMISS_ACTION_TAG),
                        ) {
                            Text(text = "Close")
                        }
                    }
                }
            }
        }
    }
}

private fun connectedCountLabel(connectedCount: Int): String =
    if (connectedCount == 1) "1 phone connected" else "$connectedCount phones connected"

@Preview(name = "Join Overlay - No connections", widthDp = 960, heightDp = 540)
@Composable
fun JoinOverlayNoConnectionsPreview() {
    CouchraokeTheme {
        JoinOverlay(
            uiState = JoinUiState(
                joinCodeDisplay = "SWIFT-PANDA",
                qrPayload = "ws://192.168.1.42:51900/?token=SWIFT-PANDA",
                connectedCount = 0,
            ),
            onDismissRequest = {},
        )
    }
}

@Preview(name = "Join Overlay - Some connected", widthDp = 960, heightDp = 540)
@Composable
fun JoinOverlaySomeConnectedPreview() {
    CouchraokeTheme {
        JoinOverlay(
            uiState = JoinUiState(
                joinCodeDisplay = "BRAVE-OTTER",
                qrPayload = "ws://192.168.1.42:51900/?token=BRAVE-OTTER",
                connectedCount = 2,
            ),
            onDismissRequest = {},
        )
    }
}
