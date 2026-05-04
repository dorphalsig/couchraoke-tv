package com.couchraoke.tv.data.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicInteger

private val connectionIdCounter = AtomicInteger(0)

// 9 NetworkController interface methods + 3 test-seam methods (acceptsHelloToken, lockSession, handleHello).
@Suppress("TooManyFunctions")
class KtorNetworkController(
    private val sessionId: String = "tv-session",
    private val sessionToken: String = "ABCDEFGH",
    private val joinCode: String = sessionToken,
    private val hostAddress: String = "127.0.0.1",
    private val initialWsPort: Int? = null,
    initialConnectedPhones: List<ConnectedPhone> = emptyList(),
    private val manifestResponses: Map<String, String> = emptyMap(),
    private val txtResponses: Map<String, ByteArray> = emptyMap(),
) : NetworkController {
    private val mutableConnectedPhones = MutableStateFlow(initialConnectedPhones)
    private val mutablePhoneEvents = MutableSharedFlow<PhoneEvent>()
    private val mutableSessionState = MutableStateFlow(
        SessionState(
            sessionId = sessionId,
            sessionToken = sessionToken,
            joinCode = joinCode,
            connectedPhones = initialConnectedPhones,
            isLocked = false,
        )
    )
    private val mutableMdnsAdvertisement = MutableStateFlow<MdnsAdvertisement?>(null)
    private val mutableFetchedTxtUrls = mutableListOf<String>()
    private val mutableFetchedManifestClientIds = mutableListOf<String>()
    private var wsPort: Int? = initialWsPort

    override val connectedPhones: StateFlow<List<ConnectedPhone>> = mutableConnectedPhones
    override val phoneEvents: SharedFlow<PhoneEvent> = mutablePhoneEvents
    val sessionState: StateFlow<SessionState> = mutableSessionState
    val mdnsAdvertisement: StateFlow<MdnsAdvertisement?> = mutableMdnsAdvertisement
    val fetchedTxtUrls: List<String> get() = mutableFetchedTxtUrls.toList()
    val fetchedManifestClientIds: List<String> get() = mutableFetchedManifestClientIds.toList()
    val joinEndpointUrl: String get() = "ws://$hostAddress:${requireNotNull(wsPort)}/?token=$sessionToken"

    override suspend fun start(udpPort: Int, wsPort: Int) {
        this.wsPort = wsPort
        mutableMdnsAdvertisement.value = MdnsAdvertisement(
            serviceType = "_karaoke._tcp",
            port = wsPort,
            txtRecords = mapOf(
                "code" to joinCode.uppercase(),
                "v" to "1",
                "udpPort" to udpPort.toString(),
            ),
        )
    }

    override suspend fun stop() {
        wsPort = null
        mutableMdnsAdvertisement.value = null
        mutableConnectedPhones.value = emptyList()
        mutableSessionState.value = mutableSessionState.value.copy(connectedPhones = emptyList(), isLocked = false)
    }

    fun acceptsHelloToken(token: String): Boolean = token == sessionToken

    fun lockSession() {
        mutableSessionState.value = mutableSessionState.value.copy(isLocked = true)
    }

    fun handleHello(
        token: String,
        protocolVersion: Int,
        clientId: String,
        deviceName: String,
        httpPort: Int,
        ipAddress: String,
    ): HelloResult {
        val current = mutableSessionState.value
        val rejectionError = when {
            protocolVersion != 1 -> HelloError.ProtocolMismatch
            token != sessionToken -> HelloError.InvalidToken
            current.isLocked && current.connectedPhones.none { it.clientId == clientId } -> HelloError.SessionLocked
            else -> null
        }
        if (rejectionError != null) return HelloResult.Rejected(rejectionError)

        val phone = ConnectedPhone(
            clientId = clientId,
            connectionId = connectionIdCounter.incrementAndGet().toUShort(),
            deviceName = deviceName,
            httpPort = httpPort,
            ipAddress = ipAddress,
        )
        val updatedPhones = if (current.connectedPhones.any { it.clientId == clientId }) {
            current.connectedPhones.map { if (it.clientId == clientId) phone else it }
        } else {
            current.connectedPhones + phone
        }
        mutableConnectedPhones.value = updatedPhones
        mutableSessionState.value = current.copy(connectedPhones = updatedPhones)
        return HelloResult.Accepted(phone = phone)
    }

    override suspend fun fetchManifest(phone: ConnectedPhone): Result<List<SongEntry>> = runCatching {
        mutableFetchedManifestClientIds += phone.clientId
        parseManifestJson(requireNotNull(manifestResponses[phone.clientId]))
    }

    override suspend fun fetchTxt(url: String): Result<ByteArray> = runCatching {
        mutableFetchedTxtUrls += url
        requireNotNull(txtResponses[url])
    }

    override suspend fun sendAssignSinger(phoneId: String, message: AssignSingerMessage) = Unit

    override suspend fun broadcastPlaybackState(message: PlaybackStateMessage) = Unit

    override suspend fun sendSessionState(phoneId: String) = Unit

    override suspend fun sendPing(phoneId: String): PongResponse = PongResponse(
        phoneId = phoneId,
        phoneTimeMs = 0L,
        tvReceiveTimeMs = 0L,
        isValidSample = true,
    )

    override suspend fun sendClockAck(phoneId: String, ack: ClockAckMessage) = Unit
}

sealed interface HelloResult {
    data class Accepted(val phone: ConnectedPhone) : HelloResult
    data class Rejected(val error: HelloError) : HelloResult
}

enum class HelloError {
    ProtocolMismatch,
    InvalidToken,
    SessionLocked,
}

data class MdnsAdvertisement(
    val serviceType: String,
    val port: Int,
    val txtRecords: Map<String, String>,
)
