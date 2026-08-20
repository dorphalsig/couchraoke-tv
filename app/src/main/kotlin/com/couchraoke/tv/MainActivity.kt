package com.couchraoke.tv

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.couchraoke.tv.data.control.KtorControlTransport
import com.couchraoke.tv.data.discovery.JmdnsSessionAnnouncer
import com.couchraoke.tv.data.platform.ConnectivityLocalAddressProvider
import com.couchraoke.tv.data.platform.WifiMulticastLease
import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.di.SessionStartOutcome
import com.couchraoke.tv.di.SessionStartResult
import com.couchraoke.tv.domain.control.ControlMessageCodec
import com.couchraoke.tv.domain.session.SessionStartFailure
import com.couchraoke.tv.presentation.join.ControlEndpoint
import com.couchraoke.tv.presentation.join.JoinOverlay
import com.couchraoke.tv.presentation.join.JoinViewModel
import com.couchraoke.tv.presentation.join.SessionStartFailureNotice
import com.couchraoke.tv.presentation.qr.QrPayloadEncoder
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import com.couchraoke.tv.presentation.songlist.SongListScreen
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val CONTROL_PORT = 8080

class MainActivity : ComponentActivity() {

    private lateinit var sessionComponent: SessionComponent
    private var startOutcome by mutableStateOf<SessionStartOutcome?>(null)
    private var startJob: Job? = null

    /**
     * Session teardown deliberately does not run on `lifecycleScope`: that scope is cancelled
     * as the lifecycle reaches DESTROYED, so a coroutine launched from [onDestroy] would be
     * cancelled at its first suspension point and the multicast lock, the bound transport and
     * the mDNS announcement would all leak — leaving phones discovering a session that no
     * longer exists. This scope outlives the Activity precisely so [SessionComponent.stopSession]
     * can finish. It is never cancelled; it goes idle once teardown completes.
     */
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionComponent = SessionComponent(
            transport = KtorControlTransport(
                ControlMessageCodec(
                    Json {
                        explicitNulls = false
                        ignoreUnknownKeys = false
                    },
                ),
            ),
            announcer = JmdnsSessionAnnouncer(),
            addressProvider = ConnectivityLocalAddressProvider(
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            ),
            multicastLease = WifiMulticastLease(
                getSystemService(Context.WIFI_SERVICE) as WifiManager,
            ),
            joinCodeGenerator = JoinCodeGenerator(),
            clock = System::currentTimeMillis,
        )

        startJob = sessionScope.launch { startOutcome = sessionComponent.startSession(CONTROL_PORT) }

        setContent {
            CouchraokeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    SessionShell(outcome = startOutcome)
                }
            }
        }
    }

    override fun onDestroy() {
        // The multicast lease is session-scoped, not activity-scoped (FR-005) — released
        // here, on session end, rather than in onStop, which T025 already stopped doing.
        // Joining startJob first so a destroy that races an in-flight start still tears
        // down whatever that start acquired, instead of stopping nothing.
        sessionScope.launch {
            startJob?.join()
            (startOutcome as? SessionStartOutcome.Started)?.let { started ->
                sessionComponent.stopSession(started.result)
            }
        }
        super.onDestroy()
    }
}

/**
 * The composition root's session-aware shell (T067). [SongListScreen] is always the base layer;
 * what sits over it is decided by [outcome] alone.
 *
 * A failed start is surfaced here rather than through [JoinViewModel] because a [JoinViewModel]
 * cannot exist on that path: its constructor immediately reads the coordinator's join code and
 * encodes a QR payload, and a start that failed produced neither a [SessionCoordinator] nor a
 * bound port to build a [ControlEndpoint] from. See spec.md Observation 24.
 *
 * `internal` rather than `private` so the composition root itself is reachable from a test.
 * Nothing here was gate-able while it lived inside `setContent`, which is why the join surface
 * could stay unreachable in the running app while every unit, screenshot and loopback gate
 * passed (spec.md Observation 23).
 */
@Composable
internal fun SessionShell(outcome: SessionStartOutcome?) {
    var joinVisible by remember { mutableStateOf(false) }
    // Keyed on the outcome so a start that resolves after first composition raises the notice.
    var failureVisible by remember(outcome) { mutableStateOf(outcome is SessionStartOutcome.Failed) }

    SongListScreen(onJoinClick = { joinVisible = true })

    val failure = (outcome as? SessionStartOutcome.Failed)?.failure
    val started = (outcome as? SessionStartOutcome.Started)?.result
    when {
        failure != null && failureVisible ->
            SessionStartFailureNotice(failure = failure, onAcknowledge = { failureVisible = false })

        started != null && joinVisible ->
            JoinSurface(result = started, onDismissRequest = { joinVisible = false })
    }
}

/**
 * Builds [JoinViewModel] through the activity's [androidx.lifecycle.ViewModelStore] rather than
 * `remember`, so its `viewModelScope` is actually cancelled when the activity goes away instead
 * of outliving it. Keyed on the bound port so a restarted session gets a fresh view model.
 */
@Composable
private fun JoinSurface(result: SessionStartResult, onDismissRequest: () -> Unit) {
    val viewModel: JoinViewModel = viewModel(
        key = "join-${result.boundPort}",
        factory = viewModelFactory {
            initializer {
                JoinViewModel(
                    coordinator = result.coordinator,
                    qrEncoder = QrPayloadEncoder,
                    endpoint = ControlEndpoint(result.address, result.boundPort),
                )
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    JoinOverlay(
        uiState = uiState,
        onDismissRequest = {
            viewModel.onOverlayDismissed()
            onDismissRequest()
        },
    )
}
