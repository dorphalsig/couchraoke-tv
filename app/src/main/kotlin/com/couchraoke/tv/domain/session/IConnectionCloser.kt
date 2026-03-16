package com.couchraoke.tv.domain.session

fun interface IConnectionCloser {
    fun closeConnection(clientId: String)
}
