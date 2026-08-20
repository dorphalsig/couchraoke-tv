package com.couchraoke.tv.domain.control

/**
 * Starts and stops the control-channel WebSocket listener. Implemented by
 * `com.couchraoke.tv.data.control.KtorControlTransport` (contracts/ports.md).
 */
interface ControlTransport {
    suspend fun start(port: Int, handler: ControlConnectionHandler): StartedTransport
    suspend fun stop()
}

/**
 * A running [ControlTransport]. [boundPort] is the **actual** bound port, not the
 * requested one — passing `0` to [ControlTransport.start] yields an ephemeral port, and
 * [boundPort] is the value that must be advertised (FR-004).
 */
interface StartedTransport {
    val boundPort: Int
}

/** Receives each accepted [ControlConnection] as it arrives. */
interface ControlConnectionHandler {
    suspend fun onConnection(connection: ControlConnection)
}

/**
 * A single accepted control-channel connection.
 *
 * [token] is the `token` query parameter, or `null` when absent; the transport does not
 * validate it — `JoinCodeMatcher` does. [receiveText] returns `null` when the peer closes.
 *
 * [refuse] is one operation rather than a send followed by a close, because FR-016
 * requires the `error` frame to arrive **before** the close and the two must not be
 * separable by a caller. The adapter sends the frame, flushes, then closes with `1008`
 * and close reason equal to `code`. Splitting this into two calls is what produces the
 * peer's exit 4.
 */
interface ControlConnection {
    val token: String?
    suspend fun receiveText(): String?
    suspend fun sendText(text: String)
    suspend fun refuse(code: String, message: String)
    suspend fun close()
}
