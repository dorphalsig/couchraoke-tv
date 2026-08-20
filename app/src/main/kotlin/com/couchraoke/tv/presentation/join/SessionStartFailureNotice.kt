package com.couchraoke.tv.presentation.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.domain.session.SessionStartFailure
import com.couchraoke.tv.ui.theme.CouchraokeTheme

// Stable Compose test tags, mirroring JoinOverlay's tags (JoinOverlay.kt).
const val START_FAILURE_NOTICE_ROOT_TAG = "start_failure_notice_root"
const val START_FAILURE_NOTICE_MESSAGE_TAG = "start_failure_notice_message"
const val START_FAILURE_NOTICE_ACKNOWLEDGE_ACTION_TAG = "start_failure_notice_acknowledge_action"

private const val SCRIM_ALPHA = 0.72f

/**
 * T060: the FR-028 blocking notice, a modal over
 * [com.couchraoke.tv.presentation.songlist.SongListScreen] shown whenever
 * [JoinUiState.startFailure] is non-null (SC-008). Dismissible with the single
 * [onAcknowledge] action FR-028 requires; there is nothing else to interact with, since the
 * only recovery this slice defines is returning to song selection -- there is no retry, and no
 * [com.couchraoke.tv.domain.session.GamePhase] transition is ever attempted from here.
 *
 * A plain composable rather than a platform `Dialog`, for the same reason as [JoinOverlay]:
 * androidx.tv.material3 1.0.1 has no TV-focus-aware dialog.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SessionStartFailureNotice(failure: SessionStartFailure, onAcknowledge: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA))
            .testTag(START_FAILURE_NOTICE_ROOT_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Surface {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = messageFor(failure), modifier = Modifier.testTag(START_FAILURE_NOTICE_MESSAGE_TAG))
                Button(
                    onClick = onAcknowledge,
                    modifier = Modifier.testTag(START_FAILURE_NOTICE_ACKNOWLEDGE_ACTION_TAG),
                ) {
                    Text(text = "OK")
                }
            }
        }
    }
}

/** FR-028: every case states plainly that gameplay is unavailable, then names the cause. */
private fun messageFor(failure: SessionStartFailure): String = when (failure) {
    SessionStartFailure.NoUsableAddress ->
        "Gameplay is unavailable. This TV could not find a usable network connection."
    SessionStartFailure.BindFailed ->
        "Gameplay is unavailable. This TV could not start listening for phones to join."
    SessionStartFailure.AnnouncementFailed ->
        "Gameplay is unavailable. This TV could not announce itself on the network."
}

@Preview(name = "Start Failure - No usable address", widthDp = 960, heightDp = 540)
@Composable
fun SessionStartFailureNoticeNoUsableAddressPreview() {
    CouchraokeTheme {
        SessionStartFailureNotice(failure = SessionStartFailure.NoUsableAddress, onAcknowledge = {})
    }
}

@Preview(name = "Start Failure - Bind failed", widthDp = 960, heightDp = 540)
@Composable
fun SessionStartFailureNoticeBindFailedPreview() {
    CouchraokeTheme {
        SessionStartFailureNotice(failure = SessionStartFailure.BindFailed, onAcknowledge = {})
    }
}

@Preview(name = "Start Failure - Announcement failed", widthDp = 960, heightDp = 540)
@Composable
fun SessionStartFailureNoticeAnnouncementFailedPreview() {
    CouchraokeTheme {
        SessionStartFailureNotice(failure = SessionStartFailure.AnnouncementFailed, onAcknowledge = {})
    }
}
