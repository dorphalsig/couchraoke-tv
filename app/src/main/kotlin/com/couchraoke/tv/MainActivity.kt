package com.couchraoke.tv

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.couchraoke.tv.data.control.KtorControlTransport
import com.couchraoke.tv.data.discovery.JmdnsSessionAnnouncer
import com.couchraoke.tv.data.platform.ConnectivityLocalAddressProvider
import com.couchraoke.tv.data.platform.WifiMulticastLease
import com.couchraoke.tv.di.SessionComponent
import com.couchraoke.tv.di.SessionStartOutcome
import com.couchraoke.tv.domain.control.ControlMessageCodec
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
    private var startOutcome: SessionStartOutcome? = null
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

        // T060 (FR-028, SC-008) now reports *which* of the three failures occurred as a
        // SessionStartOutcome.Failed(SessionStartFailure). Composing that into a visible
        // failure modal needs a JoinViewModel built from a live SessionCoordinator and
        // ControlEndpoint, neither of which exists when startSession itself is what failed —
        // wiring that composition root is out of this task's scope (no task in tasks.md owns
        // it yet), so a failed start is still not surfaced in the UI here. It is, however, no
        // longer silently swallowed: the specific SessionStartFailure is retained in
        // startOutcome for whatever composition-root work wires it up next.
        startJob = sessionScope.launch { startOutcome = sessionComponent.startSession(CONTROL_PORT) }

        setContent {
            CouchraokeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    // T040 builds JoinViewModel and T041 the overlay, both later in this
                    // slice. The Join action stays inert until that composition root exists.
                    SongListScreen(onJoinClick = {})
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
