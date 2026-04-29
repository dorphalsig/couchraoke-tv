package com.couchraoke.tv.presentation.navigation

sealed interface AppRoute {
    val route: String

    data object SongList : AppRoute {
        override val route: String = "songList"
    }

    data object Singing : AppRoute {
        override val route: String = "singing"
    }

    data object Results : AppRoute {
        override val route: String = "results"
    }
}
