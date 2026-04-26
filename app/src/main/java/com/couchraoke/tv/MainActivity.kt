package com.couchraoke.tv

import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.couchraoke.tv.ui.theme.CouchraokeTheme

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
                    shape = RectangleShape
                ) {
                    Greeting("Android")
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

    private companion object {
        const val ACCESS_LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val MULTICAST_LOCK_TAG = "jmdns_lock"
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(name = "Home", widthDp = 1920, heightDp = 1080)
@Composable
fun GreetingPreview() {
    CouchraokeTheme {
        Greeting("Android")
    }
}
