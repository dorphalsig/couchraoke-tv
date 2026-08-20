package com.couchraoke.tv.presentation.qr

import com.couchraoke.tv.domain.session.model.JoinCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

/**
 * T031: pins [QrPayloadEncoder.encode]'s exact payload shape (FR-007) -- `ws://<ip>:<port>/?token=<CODE>`
 * with no discovery-service identifier such as the `_karaoke._tcp` service type or the
 * `KaraokeTV-<noun>` mDNS instance name (contracts/domain-api.md).
 */
class QrPayloadEncoderTest {

    @Test(timeout = 30_000)
    fun encodeProducesExactlyTheControlEndpointUrlWithTheJoinCodeAsTheTokenQueryParameter() {
        val address = InetAddress.getByName("192.168.1.42") as Inet4Address
        val joinCode = JoinCode(adjective = "swift", noun = "panda")

        val payload = QrPayloadEncoder.encode(address, 51900, joinCode)

        assertEquals(
            "FR-007: payload must be exactly ws://<ip>:<port>/?token=<CODE>",
            "ws://192.168.1.42:51900/?token=SWIFT-PANDA",
            payload,
        )
    }

    @Test(timeout = 30_000)
    fun encodeReflectsADifferentAddressPortAndJoinCodeExactly() {
        // Guards against the first assertion passing by coincidence on one fixed input --
        // every field of a different payload must change to match, not just the join code.
        val address = InetAddress.getByName("10.20.30.40") as Inet4Address
        val joinCode = JoinCode(adjective = "brave", noun = "otter")

        val payload = QrPayloadEncoder.encode(address, 8080, joinCode)

        assertEquals("ws://10.20.30.40:8080/?token=BRAVE-OTTER", payload)
    }

    @Test(timeout = 30_000)
    fun encodeCarriesNoDiscoveryServiceIdentifier() {
        val address = InetAddress.getByName("10.0.0.7") as Inet4Address
        val joinCode = JoinCode(adjective = "quiet", noun = "falcon")

        val payload = QrPayloadEncoder.encode(address, 9090, joinCode)

        assertFalse(
            "FR-007: the payload must not encode the mDNS service type",
            payload.contains("_karaoke"),
        )
        assertFalse(
            "FR-007: the payload must not encode the mDNS instance name",
            payload.contains("KaraokeTV"),
        )
    }
}
