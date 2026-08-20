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
 * then read the peer's first frame, validate it with [validator], call
 * `coordinator.admit(hello)`, send the `sessionState` reply on acceptance, and route the
 * socket close to `coordinator.onDisconnected`.
 *
 * The token is checked *before* the first frame is read, not after: it arrives on the
 * connect URL (FR-009), so nothing about it depends on `hello`, and refusing up front means
 * an unauthorized peer can never hold the handler waiting on a frame it may never send.
 *
 * A refusal from any step is delivered through [ControlConnection.refuse], which sends
 * the `error` frame and closes in one operation — splitting those is what produces the
 * peer's exit 4 (FR-016/FR-017).
 *
 * [validator] (T050, wiring T049's [HandshakeValidator] in) now guards the raw first frame:
 * a frame that fails to parse, carries the wrong `type`/`protocolVersion`, or is missing or
 * out of range on a required field is refused here with the validator's own [RefusalReason]
 * and message, replacing the previous silent drop. [ControlMessageCodec.decodeHello] is no
 * longer used for this step, because [HandshakeValidator.validate]'s `Valid` case already
 * hands back the decoded [Hello] -- [codec] is still needed for [buildSessionState]'s
 * `sessionState` reply. A peer that closes *before sending anything* is left untouched: that
 * is the 5-second handshake deadline (T052), a different case from a validation failure, and
 * out of this task's scope.
 *
 * `admit`'s `RosterAdmission.Reclaimed` branch (T057/T058) is deliberately left to throw
 * rather than being normalised away: it only fires for the reclaim scenario this slice's
 * US1/US2 path never exercises, so letting it surface loudly is preferable to a silent
 * fallthrough. `admit`'s `session_full` refusal (`RosterAdmission.AtCapacity`, T050) is
 * completed in [SessionCoordinator.admit] and reaches [ControlConnection.refuse] the same
 * way as every other refusal above.
 */
class SessionControlConnectionHandler(
    private val coordinator: SessionCoordinator,
    private val codec: ControlMessageCodec,
    private val validator: HandshakeValidator = HandshakeValidator(),
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
     * Reads the peer's first frame and validates it with [validator]. Returns `null` for a
     * peer that closes before sending anything -- a different case from a validation
     * failure, and the 5-second handshake deadline (T052) this handler does not enforce --
     * left un-refused exactly as before. A frame [validator] rejects is refused here with
     * its own [RefusalReason] and message before this returns `null`, so [onConnection] does
     * nothing further. A frame [validator] accepts hands back its already-decoded [Hello]
     * directly, so no separate [ControlMessageCodec] decode step is needed here.
     */
    private suspend fun readHello(connection: ControlConnection): Hello? {
        val raw = connection.receiveText() ?: return null
        return when (val validation = validator.validate(raw)) {
            is HelloValidation.Valid -> validation.hello
            is HelloValidation.Invalid -> {
                connection.refuse(validation.reason.code, validation.message)
                null
            }
        }
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
