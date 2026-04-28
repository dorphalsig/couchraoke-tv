package com.couchraoke.tv.domain.library

import kotlinx.coroutines.flow.StateFlow

interface LibraryManager {
    /**
     * Observable catalog seam used by selection flow and later runtime library refresh.
     *
     * Phase 0 may back this with fixture/static data only. Live manifest refresh,
     * disconnect removal, and multi-phone replacement behavior are outside the
     * Phase 0 implementation gate unless explicitly added later.
     */
    val songs: StateFlow<List<IndexedSong>>

    fun getSong(songId: String): IndexedSong?
}
