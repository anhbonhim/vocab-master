package com.nhimz.vocabmaster.data.database

/**
 * Count of FSRS cards in a given [State].
 *
 * Relocated to its own file during the split-database refactor (T06) when the legacy
 * `VocabDao` was deleted; [UserDataDao.getStateCounts] returns these from the per-user
 * [UserDataDatabase].
 */
data class StateCount(val state: Int, val count: Int)
