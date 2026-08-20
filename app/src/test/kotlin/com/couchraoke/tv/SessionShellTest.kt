package com.couchraoke.tv

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.di.SessionStartOutcome
import com.couchraoke.tv.di.SessionStartResult
import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.StartedTransport
import com.couchraoke.tv.domain.platform.AnnouncementHandle
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import com.couchraoke.tv.domain.session.SessionStartFailure
import com.couchraoke.tv.presentation.join.JOIN_OVERLAY_DISMISS_ACTION_TAG
import com.couchraoke.tv.presentation.join.JOIN_OVERLAY_QR_TAG
import com.couchraoke.tv.presentation.join.JOIN_OVERLAY_ROOT_TAG
import com.couchraoke.tv.presentation.join.START_FAILURE_NOTICE_ACKNOWLEDGE_ACTION_TAG
import com.couchraoke.tv.presentation.join.START_FAILURE_NOTICE_ROOT_TAG
import com.couchraoke.tv.presentation.songlist.SONG_LIST_HEADER_JOIN_ACTION_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.Inet4Address
import java.net.InetAddress

/**
 * T067: the composition root. Every one of these assertions is about *reachability* -- whether a
 * composable the slice already built and unit-tested is actually put on screen by the running
 * app.
 *
 * This is the gap spec.md Observation 23 records. [com.couchraoke.tv.presentation.join.JoinOverlay]
 * was built (T041), unit-tested and previewed, yet was composed nowhere outside its own
 * `@Preview`s, so the QR never reached the screen. No existing gate could see it: the unit gates
 * invoke composables directly, the screenshot gate renders previews, and the loopback gate drives
 * the transport with no UI at all. T065 would have failed at the last step of the slice, on a
 * physical TV, for want of the four lines these tests pin.
 */
@RunWith(RobolectricTestRunner::class)
class SessionShellTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test(timeout = 30_000)
    fun theJoinActionPutsTheQrOnScreenOnceTheSessionHasStarted() {
        composeRule.setContent { SessionShell(outcome = startedOutcome()) }

        composeRule.onNodeWithTag(SONG_LIST_HEADER_JOIN_ACTION_TAG).activate()

        composeRule.onNodeWithTag(JOIN_OVERLAY_ROOT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(JOIN_OVERLAY_QR_TAG).assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun theOverlayStaysClosedUntilTheJoinActionIsTaken() {
        composeRule.setContent { SessionShell(outcome = startedOutcome()) }

        composeRule.onNodeWithTag(JOIN_OVERLAY_ROOT_TAG).assertDoesNotExist()
    }

    @Test(timeout = 30_000)
    fun dismissingTheOverlayReturnsToSongSelection() {
        composeRule.setContent { SessionShell(outcome = startedOutcome()) }
        composeRule.onNodeWithTag(SONG_LIST_HEADER_JOIN_ACTION_TAG).activate()

        composeRule.onNodeWithTag(JOIN_OVERLAY_DISMISS_ACTION_TAG).activate()

        composeRule.onNodeWithTag(JOIN_OVERLAY_ROOT_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(SONG_LIST_HEADER_JOIN_ACTION_TAG).assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun aFailedStartRaisesTheNoticeWithNoUserAction() {
        composeRule.setContent {
            SessionShell(outcome = SessionStartOutcome.Failed(SessionStartFailure.NoUsableAddress))
        }

        composeRule.onNodeWithTag(START_FAILURE_NOTICE_ROOT_TAG).assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun acknowledgingTheNoticeReturnsToSongSelection() {
        composeRule.setContent {
            SessionShell(outcome = SessionStartOutcome.Failed(SessionStartFailure.BindFailed))
        }

        composeRule.onNodeWithTag(START_FAILURE_NOTICE_ACKNOWLEDGE_ACTION_TAG).activate()

        composeRule.onNodeWithTag(START_FAILURE_NOTICE_ROOT_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(SONG_LIST_HEADER_JOIN_ACTION_TAG).assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun theJoinActionOpensNoOverlayWhenTheSessionNeverStarted() {
        composeRule.setContent {
            SessionShell(outcome = SessionStartOutcome.Failed(SessionStartFailure.AnnouncementFailed))
        }
        composeRule.onNodeWithTag(START_FAILURE_NOTICE_ACKNOWLEDGE_ACTION_TAG).activate()

        composeRule.onNodeWithTag(SONG_LIST_HEADER_JOIN_ACTION_TAG).activate()

        // There is no coordinator to build a JoinViewModel from, so pressing Join must be inert
        // rather than crash: JoinOverlay is only reachable from SessionStartOutcome.Started.
        composeRule.onNodeWithTag(JOIN_OVERLAY_ROOT_TAG).assertDoesNotExist()
    }

    @Test(timeout = 30_000)
    fun nothingIsOverlaidWhileTheStartIsStillInFlight() {
        composeRule.setContent { SessionShell(outcome = null) }

        composeRule.onNodeWithTag(START_FAILURE_NOTICE_ROOT_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(SONG_LIST_HEADER_JOIN_ACTION_TAG).assertIsDisplayed()
    }
}

/**
 * Invokes a control the way a TV viewer does. `performClick` injects a synthetic *touch* gesture,
 * which a `tv-material` `Button` ignores under this module's `notouch` qualifier -- the tree
 * renders, the tap lands on nothing, and the assertion fails for a reason that has nothing to do
 * with the wiring under test. Driving the node's own `OnClick` semantics action exercises exactly
 * the lambda a D-pad centre press would.
 */
private fun SemanticsNodeInteraction.activate() = performSemanticsAction(SemanticsActions.OnClick)

private fun startedOutcome(): SessionStartOutcome.Started = SessionStartOutcome.Started(
    SessionStartResult(
        coordinator = SessionComponent(
            transport = FakeControlTransport,
            announcer = FakeSessionAnnouncer,
            addressProvider = LocalAddressProvider { null },
            multicastLease = FakeMulticastLease,
            joinCodeGenerator = JoinCodeGenerator(),
            clock = { 0L },
        ).createCoordinator(),
        address = InetAddress.getByName("192.168.1.42") as Inet4Address,
        boundPort = 51900,
        announcement = FakeAnnouncementHandle,
    ),
)

private object FakeControlTransport : ControlTransport {
    override suspend fun start(port: Int, handler: ControlConnectionHandler): StartedTransport =
        object : StartedTransport {
            override val boundPort: Int = port
        }

    override suspend fun stop() = Unit
}

private object FakeSessionAnnouncer : SessionAnnouncer {
    override suspend fun publish(
        address: Inet4Address,
        instanceName: String,
        port: Int,
        joinCode: String,
        protocolVersion: Int,
    ): AnnouncementHandle = FakeAnnouncementHandle

    override suspend fun withdraw(handle: AnnouncementHandle) = Unit
}

private object FakeAnnouncementHandle : AnnouncementHandle {
    override val registeredInstanceName: String = "Couchraoke TV"
}

private object FakeMulticastLease : MulticastLease {
    override fun acquire() = Unit
    override fun release() = Unit
}
