package com.couchraoke.tv.domain.session

class FakeSessionGate(
    override var isLocked: Boolean = false,
    override var sessionId: String = "test-session",
    override var maxConnections: Int = 10,
) : ISessionGate
