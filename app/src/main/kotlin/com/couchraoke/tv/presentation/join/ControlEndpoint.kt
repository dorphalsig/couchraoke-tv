package com.couchraoke.tv.presentation.join

import java.net.Inet4Address

/**
 * The resolved local IPv4 address paired with the control server's actual bound port
 * (contracts/domain-api.md). Lets [JoinViewModel] hand
 * [com.couchraoke.tv.presentation.qr.QrPayloadEncoder] the endpoint a phone should dial
 * without reaching for a port or a socket itself (FR-035).
 *
 * Not restated in any task of `tasks.md`; [JoinViewModel]'s binding constructor
 * (contracts/domain-api.md) names this type without any task creating it. T040 owns
 * constructing it, per the contract's own note.
 */
data class ControlEndpoint(val address: Inet4Address, val port: Int)
