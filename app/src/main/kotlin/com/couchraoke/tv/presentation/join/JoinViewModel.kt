package com.couchraoke.tv.presentation.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.couchraoke.tv.domain.session.SessionCoordinator
import com.couchraoke.tv.presentation.qr.QrPayloadEncoder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * T040 (contracts/domain-api.md, FR-035): presentation-only state for the join surface.
 * Performs no network I/O -- it only maps [coordinator]'s already-live
 * [SessionCoordinator.connectedDevices] flow, plus the fixed [joinCode] and [qrPayload]
 * computed once at construction, into [JoinUiState]. It never opens a socket, reaches for a
 * transport, or calls [qrEncoder] more than once.
 */
class JoinViewModel(
    private val coordinator: SessionCoordinator,
    private val qrEncoder: QrPayloadEncoder,
    private val endpoint: ControlEndpoint,
) : ViewModel() {

    private val joinCode = coordinator.snapshot.value.joinCode
    private val qrPayload = qrEncoder.encode(endpoint.address, endpoint.port, joinCode)

    /**
     * `connectedCount` is re-derived from [SessionCoordinator.connectedDevices]`.size` on
     * every emission of that flow -- never from `snapshot.roster.size`, which retains a slot
     * for a disconnected device -- so a drop is reflected the instant the coordinator projects
     * it, with no user action, refresh, or re-opening of the overlay required (FR-025, SC-007).
     *
     * `SharingStarted.Eagerly` starts that mapping the moment this view model is constructed
     * and keeps it running for [viewModelScope]'s whole lifetime, independent of whether the
     * overlay currently has a collector -- so [uiState] is already current the instant a UI
     * subscribes or resubscribes to it, rather than catching up afterwards.
     */
    val uiState: StateFlow<JoinUiState> = coordinator.connectedDevices
        .map { connected -> uiStateFor(connectedCount = connected.size) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = uiStateFor(connectedCount = coordinator.connectedDevices.value.size),
        )

    private fun uiStateFor(connectedCount: Int) = JoinUiState(
        joinCodeDisplay = joinCode.display,
        qrPayload = qrPayload,
        connectedCount = connectedCount,
    )

    /** FR-033: dismissing the overlay is presentation-only -- the session is left untouched. */
    fun onOverlayDismissed() = Unit
}
