package com.couchraoke.tv

import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.data.library.ManifestLibraryManager
import com.couchraoke.tv.data.network.KtorNetworkController
import com.couchraoke.tv.domain.playback.DefaultPlaybackCoordinator
import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import com.couchraoke.tv.presentation.navigation.AppNavHost
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

@NoCoverageGenerated
class MainActivity : ComponentActivity() {
    private val requestLocalNetworkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startPeerNetworking()
        }

    private var multicastLock: WifiManager.MulticastLock? = null
    private lateinit var networkController: KtorNetworkController
    private lateinit var libraryManager: ManifestLibraryManager
    private lateinit var playbackCoordinator: DefaultPlaybackCoordinator
    private var libraryRefreshJob: Job? = null

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionToken = newSessionToken()
        val sessionId = UUID.randomUUID().toString()
        val hostAddress = checkNotNull(findLanIpv4Address()) {
            "Unable to determine a LAN IPv4 address for phone joining."
        }
        networkController = KtorNetworkController(
            sessionId = sessionId,
            sessionToken = sessionToken,
            joinCode = sessionToken,
            hostAddress = hostAddress,
            initialWsPort = WEB_SOCKET_PORT,
        )
        libraryManager = ManifestLibraryManager(networkController)
        playbackCoordinator = DefaultPlaybackCoordinator(
            libraryManager = libraryManager,
            networkController = networkController,
            usdxParser = DefaultUsdxParser(),
            udpPort = UDP_PORT,
            sessionId = sessionId,
        )
        setContent {
            CouchraokeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(containerColor = Color.Black),
                ) {
                    AppNavHost(
                        libraryManager = libraryManager,
                        networkController = networkController,
                        sessionState = networkController.sessionState,
                        joinEndpointUrl = networkController.joinEndpointUrl,
                        playbackCoordinator = playbackCoordinator,
                        onExitApp = ::finish,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        preparePeerNetworking()
    }

    override fun onStop() {
        libraryRefreshJob?.cancel()
        libraryRefreshJob = null
        lifecycleScope.launch { networkController.stop() }
        releaseMulticastLock()
        super.onStop()
    }

    private fun preparePeerNetworking() {
        if (requiresLocalNetworkPermission() && !hasLocalNetworkPermission()) {
            requestLocalNetworkPermission.launch(ACCESS_LOCAL_NETWORK_PERMISSION)
            return
        }

        startPeerNetworking()
    }

    private fun startPeerNetworking() {
        ensureMulticastLock()
        lifecycleScope.launch {
            networkController.start(udpPort = UDP_PORT, wsPort = WEB_SOCKET_PORT)
            libraryRefreshJob?.cancel()
            libraryRefreshJob = libraryManager.launchConnectedPhoneRefresh(this)
        }
    }

    private fun requiresLocalNetworkPermission(): Boolean = Build.VERSION.SDK_INT >= 36

    private fun hasLocalNetworkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            ACCESS_LOCAL_NETWORK_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureMulticastLock() {
        if (multicastLock?.isHeld == true) {
            return
        }

        val wifiManager = applicationContext.getSystemService(WifiManager::class.java) ?: return
        multicastLock = wifiManager.createMulticastLock(MULTICAST_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.takeIf(WifiManager.MulticastLock::isHeld)?.release()
        multicastLock = null
    }

    @NoCoverageGenerated
    companion object {
        const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val MULTICAST_LOCK_TAG = "jmdns_lock"
        private const val WEB_SOCKET_PORT = 8080
        private const val UDP_PORT = 29170
    }
}

@NoCoverageGenerated
private fun newSessionToken(): String = UUID.randomUUID()
    .toString()
    .filter(Char::isLetterOrDigit)
    .take(8)
    .uppercase()

@NoCoverageGenerated
private fun findLanIpv4Address(): String? = NetworkInterface.getNetworkInterfaces()
    .asSequence()
    .flatMap { networkInterface -> networkInterface.inetAddresses.asSequence() }
    .filterIsInstance<Inet4Address>()
    .firstOrNull { address -> !address.isLoopbackAddress }
    ?.hostAddress
