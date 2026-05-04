package com.couchraoke.tv.data.network

import com.couchraoke.tv.fixtures.SoloSingFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorNetworkControllerTest {
    @Test(timeout = 30_000)
    fun exposesStaticTokenGatedJoinEndpointForQrPayload() = runBlocking {
        val controller = KtorNetworkController(
            sessionId = SoloSingFixtures.SessionId,
            sessionToken = SoloSingFixtures.SessionToken,
            joinCode = SoloSingFixtures.JoinCode,
            hostAddress = SoloSingFixtures.TvIpAddress,
        )

        controller.start(
            udpPort = SoloSingFixtures.UdpPort,
            wsPort = SoloSingFixtures.WebSocketPort,
        )

        assertEquals(SoloSingFixtures.joinQrPayload(), controller.joinEndpointUrl)
        assertTrue(controller.acceptsHelloToken(SoloSingFixtures.SessionToken))
        assertFalse(controller.acceptsHelloToken("wrong-token"))
        assertEquals(SoloSingFixtures.SessionId, controller.sessionState.value.sessionId)
        assertEquals(SoloSingFixtures.SessionToken, controller.sessionState.value.sessionToken)
        assertFalse(controller.sessionState.value.isLocked)
    }

    @Test(timeout = 30_000)
    fun advertisesMdnsServiceWithEndpointPortAndJoinCodeTxtRecord() = runBlocking {
        val controller = KtorNetworkController(
            sessionId = SoloSingFixtures.SessionId,
            sessionToken = SoloSingFixtures.SessionToken,
            joinCode = SoloSingFixtures.JoinCode,
            hostAddress = SoloSingFixtures.TvIpAddress,
        )

        controller.start(
            udpPort = SoloSingFixtures.UdpPort,
            wsPort = SoloSingFixtures.WebSocketPort,
        )

        val advertisement = controller.mdnsAdvertisement.value
        assertEquals("_karaoke._tcp", advertisement?.serviceType)
        assertEquals(SoloSingFixtures.WebSocketPort, advertisement?.port)
        assertEquals(SoloSingFixtures.JoinCode, advertisement?.txtRecords?.get("code"))
        assertEquals("1", advertisement?.txtRecords?.get("v"))

        controller.stop()

        assertEquals(null, controller.mdnsAdvertisement.value)
    }

    @Test(timeout = 30_000)
    fun acceptsHelloAndRejectsProtocolMismatchInvalidTokenAndLockedSession() = runBlocking {
        val controller = KtorNetworkController(
            sessionId = SoloSingFixtures.SessionId,
            sessionToken = SoloSingFixtures.SessionToken,
            joinCode = SoloSingFixtures.JoinCode,
            hostAddress = SoloSingFixtures.TvIpAddress,
        )
        controller.start(
            udpPort = SoloSingFixtures.UdpPort,
            wsPort = SoloSingFixtures.WebSocketPort,
        )

        val accepted = controller.handleHello(
            token = SoloSingFixtures.SessionToken,
            protocolVersion = 1,
            clientId = SoloSingFixtures.PhoneClientId,
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        )
        val protocolMismatch = controller.handleHello(
            token = SoloSingFixtures.SessionToken,
            protocolVersion = 2,
            clientId = "protocol-mismatch-phone",
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        )
        val invalidToken = controller.handleHello(
            token = "wrong-token",
            protocolVersion = 1,
            clientId = "invalid-token-phone",
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        )

        controller.lockSession()
        val lockedNewJoin = controller.handleHello(
            token = SoloSingFixtures.SessionToken,
            protocolVersion = 1,
            clientId = "new-phone",
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        )

        assertTrue(accepted is HelloResult.Accepted)
        assertEquals(SoloSingFixtures.PhoneClientId, (accepted as HelloResult.Accepted).phone.clientId)
        assertEquals(HelloError.ProtocolMismatch, (protocolMismatch as HelloResult.Rejected).error)
        assertEquals(HelloError.InvalidToken, (invalidToken as HelloResult.Rejected).error)
        assertEquals(HelloError.SessionLocked, (lockedNewJoin as HelloResult.Rejected).error)
        assertTrue(controller.sessionState.value.isLocked)
    }

    @Test(timeout = 30_000)
    fun reconnectReplacesConnectionIdAndRosterEntryDuringLockedSession() = runBlocking {
        val controller = KtorNetworkController(sessionToken = SoloSingFixtures.SessionToken)

        val firstJoin = controller.handleHello(
            token = SoloSingFixtures.SessionToken,
            protocolVersion = 1,
            clientId = SoloSingFixtures.PhoneClientId,
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        ) as HelloResult.Accepted

        controller.lockSession()

        val reconnect = controller.handleHello(
            token = SoloSingFixtures.SessionToken,
            protocolVersion = 1,
            clientId = SoloSingFixtures.PhoneClientId,
            deviceName = "Reconnected Phone",
            httpPort = SoloSingFixtures.PhoneHttpPort + 1,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        ) as HelloResult.Accepted

        assertNotEquals(firstJoin.phone.connectionId, reconnect.phone.connectionId)
        assertEquals("Reconnected Phone", reconnect.phone.deviceName)
        assertEquals(SoloSingFixtures.PhoneHttpPort + 1, reconnect.phone.httpPort)
        assertEquals(listOf(reconnect.phone), controller.connectedPhones.value)
    }

    @Test(timeout = 30_000)
    fun mapsManifestJsonIntoSongEntriesFromPhoneHttpSource() = runBlocking {
        val phone = ConnectedPhone(
            clientId = SoloSingFixtures.PhoneClientId,
            connectionId = SoloSingFixtures.PhoneConnectionId,
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = SoloSingFixtures.PhoneHttpPort,
            ipAddress = SoloSingFixtures.PhoneIpAddress,
        )
        val controller = KtorNetworkController(
            manifestResponses = mapOf(phone.clientId to SoloSingFixtures.manifestJson()),
        )

        val songs = controller.fetchManifest(phone).getOrThrow()
        val refreshedSongs = controller.fetchManifest(phone).getOrThrow()

        assertEquals(1, songs.size)
        assertEquals(2, controller.fetchedManifestClientIds.size)
        assertEquals(listOf(phone.clientId, phone.clientId), controller.fetchedManifestClientIds)
        assertEquals(songs, refreshedSongs)
        assertEquals(SoloSingFixtures.RelativeTxtPath, songs.single().relativeTxtPath)
        assertEquals(SoloSingFixtures.SongTitle, songs.single().title)
        assertEquals(SoloSingFixtures.SongArtist, songs.single().artist)
        assertEquals(SoloSingFixtures.assetUrl("/songs/${SoloSingFixtures.RelativeTxtPath}"), songs.single().txtUrl)
        assertEquals(SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp3"), songs.single().audioUrl)
        assertEquals(SoloSingFixtures.assetUrl("/songs/solo/demo-song.mp4"), songs.single().videoUrl)
    }

    @Test(timeout = 30_000)
    fun fetchesTxtBytesFromStreamedPhoneUrlWithoutPersistence() = runBlocking {
        val txtUrl = SoloSingFixtures.assetUrl("/songs/${SoloSingFixtures.RelativeTxtPath}")
        val txtBytes = """
            #TITLE:${SoloSingFixtures.SongTitle}
            #ARTIST:${SoloSingFixtures.SongArtist}
        """.trimIndent().encodeToByteArray()
        val controller = KtorNetworkController(txtResponses = mapOf(txtUrl to txtBytes))

        val result = controller.fetchTxt(txtUrl).getOrThrow()

        assertArrayEquals(txtBytes, result)
        assertEquals(listOf(txtUrl), controller.fetchedTxtUrls)
    }
}
