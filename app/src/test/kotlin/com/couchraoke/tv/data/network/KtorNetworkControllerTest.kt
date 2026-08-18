package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.model.PlayerId
import com.couchraoke.tv.domain.scoring.model.Difficulty
import com.couchraoke.tv.fixtures.SoloSingFixtures
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import io.ktor.client.engine.okhttp.OkHttp as ClientOkHttp
import io.ktor.server.cio.CIO as ServerCIO

class KtorNetworkControllerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test(timeout = 30_000)
    fun exposesFullTokenGatedJoinEndpointForQrPayload() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)

        controller.start(
            udpPort = SoloSingFixtures.UdpPort,
            wsPort = wsPort,
        )

        assertEquals(joinQrPayload(wsPort), controller.joinEndpointUrl)
        assertEquals(SoloSingFixtures.SessionId, controller.sessionState.value.sessionId)
        assertEquals(SoloSingFixtures.SessionToken, controller.sessionState.value.sessionToken)
        assertFalse(controller.sessionState.value.isLocked)

        controller.stop()
    }

    @Test(timeout = 30_000)
    fun advertisesMdnsServiceWithEndpointPortAndJoinCodeTxtRecord() = runBlocking {
        val wsPort = freePort()
        val mdns = RecordingMdnsAdvertiser()
        val controller = newController(wsPort = wsPort, mdnsAdvertiser = mdns)

        controller.start(
            udpPort = SoloSingFixtures.UdpPort,
            wsPort = wsPort,
        )

        val advertisement = controller.mdnsAdvertisement.value
        assertEquals("_karaoke._tcp.local.", advertisement?.serviceType)
        assertEquals(wsPort, advertisement?.port)
        assertEquals(SoloSingFixtures.JoinCode, advertisement?.txtRecords?.get("code"))
        assertEquals("1", advertisement?.txtRecords?.get("v"))
        assertEquals(advertisement, mdns.advertised.single())

        controller.stop()

        assertEquals(null, controller.mdnsAdvertisement.value)
        assertEquals(listOf(advertisement), mdns.stopped)
    }

    @Test(timeout = 30_000)
    fun websocketHelloAcceptsValidTokenAndRejectsProtocolInvalidTokenAndLockedSession() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)
        controller.start(udpPort = SoloSingFixtures.UdpPort, wsPort = wsPort)

        val accepted = hello(
            port = wsPort,
            token = SoloSingFixtures.SessionToken,
            clientId = SoloSingFixtures.PhoneClientId,
        )
        val protocolMismatch = hello(
            port = wsPort,
            token = SoloSingFixtures.SessionToken,
            protocolVersion = 2,
            clientId = "protocol-mismatch",
        )
        val invalidToken = hello(
            port = wsPort,
            token = "wrong-token",
            clientId = "invalid-token",
        )
        controller.broadcastPlaybackState(playbackStateMessage())
        val lockedNewJoin = hello(
            port = wsPort,
            token = SoloSingFixtures.SessionToken,
            clientId = "new-phone",
        )

        assertSessionStatePayload(accepted, connectionIdExpected = true, inSong = false)
        assertErrorPayload(protocolMismatch, code = "protocol_mismatch", message = "Protocol version mismatch.")
        assertErrorPayload(invalidToken, code = "invalid_token", message = "Invalid session token.")
        val missingAppVersion = hello(
            port = wsPort,
            token = SoloSingFixtures.SessionToken,
            clientId = "missing-version",
            includeAppVersion = false,
        )
        val missingToken = hello(
            port = wsPort,
            token = null,
            clientId = "missing-token",
        )

        assertErrorPayload(lockedNewJoin, code = "session_locked", message = "Session is locked.")
        assertErrorPayload(missingAppVersion, code = "protocol_mismatch", message = "Protocol version mismatch.")
        assertErrorPayload(missingToken, code = "invalid_token", message = "Invalid session token.")

        controller.stop()
    }

    @Test(timeout = 30_000)
    fun stoppedPlaybackStateReopensSessionForNewPhoneJoins() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)
        controller.start(udpPort = SoloSingFixtures.UdpPort, wsPort = wsPort)

        controller.broadcastPlaybackState(playbackStateMessage())
        assertTrue(controller.sessionState.value.isLocked)
        assertErrorPayload(
            hello(port = wsPort, token = SoloSingFixtures.SessionToken, clientId = "locked-new-phone"),
            code = "session_locked",
            message = "Session is locked.",
        )

        controller.broadcastPlaybackState(
            playbackStateMessage(state = PlaybackNetworkState.Stopped, countdownRemainingMs = null),
        )

        assertFalse(controller.sessionState.value.isLocked)
        assertSessionStatePayload(
            hello(port = wsPort, token = SoloSingFixtures.SessionToken, clientId = "reopened-new-phone"),
            connectionIdExpected = true,
            inSong = false,
        )

        controller.stop()
    }

    @Test(timeout = 30_000)
    fun eleventhDistinctPhoneIsRejectedButExistingRosterReconnectIsAdmittedWhenLocked() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)
        controller.start(udpPort = SoloSingFixtures.UdpPort, wsPort = wsPort)
        val clients = mutableListOf<HttpClient>()
        val holders = mutableListOf<kotlinx.coroutines.Job>()

        try {
            repeat(10) { index ->
                val client = HttpClient(ClientOkHttp) { install(WebSockets) }
                val ready = CompletableDeferred<Unit>()
                clients += client
                holders += launch {
                    client.webSocket(
                        host = SoloSingFixtures.LoopbackHost,
                        port = wsPort,
                        path = "/",
                        request = { parameter("token", SoloSingFixtures.SessionToken) },
                    ) {
                        send(Frame.Text(helloJson(clientId = "phone-%03d".format(index))))
                        assertSessionStatePayload(incoming.receiveJson(), connectionIdExpected = true, inSong = false)
                        ready.complete(Unit)
                        awaitCancellation()
                    }
                }
                withTimeout(5_000L) { ready.await() }
            }
            assertEquals(10, controller.connectedPhones.value.size)
            controller.broadcastPlaybackState(playbackStateMessage())

            val sessionFull = hello(
                port = wsPort,
                token = SoloSingFixtures.SessionToken,
                clientId = "phone-999",
            )
            val reconnect = hello(
                port = wsPort,
                token = SoloSingFixtures.SessionToken,
                clientId = "phone-000",
            )

            assertErrorPayload(sessionFull, code = "session_full", message = "Session is full.")
            assertSessionStatePayload(reconnect, connectionIdExpected = true, inSong = true)
        } finally {
            holders.forEach { it.cancel() }
            clients.forEach { it.close() }
            controller.stop()
        }
    }

    @Test(timeout = 30_000)
    fun reconnectReplacesConnectionIdAndEmitsReconnectEventDuringLockedSession() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)
        controller.start(udpPort = SoloSingFixtures.UdpPort, wsPort = wsPort)

        val client = HttpClient(ClientOkHttp) { install(WebSockets) }
        client.webSocket(
            host = SoloSingFixtures.LoopbackHost,
            port = wsPort,
            path = "/",
            request = { parameter("token", SoloSingFixtures.SessionToken) },
        ) {
            send(Frame.Text(helloJson(SoloSingFixtures.PhoneClientId)))
            incoming.receiveTextFrame()
            val firstConnectionId = controller.connectedPhones.value.single().connectionId
            controller.broadcastPlaybackState(playbackStateMessage())
            val reconnectEvent = async { controller.phoneEvents.first { it is PhoneEvent.Reconnected } }

            val reconnectClient = HttpClient(ClientOkHttp) { install(WebSockets) }
            reconnectClient.webSocket(
                host = SoloSingFixtures.LoopbackHost,
                port = wsPort,
                path = "/",
                request = { parameter("token", SoloSingFixtures.SessionToken) },
            ) {
                send(
                    Frame.Text(
                        helloJson(
                            clientId = SoloSingFixtures.PhoneClientId,
                            deviceName = SoloSingFixtures.ReconnectedPhoneDeviceName,
                            httpPort = SoloSingFixtures.ReconnectedPhoneHttpPort,
                        ),
                    ),
                )
                incoming.receiveTextFrame()
                val phone = controller.connectedPhones.value.single()
                assertNotEquals(firstConnectionId, phone.connectionId)
                assertEquals(SoloSingFixtures.ReconnectedPhoneDeviceName, phone.deviceName)
                assertEquals(SoloSingFixtures.ReconnectedPhoneHttpPort, phone.httpPort)
                assertEquals(phone, (reconnectEvent.await() as PhoneEvent.Reconnected).phone)
            }
            reconnectClient.close()
        }
        client.close()
        controller.stop()
    }

    @Test(timeout = 30_000)
    fun initialSessionStateReportsAssignedConnectedSlotsOnReconnect() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)
        controller.start(udpPort = SoloSingFixtures.UdpPort, wsPort = wsPort)

        val client = HttpClient(ClientOkHttp) { install(WebSockets) }
        client.webSocket(
            host = SoloSingFixtures.LoopbackHost,
            port = wsPort,
            path = "/",
            request = { parameter("token", SoloSingFixtures.SessionToken) },
        ) {
            send(Frame.Text(helloJson(SoloSingFixtures.PhoneClientId)))
            incoming.receiveTextFrame()
            controller.sendAssignSinger(SoloSingFixtures.PhoneClientId, assignSingerMessage())
            incoming.receiveJson()
            controller.broadcastPlaybackState(playbackStateMessage())
            incoming.receiveJson()

            val reconnect = hello(
                port = wsPort,
                token = SoloSingFixtures.SessionToken,
                clientId = SoloSingFixtures.PhoneClientId,
                deviceName = SoloSingFixtures.ReconnectedPhoneDeviceName,
            )

            assertAssignedP1SessionState(reconnect)
        }
        client.close()
        controller.stop()
    }

    @Test(timeout = 30_000)
    fun outboundMessagesAreDeliveredToConnectedPhoneWebSocket() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)
        controller.start(udpPort = SoloSingFixtures.UdpPort, wsPort = wsPort)

        val client = HttpClient(ClientOkHttp) { install(WebSockets) }
        client.webSocket(
            host = SoloSingFixtures.LoopbackHost,
            port = wsPort,
            path = "/",
            request = { parameter("token", SoloSingFixtures.SessionToken) },
        ) {
            send(Frame.Text(helloJson(SoloSingFixtures.PhoneClientId)))
            incoming.receiveTextFrame()

            controller.sendAssignSinger(SoloSingFixtures.PhoneClientId, assignSingerMessage())
            assertAssignSingerPayload(incoming.receiveJson())

            controller.sendAssignSinger(
                SoloSingFixtures.PhoneClientId,
                assignSingerMessage(startMode = StartMode.Live, countdownMs = null),
            )
            assertLiveAssignSingerPayload(incoming.receiveJson())

            controller.broadcastPlaybackState(playbackStateMessage())
            assertPlaybackStatePayload(incoming.receiveJson())

            controller.broadcastPlaybackState(
                playbackStateMessage(
                    state = PlaybackNetworkState.Playing,
                    countdownRemainingMs = null,
                    tsTvMs = null,
                ),
            )
            assertPlayingPlaybackStatePayload(incoming.receiveJson())

            controller.sendSessionState(SoloSingFixtures.PhoneClientId)
            assertSessionStatePayload(
                payload = incoming.receiveJson(),
                connectionIdExpected = false,
                inSong = true,
                p1DeviceName = SoloSingFixtures.PhoneDeviceName,
            )

            val ping = async { controller.sendPing(SoloSingFixtures.PhoneClientId) }
            val pingPayload = answerPing(incoming.receiveJson())
            assertPongResponse(ping.await(), pingPayload)

            controller.sendClockAck(
                SoloSingFixtures.PhoneClientId,
                ClockAckMessage(
                    pingId = pingPayload.pingId,
                    tTvRecvMs = SoloSingFixtures.ClockAckTvTimeMs,
                ),
            )
            assertEquals("clockAck", incoming.receiveJson()["type"]?.jsonPrimitive?.content)
        }
        client.close()
        controller.stop()
    }

    @Test(timeout = 30_000)
    fun disconnectRemovesPhoneAndEmitsDisconnectEvent() = runBlocking {
        val wsPort = freePort()
        val controller = newController(wsPort = wsPort)
        controller.start(udpPort = SoloSingFixtures.UdpPort, wsPort = wsPort)
        val event = async { controller.phoneEvents.first { it is PhoneEvent.Disconnected } }

        val client = HttpClient(ClientOkHttp) { install(WebSockets) }
        client.webSocket(
            host = SoloSingFixtures.LoopbackHost,
            port = wsPort,
            path = "/",
            request = { parameter("token", SoloSingFixtures.SessionToken) },
        ) {
            send(Frame.Text(helloJson(SoloSingFixtures.PhoneClientId)))
            incoming.receiveTextFrame()
        }
        client.close()

        val disconnected = event.await() as PhoneEvent.Disconnected
        assertEquals(SoloSingFixtures.PhoneClientId, disconnected.clientId)
        assertTrue(controller.connectedPhones.value.isEmpty())

        controller.stop()
    }

    @Test(timeout = 30_000)
    fun fetchesManifestAndTxtFromRealPhoneHttpSourceWithoutCaching() = runBlocking {
        val manifestRequests = mutableListOf<String?>()
        val txtBytes = "#TITLE:${SoloSingFixtures.SongTitle}".encodeToByteArray()
        val httpPort = freePort()
        val phoneServer = embeddedServer(ServerCIO, host = SoloSingFixtures.LoopbackHost, port = httpPort) {
            routing {
                get("/manifest.json") {
                    manifestRequests += call.request.headers[HttpHeaders.CacheControl]
                    call.respondText("[${SoloSingFixtures.manifestSongJson()}]")
                }
                get("/songs/${SoloSingFixtures.RelativeTxtPath}") {
                    call.respondBytes(txtBytes)
                }
            }
        }.start(wait = false)
        val controller = newController()
        val phone = ConnectedPhone(
            clientId = SoloSingFixtures.PhoneClientId,
            connectionId = SoloSingFixtures.PhoneConnectionId,
            deviceName = SoloSingFixtures.PhoneDeviceName,
            httpPort = httpPort,
            ipAddress = SoloSingFixtures.LoopbackHost,
        )

        val first = controller.fetchManifest(phone).getOrThrow()
        val second = controller.fetchManifest(phone).getOrThrow()
        val txt = controller.fetchTxt(
            SoloSingFixtures.assetUrl(
                "/songs/${SoloSingFixtures.RelativeTxtPath}",
                host = SoloSingFixtures.LoopbackHost,
                port = httpPort,
            ),
        ).getOrThrow()

        assertEquals(first, second)
        assertEquals(listOf("no-cache", "no-cache"), manifestRequests)
        assertEquals(SoloSingFixtures.RelativeTxtPath, first.single().relativeTxtPath)
        assertArrayEquals(txtBytes, txt)

        phoneServer.stop(0, 0)
    }

    private fun joinQrPayload(wsPort: Int): String =
        "ws://${SoloSingFixtures.TvIpAddress}:$wsPort/?token=${SoloSingFixtures.SessionToken}"

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private fun newController(
        wsPort: Int = SoloSingFixtures.WebSocketPort,
        mdnsAdvertiser: MdnsAdvertiser = NoOpMdnsAdvertiser,
    ) = KtorNetworkController(
        sessionId = SoloSingFixtures.SessionId,
        sessionToken = SoloSingFixtures.SessionToken,
        joinCode = SoloSingFixtures.JoinCode,
        hostAddress = SoloSingFixtures.TvIpAddress,
        initialWsPort = wsPort,
        mdnsAdvertiser = mdnsAdvertiser,
    )

    private suspend fun hello(
        port: Int,
        token: String?,
        protocolVersion: Int = 1,
        clientId: String,
        deviceName: String = SoloSingFixtures.PhoneDeviceName,
        httpPort: Int = SoloSingFixtures.PhoneHttpPort,
        includeAppVersion: Boolean = true,
    ): JsonObject {
        val client = HttpClient(ClientOkHttp) { install(WebSockets) }
        var response: JsonObject? = null
        client.webSocket(
            host = SoloSingFixtures.LoopbackHost,
            port = port,
            path = "/",
            request = { if (token != null) parameter("token", token) },
        ) {
            send(Frame.Text(helloJson(clientId, protocolVersion, deviceName, httpPort, includeAppVersion)))
            response = incoming.receiveJson()
        }
        client.close()
        return requireNotNull(response)
    }

    private fun helloJson(
        clientId: String,
        protocolVersion: Int = 1,
        deviceName: String = SoloSingFixtures.PhoneDeviceName,
        httpPort: Int = SoloSingFixtures.PhoneHttpPort,
        includeAppVersion: Boolean = true,
    ): String = buildString {
        append(
            """
            {
                "type":"hello",
                "protocolVersion":$protocolVersion,
                "clientId":"$clientId",
                "deviceName":"$deviceName",
            """.trimIndent(),
        )
        if (includeAppVersion) append("\n    \"appVersion\":\"1.0\",")
        append("\n    \"httpPort\":$httpPort\n}")
    }

    private suspend fun kotlinx.coroutines.channels.ReceiveChannel<Frame>.receiveJson(): JsonObject =
        json.parseToJsonElement(receiveTextFrame()).jsonObject

    private suspend fun kotlinx.coroutines.channels.ReceiveChannel<Frame>.receiveTextFrame(): String =
        withTimeout(5_000L) { (receive() as Frame.Text).readText() }

    private suspend fun WebSocketSession.answerPing(pingMessage: JsonObject): PingPayload {
        assertEquals("ping", pingMessage["type"]?.jsonPrimitive?.content)
        assertEquals("1", pingMessage["protocolVersion"]?.jsonPrimitive?.content)
        val payload = PingPayload(
            pingId = requireNotNull(pingMessage["pingId"]?.jsonPrimitive?.content),
            tTvSendMs = requireNotNull(pingMessage["tTvSendMs"]?.jsonPrimitive?.content).toLong(),
        )
        assertFalse(pingMessage.containsKey("tvTimeMs"))
        send(
            Frame.Text(
                """
                {
                  "type":"pong",
                  "protocolVersion":1,
                  "pingId":"${payload.pingId}",
                  "tTvSendMs":${payload.tTvSendMs},
                  "tPhoneRecvMs":456,
                  "tPhoneSendMs":478
                }
                """.trimIndent(),
            ),
        )
        return payload
    }

    private fun assertPongResponse(pong: PongResponse, pingPayload: PingPayload) {
        assertEquals(pingPayload.pingId, pong.pingId)
        assertEquals(pingPayload.tTvSendMs, pong.tTvSendMs)
        assertEquals(456L, pong.tPhoneRecvMs)
        assertEquals(478L, pong.tPhoneSendMs)
    }

    private fun assertAssignSingerPayload(assignSinger: JsonObject) {
        assertEquals("assignSinger", assignSinger["type"]?.jsonPrimitive?.content)
        assertEquals(SoloSingFixtures.SessionId, assignSinger["sessionId"]?.jsonPrimitive?.content)
        assertEquals("P1", assignSinger["playerId"]?.jsonPrimitive?.content)
        assertEquals("Medium", assignSinger["difficulty"]?.jsonPrimitive?.content)
        assertEquals("countdown", assignSinger["startMode"]?.jsonPrimitive?.content)
        assertEquals("3000", assignSinger["countdownMs"]?.jsonPrimitive?.content)
        assertEquals(
            SoloSingFixtures.StopAtLyricsTimeMs.toString(),
            assignSinger["stopAtLyricsTimeMs"]?.jsonPrimitive?.content,
        )
        assertEquals(SoloSingFixtures.UdpPort.toString(), assignSinger["udpPort"]?.jsonPrimitive?.content)
        assertEquals(SoloSingFixtures.SongTitle, assignSinger["songTitle"]?.jsonPrimitive?.content)
        assertEquals(SoloSingFixtures.SongArtist, assignSinger["songArtist"]?.jsonPrimitive?.content)
        assertFalse(assignSinger.containsKey("connectionId"))
    }

    private fun assertLiveAssignSingerPayload(assignSinger: JsonObject) {
        assertEquals("assignSinger", assignSinger["type"]?.jsonPrimitive?.content)
        assertEquals("live", assignSinger["startMode"]?.jsonPrimitive?.content)
        assertFalse(assignSinger.containsKey("countdownMs"))
        assertFalse(assignSinger.containsKey("connectionId"))
    }

    private fun assertPlaybackStatePayload(playbackState: JsonObject) {
        assertEquals("playbackState", playbackState["type"]?.jsonPrimitive?.content)
        assertEquals(SoloSingFixtures.SessionId, playbackState["sessionId"]?.jsonPrimitive?.content)
        assertEquals(
            SoloSingFixtures.SongInstanceSeq.toString(),
            playbackState["songInstanceSeq"]?.jsonPrimitive?.content,
        )
        assertEquals("1", playbackState["revision"]?.jsonPrimitive?.content)
        assertEquals("countdown", playbackState["state"]?.jsonPrimitive?.content)
        assertEquals("0", playbackState["lyricsTimeMs"]?.jsonPrimitive?.content)
        assertEquals(
            SoloSingFixtures.StopAtLyricsTimeMs.toString(),
            playbackState["stopAtLyricsTimeMs"]?.jsonPrimitive?.content,
        )
        assertEquals("3000", playbackState["countdownRemainingMs"]?.jsonPrimitive?.content)
        assertEquals("", playbackState["reason"]?.jsonPrimitive?.content)
    }

    private fun assertPlayingPlaybackStatePayload(playbackState: JsonObject) {
        assertEquals("playbackState", playbackState["type"]?.jsonPrimitive?.content)
        assertEquals("playing", playbackState["state"]?.jsonPrimitive?.content)
        assertFalse(playbackState.containsKey("countdownRemainingMs"))
        assertFalse(playbackState.containsKey("tsTvMs"))
    }

    private fun assertSessionStatePayload(
        payload: JsonObject,
        connectionIdExpected: Boolean,
        inSong: Boolean,
        p1DeviceName: String = "",
    ) {
        assertEquals("sessionState", payload["type"]?.jsonPrimitive?.content)
        assertEquals("1", payload["protocolVersion"]?.jsonPrimitive?.content)
        assertEquals(SoloSingFixtures.SessionId, payload["sessionId"]?.jsonPrimitive?.content)
        assertEquals(inSong.toString(), payload["inSong"]?.jsonPrimitive?.content)
        val connectionId = payload["connectionId"]
        if (connectionIdExpected) {
            assertTrue(connectionId != null && connectionId.toString() != "null")
        } else {
            assertFalse(payload.containsKey("connectionId"))
        }
        assertFalse(payload.containsKey("songTimeSec"))
        val slots = requireNotNull(payload["slots"]?.jsonObject)
        val p1 = requireNotNull(slots["P1"]?.jsonObject)
        val p2 = requireNotNull(slots["P2"]?.jsonObject)
        assertEquals((p1DeviceName.isNotEmpty()).toString(), p1["connected"]?.jsonPrimitive?.content)
        assertEquals(p1DeviceName, p1["deviceName"]?.jsonPrimitive?.content)
        assertEquals("false", p2["connected"]?.jsonPrimitive?.content)
        assertEquals("", p2["deviceName"]?.jsonPrimitive?.content)
        assertFalse(payload.containsKey("joinCode"))
        assertFalse(payload.containsKey("connectedPhones"))
        assertFalse(payload.containsKey("locked"))
    }

    private fun assertErrorPayload(payload: JsonObject, code: String, message: String) {
        assertEquals("error", payload["type"]?.jsonPrimitive?.content)
        assertEquals("1", payload["protocolVersion"]?.jsonPrimitive?.content)
        assertEquals(code, payload["code"]?.jsonPrimitive?.content)
        assertEquals(message, payload["message"]?.jsonPrimitive?.content)
        assertFalse(payload.containsKey("sessionId"))
    }

    private fun assertAssignedP1SessionState(payload: JsonObject) {
        assertEquals("sessionState", payload["type"]?.jsonPrimitive?.content)
        val slots = requireNotNull(payload["slots"]?.jsonObject)
        val p1 = requireNotNull(slots["P1"]?.jsonObject)
        val p2 = requireNotNull(slots["P2"]?.jsonObject)
        assertEquals("true", p1["connected"]?.jsonPrimitive?.content)
        assertEquals(SoloSingFixtures.ReconnectedPhoneDeviceName, p1["deviceName"]?.jsonPrimitive?.content)
        assertEquals("false", p2["connected"]?.jsonPrimitive?.content)
        assertEquals("", p2["deviceName"]?.jsonPrimitive?.content)
    }

    private fun assignSingerMessage(
        startMode: StartMode = StartMode.Countdown,
        countdownMs: Int? = 3_000,
    ) = AssignSingerMessage(
        sessionId = SoloSingFixtures.SessionId,
        songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
        playerId = PlayerId.P1,
        difficulty = Difficulty.Medium,
        startMode = startMode,
        countdownMs = countdownMs,
        stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        udpPort = SoloSingFixtures.UdpPort,
        songTitle = SoloSingFixtures.SongTitle,
        songArtist = SoloSingFixtures.SongArtist,
    )

    private fun playbackStateMessage(
        state: PlaybackNetworkState = PlaybackNetworkState.Countdown,
        countdownRemainingMs: Int? = 3_000,
        tsTvMs: Long? = null,
    ) = PlaybackStateMessage(
        sessionId = SoloSingFixtures.SessionId,
        songInstanceSeq = SoloSingFixtures.SongInstanceSeq,
        revision = 1L,
        state = state,
        lyricsTimeMs = 0L,
        stopAtLyricsTimeMs = SoloSingFixtures.StopAtLyricsTimeMs,
        countdownRemainingMs = countdownRemainingMs,
        reason = PlaybackStateReason.Unspecified,
        tsTvMs = tsTvMs,
    )

    private data class PingPayload(
        val pingId: String,
        val tTvSendMs: Long,
    )

    private object NoOpMdnsAdvertiser : MdnsAdvertiser {
        override fun advertise(advertisement: MdnsAdvertisement) = Unit
        override fun stop(advertisement: MdnsAdvertisement) = Unit
    }

    private class RecordingMdnsAdvertiser : MdnsAdvertiser {
        val advertised = mutableListOf<MdnsAdvertisement>()
        val stopped = mutableListOf<MdnsAdvertisement>()
        override fun advertise(advertisement: MdnsAdvertisement) {
            advertised += advertisement
        }
        override fun stop(advertisement: MdnsAdvertisement) {
            stopped += advertisement
        }
    }
}
