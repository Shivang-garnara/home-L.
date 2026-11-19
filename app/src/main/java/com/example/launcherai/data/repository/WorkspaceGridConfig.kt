package com.example.launcherai.data.repository

/**
 * Persisted grid configuration for workspace pages (columns x rows).
 */
data class WorkspaceGridConfig(
    val cols: Int = DEFAULT_COLS,
    val rows: Int = DEFAULT_ROWS
) {
    companion object {
        const val DEFAULT_COLS = 4
        const val DEFAULT_ROWS = 5
        const val MIN_COLS = 3
        const val MAX_COLS = 7
        const val MIN_ROWS = 3
        const val MAX_ROWS = 8
    }
}

/**
 * Result of attempting to modify the workspace grid configuration.
 */
sealed class GridChangeResult {
    object Success : GridChangeResult()
    data class Failure(val reason: String) : GridChangeResult()
}
