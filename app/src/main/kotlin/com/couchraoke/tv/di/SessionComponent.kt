package com.couchraoke.tv.di

import com.couchraoke.tv.domain.control.ControlTransport
import com.couchraoke.tv.domain.control.HandshakeValidator
import com.couchraoke.tv.domain.control.JoinCodeMatcher
import com.couchraoke.tv.domain.platform.LocalAddressProvider
import com.couchraoke.tv.domain.platform.MulticastLease
import com.couchraoke.tv.domain.platform.SessionAnnouncer
import com.couchraoke.tv.domain.session.ConnectionIdAllocator
import com.couchraoke.tv.domain.session.GamePhaseMachine
import com.couchraoke.tv.domain.session.JoinCodeGenerator
import com.couchraoke.tv.domain.session.SessionCoordinator
import com.couchraoke.tv.domain.session.SessionRoster
import com.couchraoke.tv.domain.session.model.SessionId
import java.util.UUID

/**
 * Manual composition root for one session (contracts/ports.md). No DI framework (FR-037).
 *
 * Mints the [SessionId] here — a random [UUID] string, which is opaque, non-empty and
 * practically unique per call (FR-001). No other task owns `SessionId` creation;
 * [SessionCoordinator] only ever receives one.
 *
 * [transport], [announcer], [addressProvider], [multicastLease] and [clock] are the four
 * injected ports plus the wall clock from contracts/ports.md's binding constructor. This
 * phase (T023) only builds the coordinator; T036 wires session start — resolving the
 * active address, acquiring the multicast lease, starting the transport and publishing the
 * announcement — using those same four ports. Each is `@Suppress`ed below because the
 * binding constructor (ports.md L134-141) requires storing them now, even though T036 is
 * the first task to read them.
 */
class SessionComponent(
    @Suppress("UnusedPrivateProperty") private val transport: ControlTransport,
    @Suppress("UnusedPrivateProperty") private val announcer: SessionAnnouncer,
    @Suppress("UnusedPrivateProperty") private val addressProvider: LocalAddressProvider,
    @Suppress("UnusedPrivateProperty") private val multicastLease: MulticastLease,
    private val joinCodeGenerator: JoinCodeGenerator,
    @Suppress("UnusedPrivateProperty") private val clock: () -> Long,
) {

    fun createCoordinator(): SessionCoordinator = SessionCoordinator(
        roster = SessionRoster(),
        phaseMachine = GamePhaseMachine(),
        connectionIds = ConnectionIdAllocator(),
        validator = HandshakeValidator(),
        codeMatcher = JoinCodeMatcher,
        sessionId = SessionId(UUID.randomUUID().toString()),
        joinCode = joinCodeGenerator.next(),
    )
}
