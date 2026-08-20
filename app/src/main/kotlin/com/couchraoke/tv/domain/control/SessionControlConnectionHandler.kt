package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.control.model.ConnectedDeviceDto
import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.domain.control.model.SessionState
import com.couchraoke.tv.domain.control.model.SlotDto
import com.couchraoke.tv.domain.control.model.Slots
import com.couchraoke.tv.domain.session.SessionCoordinator
import com.couchraoke.tv.domain.session.model.DeviceId

private const val UNASSIGNED_SLOT_STATE = "connected_unassigned"

/**
 * The production join dispatch (T036; spec.md Observation 18 — no task previously owned
 * this class, so nothing implemented [ControlConnectionHandler] until now).
 *
 * Per T036's literal sequence: check the query-string token with `coordinator.authorize`,
 * then read the peer's first frame, decode it with [codec], call `coordinator.admit(hello)`,
 * send the `sessionState` reply on acceptance, and route the socket close to
 * `coordinator.onDisconnected`.
 *
 * The token is checked *before* the first frame is read, not after: it arrives on the
 * connect URL (FR-009), so nothing about it depends on `hello`, and refusing up front means
 * an unauthorized peer can never hold the handler waiting on a frame it may never send.
 *
 * A refusal from either step is delivered through [ControlConnection.refuse], which sends
 * the `error` frame and closes in one operation — splitting those is what produces the
 * peer's exit 4 (FR-016/FR-017).
 *
 * `admit`'s unimplemented branches (`RosterAdmission.Reclaimed`, `RosterAdmission.AtCapacity`
 * — T057/T050) are deliberately left to throw rather than being normalised away: they only
 * fire for capacity and reclaim scenarios this slice's US1 path never exercises, so letting
 * them surface loudly is preferable to a silent fallthrough.
 *
 * A `hello` that fails to decode is left alone — no refusal is sent, the connection is
 * simply not carried forward — because `invalid_message` is `RefusalReason` vocabulary
 * owned by T050/T049 (`HandshakeValidator`), and this task's text only ever names
 * `ControlMessageCodec`, never `HandshakeValidator`, for decoding.
 */
class SessionControlConnectionHandler(
    private val coordinator: SessionCoordinator,
    private val codec: ControlMessageCodec,
) : ControlConnectionHandler {

    override suspend fun onConnection(connection: ControlConnection) {
        val authorization = coordinator.authorize(connection.token)
        if (authorization is AdmissionDecision.Refused) {
            connection.refuse(authorization.reason.code, authorization.message)
            return
        }

        val hello = readHello(connection) ?: return
        serveAdmitted(connection, hello)
    }

    /**
     * Runs the post-authorization half of the handshake. Split out of [onConnection] so
     * neither function needs more than two exits.
     */
    private suspend fun serveAdmitted(connection: ControlConnection, hello: Hello) {
        val decision = coordinator.admit(hello)
        if (decision is AdmissionDecision.Refused) {
            connection.refuse(decision.reason.code, decision.message)
            return
        }

        val admitted = decision as AdmissionDecision.Admitted
        connection.sendText(codec.encodeSessionState(buildSessionState(admitted)))
        while (connection.receiveText() != null) {
            // The control channel carries no further inbound frames in this slice; keep
            // reading until the peer closes so onDisconnected below always runs.
        }
        coordinator.onDisconnected(DeviceId(hello.clientId), admitted.connectionId)
    }

    /**
     * Reads and decodes the peer's first frame. `null` covers both a peer that closes
     * before sending anything and a frame that fails to decode -- neither is refused,
     * since `invalid_message` is T049/T050's `HandshakeValidator` vocabulary, never
     * invoked here (T036 names only [ControlMessageCodec] for decoding).
     */
    private suspend fun readHello(connection: ControlConnection): Hello? {
        val raw = connection.receiveText() ?: return null
        return codec.decodeHello(raw).getOrNull()
    }

    /**
     * Builds the `sessionState` reply from [coordinator]'s current snapshot plus the
     * connectionId [admission] just allocated (contracts/domain-api.md's `AdmissionDecision`
     * doc: "Building it from `Admitted.connectionId` plus `SessionCoordinator.snapshot` is
     * left to the transport-facing caller"). This slice's fixed values (contracts/wire-protocol.md):
     * both slots unfilled, every connected device `connected_unassigned` with no slot, no
     * song in progress, and `connectionId` set only on this direct reply (FR-014).
     */
    private fun buildSessionState(admission: AdmissionDecision.Admitted): SessionState {
        val snapshot = coordinator.snapshot.value
        return SessionState(
            sessionId = snapshot.sessionId.value,
            slots = Slots(
                p1 = SlotDto(connected = false, deviceName = ""),
                p2 = SlotDto(connected = false, deviceName = ""),
            ),
            connectedDevices = snapshot.connected.map { device ->
                ConnectedDeviceDto(
                    clientId = device.deviceId.value,
                    displayName = device.displayName,
                    state = UNASSIGNED_SLOT_STATE,
                )
            },
            inSong = false,
            connectionId = admission.connectionId.value,
        )
    }
}
