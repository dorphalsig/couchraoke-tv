package com.couchraoke.tv.domain.session

interface ISessionCallbacks {
    fun onPhoneConnected(clientId: String, deviceName: String, connectionId: UShort)
    fun onPhoneDisconnected(clientId: String)
    fun onPhoneReconnected(clientId: String, newConnectionId: UShort)
}
