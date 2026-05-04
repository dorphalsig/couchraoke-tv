@file:NoCoverageGenerated

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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.couchraoke.quality.NoCoverageGenerated
import com.couchraoke.tv.data.library.ManifestLibraryManager
import com.couchraoke.tv.data.network.ConnectedPhone
import com.couchraoke.tv.data.network.KtorNetworkController
import com.couchraoke.tv.domain.playback.DefaultPlaybackCoordinator
import com.couchraoke.tv.domain.usdx.internal.DefaultUsdxParser
import com.couchraoke.tv.presentation.navigation.AppNavHost
import com.couchraoke.tv.ui.theme.CouchraokeTheme

@NoCoverageGenerated
class MainActivity : ComponentActivity() {
    private val requestLocalNetworkPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                ensureMulticastLock()
            }
        }

    private var multicastLock: WifiManager.MulticastLock? = null

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CouchraokeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(containerColor = Color.Black),
                ) {
                    val networkController = KtorNetworkController(
                        sessionId = DemoSoloSingSeed.SessionId,
                        sessionToken = DemoSoloSingSeed.SessionToken,
                        joinCode = DemoSoloSingSeed.JoinCode,
                        hostAddress = DemoSoloSingSeed.TvIpAddress,
                        initialWsPort = DemoSoloSingSeed.WebSocketPort,
                        initialConnectedPhones = listOf(
                            ConnectedPhone(
                                clientId = DemoSoloSingSeed.PhoneClientId,
                                connectionId = DemoSoloSingSeed.PhoneConnectionId,
                                deviceName = DemoSoloSingSeed.PhoneDeviceName,
                                httpPort = DemoSoloSingSeed.PhoneHttpPort,
                                ipAddress = DemoSoloSingSeed.PhoneIpAddress,
                            ),
                        ),
                        manifestResponses = mapOf(
                            DemoSoloSingSeed.PhoneClientId to DemoSoloSingSeed.manifestJson(),
                        ),
                        txtResponses = mapOf(
                            DemoSoloSingSeed.TxtUrl to DemoSoloSingSeed.StaticSoloChart.encodeToByteArray(),
                        ),
                    )
                    val libraryManager = ManifestLibraryManager(networkController)
                    val playbackCoordinator = DefaultPlaybackCoordinator(
                        libraryManager = libraryManager,
                        networkController = networkController,
                        usdxParser = DefaultUsdxParser(),
                        udpPort = DemoSoloSingSeed.UdpPort,
                        sessionId = DemoSoloSingSeed.SessionId,
                    )
                    AppNavHost(
                        libraryManager = libraryManager,
                        networkController = networkController,
                        playbackCoordinator = playbackCoordinator,
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
        releaseMulticastLock()
        super.onStop()
    }

    private fun preparePeerNetworking() {
        if (requiresLocalNetworkPermission() && !hasLocalNetworkPermission()) {
            requestLocalNetworkPermission.launch(ACCESS_LOCAL_NETWORK_PERMISSION)
            return
        }

        ensureMulticastLock()
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
    }
}
