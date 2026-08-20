package com.couchraoke.tv.domain.control

import com.couchraoke.tv.domain.control.model.ConnectedDeviceDto
import com.couchraoke.tv.domain.control.model.Hello
import com.couchraoke.tv.domain.control.model.SessionState
import com.couchraoke.tv.domain.control.model.SlotDto
import com.couchraoke.tv.domain.control.model.Slots
import com.couchraoke.tv.domain.session.SessionCoordinator
import com.couchraoke.tv.domain.session.model.DeviceId
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private const val UNASSIGNED_SLOT_STATE = "connected_unassigned"

/** FR-017 / research.md R6: a pending connection must introduce itself within this window. */
private val HANDSHAKE_DEADLINE = 5.seconds
private const val HANDSHAKE_TIMEOUT_MESSAGE = "No introduction received within 5 seconds"

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
 * `sessionState` reply. A peer that closes *before sending anything* is a different case
 * from a peer that stays connected but silent: only the latter trips the 5-second handshake
 * deadline (T052, FR-017, research.md R6) enforced by [readFirstFrame] below.
 *
 * FR-018's during/after asymmetry (T053) was already structurally correct before T053: an
 * unexpected message `type` during the handshake is fatal because [HandshakeValidator]
 * refuses it before [Hello] ever exists, ending [onConnection] on that refusal; an
 * unrecognised message *after* admission is drained by [serveAdmitted]'s read loop without
 * inspection and never closes the connection. What T053 actually adds is test coverage
 * pinning both halves -- see `SessionControlConnectionHandlerTest`'s second test class.
 * The FR-018 "logged as a warning" half is not implemented: there is no logging facility
 * anywhere in `«main»`, and this class lives in `domain.control`, which contracts/domain-api.md
 * requires to stay pure Kotlin with no Android types, so adding `android.util.Log` here would
 * both invent a dependency and violate that boundary. Reported as an open gap rather than
 * fixed; see this unit's report for a suggested `Logger` port.
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
            // FR-018 (T053): once admitted, any inbound frame's type is unrecognised in this
            // slice's protocol and is ignored without inspection -- it is never treated as
            // fatal, so the loop keeps reading and the connection stays open until the peer
            // itself closes it, at which point onDisconnected below runs.
        }
        coordinator.onDisconnected(DeviceId(hello.clientId), admitted.connectionId)
    }

    /**
     * Reads the peer's first frame and validates it with [validator]. Returns `null` for a
     * peer that closes before sending anything -- a different case from the 5-second
     * handshake deadline enforced by [readFirstFrame]. A frame [validator] rejects is
     * refused here with its own [RefusalReason] and message before this returns `null`, so
     * [onConnection] does nothing further -- this is FR-018's "during the handshake" half:
     * an unexpected `type` is fatal because it never produces a [Hello]. A frame [validator]
     * accepts hands back its already-decoded [Hello] directly, so no separate
     * [ControlMessageCodec] decode step is needed here.
     */
    private suspend fun readHello(connection: ControlConnection): Hello? {
        val raw = readFirstFrame(connection) ?: return null
        return when (val validation = validator.validate(raw)) {
            is HelloValidation.Valid -> validation.hello
            is HelloValidation.Invalid -> {
                connection.refuse(validation.reason.code, validation.message)
                null
            }
        }
    }

    /**
     * Waits for the peer's first frame under the FR-017 5-second handshake deadline
     * (research.md R6). This connection has already passed [coordinator]'s token check and
     * is pending -- it holds no roster slot and appears in no list -- so exactly one of two
     * distinct `null` outcomes is refused:
     * - the peer closes *before sending anything*: [ControlConnection.receiveText] completes
     *   with `null` inside the deadline, and this is left un-refused, matching this
     *   handler's original (pre-T052) behaviour for that case;
     * - the peer stays connected but silent past the deadline: [withTimeoutOrNull] cancels
     *   the read, and only this branch calls [ControlConnection.refuse] with
     *   `invalid_message`, because a pending connection that never introduces itself must
     *   not be left open indefinitely.
     *
     * [FirstFrame] boxes the nullable [ControlConnection.receiveText] result so that
     * [withTimeoutOrNull] returning `null` can only mean "timed out" -- without the wrapper,
     * a same-typed `null` from a peer that closed within the deadline would be
     * indistinguishable from a timeout, since both are `String?`.
     */
    private suspend fun readFirstFrame(connection: ControlConnection): String? {
        val frame = withTimeoutOrNull(HANDSHAKE_DEADLINE) { FirstFrame(connection.receiveText()) }
        if (frame == null) {
            connection.refuse(RefusalReason.INVALID_MESSAGE.code, HANDSHAKE_TIMEOUT_MESSAGE)
            return null
        }
        return frame.raw
    }

    /** Non-null box around a nullable first-frame read; see [readFirstFrame]. */
    private data class FirstFrame(val raw: String?)

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
