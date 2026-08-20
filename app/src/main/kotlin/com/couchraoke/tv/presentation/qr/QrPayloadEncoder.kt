package com.couchraoke.tv.presentation.qr

import com.couchraoke.tv.domain.session.model.JoinCode
import java.net.Inet4Address

/**
 * T038 (contracts/domain-api.md): pure string construction of the control endpoint URL a
 * phone dials to join. No ZXing type appears in this signature so this object stays
 * JVM-testable and `--src`-selected for the coverage gate; rendering the payload to pixels is
 * [com.couchraoke.tv.presentation.qr.QrBitmapRenderer]'s job (T039).
 */
object QrPayloadEncoder {

    /**
     * Returns exactly `ws://<address>:<port>/?token=<CODE>` -- the full control endpoint URL
     * with the join code as the `token` query parameter, and nothing else. It deliberately
     * encodes no discovery-service identifier such as an mDNS service type or instance name
     * (FR-007).
     */
    fun encode(address: Inet4Address, port: Int, joinCode: JoinCode): String =
        "ws://${address.hostAddress}:$port/?token=${joinCode.display}"
}
