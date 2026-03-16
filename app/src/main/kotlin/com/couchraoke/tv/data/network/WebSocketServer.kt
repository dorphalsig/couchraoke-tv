package com.couchraoke.tv.data.network

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

import android.util.Log
import com.couchraoke.tv.domain.library.SongLibrary
import com.couchraoke.tv.domain.network.clock.ClockSyncEngine
import com.couchraoke.tv.domain.network.protocol.*
import com.couchraoke.tv.domain.session.ConnectionRegistry
import com.couchraoke.tv.domain.session.IConnectionCloser
import com.couchraoke.tv.domain.session.ISessionCallbacks
import com.couchraoke.tv.domain.session.ISessionGate
import com.couchraoke.tv.domain.session.SessionToken

class WebSocketServer(
    private val token: String,
    private val gate: ISessionGate,
    private val registry: ConnectionRegistry,
    private val library: SongLibrary,
    private val manifestFetcher: ManifestFetcher,
    private val clockEngine: ClockSyncEngine,
    private val port: Int = 8080,
    private val callbacks: ISessionCallbacks? = null,
) : IConnectionCloser {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private val activeSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    fun start() {
        if (server != null) return
        server = embeddedServer(CIO, port = port) {
            installWebSockets()
            routing {
                configureWebSocket()
            }
        }.start(wait = false)
        Log.i(TAG, "WebSocket server started on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        Log.i(TAG, "WebSocket server stopped")
    }

    fun Application.installWebSockets() {
        install(WebSockets)
    }

    fun Route.configureWebSocket() {
        webSocket("/") {
            handleConnection(this)
        }
    }

    private suspend fun handleConnection(session: DefaultWebSocketServerSession) {
        val queryToken = session.call.parameters["token"] ?: ""
        val phoneIp = session.call.request.origin.remoteAddress

        // 1. Validate token
        if (!SessionToken.matches(queryToken, token)) {
            Log.w(TAG, "Invalid token from $phoneIp")
            session.sendErrorAndClose("invalid_token", "Invalid session token")
            return
        }

        // 2. Receive first frame (text)
        val firstFrame = session.incoming.receive()
        if (firstFrame !is Frame.Text) {
            Log.w(TAG, "First frame not text from $phoneIp")
            session.sendErrorAndClose("error", "First frame must be text")
            return
        }

        val firstText = firstFrame.readText()
        val type = ControlMessageCodec.decodeType(firstText)

        // Decode type -> if not "hello" -> reject error + close
        if (type != "hello") {
            Log.w(TAG, "Expected hello message, got $type from $phoneIp")
            session.sendErrorAndClose("error", "Expected hello message")
            return
        }

        // 3. Decode HelloMessage
        val hello = try {
            ControlMessageCodec.json.decodeFromString<HelloMessage>(firstText)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode hello message: ${e.message}")
            session.sendErrorAndClose("error", "Invalid hello message")
            return
        }

        if (hello.protocolVersion != 1) {
            Log.w(TAG, "Protocol mismatch from ${hello.clientId}: ${hello.protocolVersion}")
            session.sendErrorAndClose("protocol_mismatch", "Protocol version 1 required")
            return
        }

        // 4. httpPort absent (0 or null) -> reject with generic error
        if (hello.httpPort <= 0) {
            Log.w(TAG, "Invalid httpPort from ${hello.clientId}: ${hello.httpPort}")
            session.sendErrorAndClose("error", "Invalid HTTP port")
            return
        }

        // 5. Reconnect detection (bypass lock/cap)
        val existing = registry.getByClientId(hello.clientId)
        val isReconnect = existing != null

        if (!isReconnect) {
            // 6. registry.connections.size >= gate.maxConnections -> reject session_full
            if (registry.size >= gate.maxConnections) {
                Log.w(TAG, "Session full, rejecting ${hello.clientId}")
                session.sendErrorAndClose("session_full", "Session is full")
                return
            }

            // 7. gate.isLocked -> reject session_locked
            if (gate.isLocked) {
                Log.w(TAG, "Session locked, rejecting ${hello.clientId}")
                session.sendErrorAndClose("session_locked", "Session is locked")
                return
            }
        } else {
            // Close old session if still active
            activeSessions[hello.clientId]?.let { oldSession ->
                session.launch {
                    try {
                        oldSession.close(
                            CloseReason(
                                CloseReason.Codes.PROTOCOL_ERROR,
                                "Reconnected from another session"
                            )
                        )
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }

        // Success
        val connectionId = registry.register(
            hello.clientId,
            hello.deviceName,
            hello.httpPort,
            phoneIp,
            playerSlot = null
        )
        activeSessions[hello.clientId] = session

        if (isReconnect) {
            callbacks?.onPhoneReconnected(hello.clientId, connectionId)
        } else {
            callbacks?.onPhoneConnected(hello.clientId, hello.deviceName, connectionId)
        }

        // Send SessionStateMessage
        val sessionState = JsonObject(mapOf(
            "type" to JsonPrimitive("sessionState"),
            "protocolVersion" to JsonPrimitive(1),
            "connectionId" to JsonPrimitive(connectionId.toInt()),
            "sessionId" to JsonPrimitive(gate.sessionId),
            "slots" to ControlMessageCodec.json.encodeToJsonElement(gate.slots),
            "inSong" to JsonPrimitive(gate.inSong),
            "songTimeSec" to JsonNull,
            "tsTvMs" to JsonNull
        ))
        session.send(Frame.Text(ControlMessageCodec.json.encodeToString(sessionState)))

        // Proactor - launch two coroutines (do NOT await)
        val scope = session // DefaultWebSocketServerSession is a CoroutineScope
        scope.launch(Dispatchers.IO) {
            manifestFetcher.fetch(phoneIp, hello.httpPort, hello.clientId)
        }
        scope.launch(Dispatchers.IO) {
            clockEngine.startInitialSync(hello.clientId)
        }

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val incomingType = ControlMessageCodec.decodeType(text)
                    if (incomingType == "pong") {
                        val pong = ControlMessageCodec.json.decodeFromString<PongMessage>(text)
                        val ack = clockEngine.onPong(pong)
                        session.send(Frame.Text(ControlMessageCodec.json.encodeToString(ack)))
                    } else {
                        Log.d(TAG, "Ignored message type $incomingType from ${hello.clientId}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Connection closed for ${hello.clientId}: ${e.message}")
        } finally {
            registry.deregister(hello.clientId)
            library.removePhone(hello.clientId)
            activeSessions.remove(hello.clientId)
            callbacks?.onPhoneDisconnected(hello.clientId)
            Log.i(TAG, "Deregistered ${hello.clientId}")
        }
    }

    override fun closeConnection(clientId: String) {
        activeSessions[clientId]?.let { session ->
            session.launch(Dispatchers.IO) {
                try {
                    session.close(CloseReason(CloseReason.Codes.NORMAL, "Closed by TV"))
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.sendErrorAndClose(code: String, message: String) {
        val error = JsonObject(mapOf(
            "type" to JsonPrimitive("error"),
            "protocolVersion" to JsonPrimitive(1),
            "code" to JsonPrimitive(code),
            "message" to JsonPrimitive(message),
            "tsTvMs" to JsonNull
        ))
        send(Frame.Text(ControlMessageCodec.json.encodeToString(error)))
        close(CloseReason(CloseReason.Codes.NORMAL, code))
    }

    companion object {
        private const val TAG = "WebSocketServer"
    }
}
