package com.couchraoke.tv.data.network

import com.couchraoke.tv.domain.model.PlayerId
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.websocket.WebSockets as ServerWebSockets

private const val MAX_CONNECTED_PHONES = 10
private val connectionIdCounter = AtomicInteger(0)

@Suppress("TooManyFunctions")
class KtorNetworkController(
    private val sessionId: String,
    private val sessionToken: String,
    private val joinCode: String = sessionToken,
    private val hostAddress: String,
    private val initialWsPort: Int? = null,
    private val httpClient: HttpClient = HttpClient(OkHttp) { install(WebSockets) },
    private val mdnsAdvertiser: MdnsAdvertiser = JmDnsMdnsAdvertiser(hostAddress),
) : NetworkController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableConnectedPhones = MutableStateFlow<List<ConnectedPhone>>(emptyList())
    private val mutablePhoneEvents = MutableSharedFlow<PhoneEvent>()
    private val mutableSessionState = MutableStateFlow(
        SessionState(
            sessionId = sessionId,
            sessionToken = sessionToken,
            joinCode = joinCode,
            connectedPhones = emptyList(),
            isLocked = false,
        )
    )
    private val mutableMdnsAdvertisement = MutableStateFlow<MdnsAdvertisement?>(null)
    private val clientSessions = ConcurrentHashMap<String, WebSocketSession>()
    private val assignedPlayerPhoneIds = ConcurrentHashMap<PlayerId, String>()
    private val pendingPongs = ConcurrentHashMap<String, PendingPong>()
    private var server: EmbeddedServer<out ApplicationEngine, out ApplicationEngine.Configuration>? = null
    private var wsPort: Int? = initialWsPort

    override val connectedPhones: StateFlow<List<ConnectedPhone>> = mutableConnectedPhones
    override val phoneEvents: SharedFlow<PhoneEvent> = mutablePhoneEvents
    val sessionState: StateFlow<SessionState> = mutableSessionState
    val mdnsAdvertisement: StateFlow<MdnsAdvertisement?> = mutableMdnsAdvertisement
    val joinEndpointUrl: String get() = "ws://$hostAddress:${requireNotNull(wsPort)}/?token=$sessionToken"

    override suspend fun start(udpPort: Int, wsPort: Int) {
        stopServerOnly()
        this.wsPort = wsPort
        server = embeddedServer(ServerCIO, host = "0.0.0.0", port = wsPort) {
            install(ServerWebSockets)
            routing {
                webSocket("/") {
                    handleSocket(
                        token = call.request.queryParameters["token"],
                        remoteHost = call.request.local.remoteHost,
                    )
                }
            }
        }.start(wait = false)
        val advertisement = MdnsAdvertisement(
            serviceType = "_karaoke._tcp.local.",
            port = wsPort,
            txtRecords = mapOf(
                "code" to joinCode.uppercase(),
                "v" to "1",
                "udpPort" to udpPort.toString(),
            ),
        )
        mutableMdnsAdvertisement.value = advertisement
        withContext(Dispatchers.IO) { mdnsAdvertiser.advertise(advertisement) }
    }

    override suspend fun stop() {
        stopServerOnly()
        mutableConnectedPhones.value = emptyList()
        assignedPlayerPhoneIds.clear()
        updateSession(connectedPhones = emptyList(), isLocked = false)
        pendingPongs.values.forEach { it.response.cancel() }
        pendingPongs.clear()
    }

    override suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>> = runCatching {
        val response = httpClient.get("http://${phone.ipAddress}:${phone.httpPort}/manifest.json") {
            header(HttpHeaders.CacheControl, "no-cache")
            header(HttpHeaders.Pragma, "no-cache")
        }
        parseManifestJson(response.bodyAsText())
    }

    override suspend fun fetchTxt(url: String): Result<ByteArray> = runCatching {
        httpClient.get(url).bodyAsBytes()
    }

    override suspend fun sendAssignSinger(phoneId: String, message: AssignSingerMessage) {
        assignedPlayerPhoneIds[message.playerId] = phoneId
        clientSessions[phoneId]?.sendSerialized(message)
    }

    override suspend fun broadcastPlaybackState(message: PlaybackStateMessage) {
        if (message.state == PlaybackNetworkState.Stopped) assignedPlayerPhoneIds.clear()
        updateSession(isLocked = message.state != PlaybackNetworkState.Stopped)
        clientSessions.values.forEach { it.sendSerialized(message) }
    }

    override suspend fun sendSessionState(phoneId: String) {
        clientSessions[phoneId]?.send(
            sessionStatePayload(
                phone = clientSessionsPhone(phoneId),
                includeConnectionId = false,
            ),
        )
    }

    override suspend fun sendPing(phoneId: String): PongResponse {
        val session = clientSessions[phoneId] ?: return invalidPong(phoneId)
        val ping = PendingPong(
            pingId = UUID.randomUUID().toString(),
            tTvSendMs = nowMs(),
            response = CompletableDeferred(),
        )
        pendingPongs[phoneId] = ping
        session.send(encodeNetworkMessage(PingWireMessage(pingId = ping.pingId, tTvSendMs = ping.tTvSendMs)))
        return withTimeoutOrNull(1_000L) { ping.response.await() } ?: invalidPong(phoneId)
    }

    override suspend fun sendClockAck(phoneId: String, ack: ClockAckMessage) {
        clientSessions[phoneId]?.sendSerialized(ack)
    }

    private suspend fun WebSocketSession.handleSocket(token: String?, remoteHost: String) {
        var clientId: String? = null
        try {
            if (token != sessionToken) {
                sendErrorAndClose("invalid_token")
            } else {
                clientId = handleAcceptedSocket(remoteHost)
            }
        } finally {
            unregisterPhone(clientId)
        }
    }

    private suspend fun WebSocketSession.handleAcceptedSocket(remoteHost: String): String? {
        val hello = receiveHello() ?: return null
        val current = mutableSessionState.value
        val existing = current.connectedPhones.firstOrNull { it.clientId == hello.clientId }
        return when {
            existing == null && current.connectedPhones.size >= MAX_CONNECTED_PHONES -> {
                sendErrorAndClose("session_full")
                null
            }
            current.isLocked && existing == null -> {
                sendErrorAndClose("session_locked")
                null
            }
            else -> {
                val phone = hello.toConnectedPhone(remoteHost)
                registerPhone(phone = phone, existing = existing, current = current)
                send(sessionStatePayload(phone = phone, includeConnectionId = true))
                readPhoneMessages(phone.clientId)
                phone.clientId
            }
        }
    }

    private suspend fun WebSocketSession.receiveHello(): HelloWireMessage? {
        val frame = incoming.receive() as? Frame.Text
        val hello = frame?.readText()?.let(::decodeHello)
        return if (hello != null && hello.protocolVersion == 1 && hello.type == "hello") {
            hello
        } else {
            sendErrorAndClose("protocol_mismatch")
            null
        }
    }

    private fun HelloWireMessage.toConnectedPhone(remoteHost: String): ConnectedPhone = ConnectedPhone(
        clientId = clientId,
        connectionId = connectionIdCounter.incrementAndGet().toUShort(),
        deviceName = deviceName,
        httpPort = httpPort,
        ipAddress = remoteHost,
    )

    private suspend fun WebSocketSession.registerPhone(
        phone: ConnectedPhone,
        existing: ConnectedPhone?,
        current: SessionState,
    ) {
        clientSessions[phone.clientId]?.close()
        clientSessions[phone.clientId] = this
        val updatedPhones = if (existing == null) {
            current.connectedPhones + phone
        } else {
            current.connectedPhones.map { if (it.clientId == phone.clientId) phone else it }
        }
        mutableConnectedPhones.value = updatedPhones
        updateSession(connectedPhones = updatedPhones)
        if (existing == null) {
            mutablePhoneEvents.emit(PhoneEvent.Connected(phone))
        } else {
            mutablePhoneEvents.emit(PhoneEvent.Reconnected(phone = phone, wasAssignedSinger = current.isLocked))
        }
    }

    private suspend fun WebSocketSession.readPhoneMessages(clientId: String) {
        for (incomingFrame in incoming) {
            if (incomingFrame is Frame.Text) handleClientMessage(clientId, incomingFrame.readText())
        }
    }

    private suspend fun WebSocketSession.unregisterPhone(clientId: String?) {
        val id = clientId ?: return
        if (clientSessions[id] == this) {
            clientSessions.remove(id)
            val updatedPhones = mutableConnectedPhones.value.filterNot { it.clientId == id }
            mutableConnectedPhones.value = updatedPhones
            updateSession(connectedPhones = updatedPhones)
            mutablePhoneEvents.emit(
                PhoneEvent.Disconnected(
                    clientId = id,
                    wasAssignedSinger = mutableSessionState.value.isLocked,
                ),
            )
        }
    }

    private fun decodeHello(text: String): HelloWireMessage? = runCatching {
        wireJson.decodeFromString<HelloWireMessage>(text)
    }.getOrNull()

    private fun handleClientMessage(phoneId: String, text: String) {
        val json = runCatching { wireJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        when (json["type"]?.jsonPrimitive?.content) {
            "pong" -> completePong(phoneId, json)
        }
    }

    private suspend fun WebSocketSession.sendErrorAndClose(code: String) {
        send(encodeNetworkMessage(ErrorWireMessage(code = code, message = errorMessageFor(code))))
        close()
    }

    private suspend inline fun <reified T> WebSocketSession.sendSerialized(message: T) {
        send(encodeNetworkMessage(message))
    }

    private fun clientSessionsPhone(phoneId: String): ConnectedPhone? =
        mutableConnectedPhones.value.firstOrNull { it.clientId == phoneId }

    private fun sessionStatePayload(
        phone: ConnectedPhone?,
        includeConnectionId: Boolean,
    ): String = encodeNetworkMessage(
        SessionStateWireMessage(
            sessionId = sessionId,
            slots = sessionSlotsPayload(),
            inSong = mutableSessionState.value.isLocked,
            connectionId = phone?.connectionId?.toInt()?.takeIf { includeConnectionId },
        ),
    )

    private fun sessionSlotsPayload(): SessionSlotsWireMessage = SessionSlotsWireMessage(
        p1 = slotPayload(PlayerId.P1),
        p2 = slotPayload(PlayerId.P2),
    )

    private fun slotPayload(playerId: PlayerId): SessionSlotWireMessage {
        val phone = assignedPlayerPhoneIds[playerId]?.let(::clientSessionsPhone)
        return SessionSlotWireMessage(
            connected = phone != null,
            deviceName = phone?.deviceName.orEmpty(),
        )
    }

    private fun completePong(phoneId: String, json: kotlinx.serialization.json.JsonObject) {
        val pending = pendingPongs.remove(phoneId) ?: return
        val pingId = json["pingId"]?.jsonPrimitive?.content.orEmpty()
        val tTvSendMs = json["tTvSendMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val tPhoneRecvMs = json["tPhoneRecvMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val tPhoneSendMs = json["tPhoneSendMs"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        pending.response.complete(
            PongResponse(
                phoneId = phoneId,
                pingId = pingId,
                tTvSendMs = tTvSendMs,
                tPhoneRecvMs = tPhoneRecvMs,
                tPhoneSendMs = tPhoneSendMs,
                tvReceiveTimeMs = nowMs(),
                isValidSample = pingId == pending.pingId && tTvSendMs == pending.tTvSendMs,
            ),
        )
    }

    private fun invalidPong(phoneId: String): PongResponse = PongResponse(
        phoneId = phoneId,
        tvReceiveTimeMs = nowMs(),
        isValidSample = false,
    )

    private fun updateSession(
        connectedPhones: List<ConnectedPhone> = mutableConnectedPhones.value,
        isLocked: Boolean = mutableSessionState.value.isLocked,
    ) {
        mutableSessionState.value = mutableSessionState.value.copy(
            connectedPhones = connectedPhones,
            isLocked = isLocked,
        )
    }

    private suspend fun stopServerOnly() {
        val advertisement = mutableMdnsAdvertisement.value
        if (advertisement != null) withContext(Dispatchers.IO) { mdnsAdvertiser.stop(advertisement) }
        mutableMdnsAdvertisement.value = null
        clientSessions.values.forEach { session -> scope.launch { session.close() } }
        clientSessions.clear()
        server?.stop(0, 0)
        server = null
        wsPort = null
    }
}

private data class PendingPong(
    val pingId: String,
    val tTvSendMs: Long,
    val response: CompletableDeferred<PongResponse>,
)

private fun nowMs(): Long = System.nanoTime() / 1_000_000
