package com.couchraoke.tv.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class MdnsAdvertiser(private val context: Context) {

    private var jmDNS: JmDNS? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(joinCode: String, wsPort: Int) {
        val normalized = normalize(joinCode)
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiManager.createMulticastLock("jmdns_lock").also {
            it.setReferenceCounted(true)
            it.acquire()
        }
        jmDNS = JmDNS.create()
        val serviceInfo = ServiceInfo.create(
            "_karaoke._tcp.local.",
            instanceName(normalized),
            wsPort,
            0, 0,
            txtRecords(normalized),
        )
        jmDNS?.registerService(serviceInfo)
        Log.i(TAG, "mDNS advertised: ${instanceName(normalized)} on port $wsPort")
    }

    fun stop() {
        jmDNS?.unregisterAllServices()
        jmDNS?.close()
        jmDNS = null
        multicastLock?.release()
        multicastLock = null
    }

    companion object {
        private const val TAG = "MdnsAdvertiser"

        /** Instance name: KaraokeTV-<last4 chars of normalized code> */
        internal fun instanceName(normalizedCode: String): String =
            "KaraokeTV-${normalizedCode.takeLast(4)}"

        /** TXT record map with required fields */
        internal fun txtRecords(normalizedCode: String): Map<String, String> =
            mapOf("code" to normalizedCode, "v" to "1")

        internal fun normalize(code: String): String =
            code.uppercase().replace(" ", "").replace("-", "")
    }
}
