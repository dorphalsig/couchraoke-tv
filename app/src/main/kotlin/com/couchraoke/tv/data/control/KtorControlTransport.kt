package com.couchraoke.tv.data.control

import com.couchraoke.tv.domain.control.ControlConnection
import com.couchraoke.tv.domain.control.ControlConnectionHandler
import com.couchraoke.tv.domain.control.ControlMessageCodec
import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.StartedTransport
import com.couchraoke.tv.domain.control.model.Refusal
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send

/**
 * Ktor CIO WebSocket adapter for [ControlTransport] (contracts/ports.md, research.md R2).
 *
 * Carries no admission or protocol decision: `JoinCodeMatcher` and `HandshakeValidator`
 * (later units) own those. [codec] is used only to encode the already-modelled [Refusal]
 * value that [refuse] is handed as a plain `code`/`message` pair — building that value and
 * delegating its encoding to the domain codec is not a decision, per research.md R8; it is
 * the one place [refuse]'s send-then-close contract (FR-016) has to be assembled.
 */
class KtorControlTransport(private val codec: ControlMessageCodec) : ControlTransport {

    private var server: EmbeddedServer<*, *>? = null

    override suspend fun start(port: Int, handler: ControlConnectionHandler): StartedTransport {
        val embedded = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(WebSockets)
            routing {
                webSocket("/") {
                    val token = call.request.queryParameters["token"]
                    handler.onConnection(KtorControlConnection(this, token, codec))
                }
            }
        }
        // startSuspend(wait = false) only returns once CIO has completed its resolved-connector
        // deferred, so resolvedConnectors() below reflects the real bound port, not the requested one.
        embedded.startSuspend(wait = false)
        server = embedded
        val resolvedPort = embedded.engine.resolvedConnectors().first().port
        return object : StartedTransport {
            override val boundPort: Int = resolvedPort
        }
    }

    override suspend fun stop() {
        server?.stopSuspend()
        server = null
    }
}

private class KtorControlConnection(
    private val session: DefaultWebSocketServerSession,
    override val token: String?,
    private val codec: ControlMessageCodec,
) : ControlConnection {

    override suspend fun receiveText(): String? {
        for (frame in session.incoming) {
            if (frame is Frame.Text) return frame.readText()
        }
        return null
    }

    override suspend fun sendText(text: String) {
        session.send(Frame.Text(text))
    }

    override suspend fun refuse(code: String, message: String) {
        session.send(Frame.Text(codec.encodeError(Refusal(code = code, message = message))))
        session.flush()
        session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, code))
    }

    override suspend fun close() {
        session.close()
    }
}
