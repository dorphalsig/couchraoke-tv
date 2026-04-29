package com.couchraoke.tv.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test(timeout = 30_000)
    fun routeContractsIncludeSongListSingingAndInertResultsOnly() {
        val routes: List<AppRoute> = listOf(
            AppRoute.SongList,
            AppRoute.Singing,
            AppRoute.Results,
        )

        assertEquals(listOf("songList", "singing", "results"), routes.map { it.route })
    }
}
