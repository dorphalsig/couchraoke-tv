package com.couchraoke.tv.data.network

import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

interface MdnsAdvertiser {
    fun advertise(advertisement: MdnsAdvertisement)
    fun stop(advertisement: MdnsAdvertisement)
}

class JmDnsMdnsAdvertiser(private val hostAddress: String) : MdnsAdvertiser {
    private var jmDns: JmDNS? = null
    private var serviceInfo: ServiceInfo? = null

    override fun advertise(advertisement: MdnsAdvertisement) {
        val address = InetAddress.getByName(hostAddress)
        jmDns = JmDNS.create(address)
        serviceInfo = ServiceInfo.create(
            advertisement.serviceType,
            "KaraokeTV-${advertisement.txtRecords.getValue("code").takeLast(4)}",
            advertisement.port,
            0,
            0,
            advertisement.txtRecords,
        )
        jmDns?.registerService(serviceInfo)
    }

    override fun stop(advertisement: MdnsAdvertisement) {
        serviceInfo?.let { jmDns?.unregisterService(it) }
        jmDns?.close()
        serviceInfo = null
        jmDns = null
    }
}

data class MdnsAdvertisement(
    val serviceType: String,
    val port: Int,
    val txtRecords: Map<String, String>,
)
