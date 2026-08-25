package com.couchraoke.tv.domain.session

/**
 * The three distinguishable ways [com.couchraoke.tv.di.SessionComponent.startSession] can fail
 * (FR-028, SC-008). Pure domain data -- no Android, Ktor or jmDNS type -- so `JoinViewModel` can
 * surface it as [com.couchraoke.tv.presentation.join.JoinUiState.startFailure] without leaking a
 * platform type into presentation state (FR-036).
 *
 * A session-start failure is a blocking notice, never a [GamePhase] change: [GamePhaseMachine]
 * has no `Open -> Error` edge, so none of these three cases is routed through
 * [SessionCoordinator.requestPhase].
 */
sealed interface SessionStartFailure {

    /** [com.couchraoke.tv.domain.platform.LocalAddressProvider.activeLocalIpv4] returned `null`. */
    data object NoUsableAddress : SessionStartFailure

    /** [com.couchraoke.tv.domain.control.ControlTransport.start] could not bind the control port. */
    data object BindFailed : SessionStartFailure

    /**
     * The announcer's registered instance name did not match the one requested -- jmDNS
     * renamed it on a LAN collision (contracts/ports.md's
     * [com.couchraoke.tv.domain.platform.AnnouncementHandle.registeredInstanceName]).
     */
    data object AnnouncementFailed : SessionStartFailure
}
