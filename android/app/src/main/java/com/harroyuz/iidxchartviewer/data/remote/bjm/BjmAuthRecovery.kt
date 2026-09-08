package com.harroyuz.iidxchartviewer.data.remote.bjm

/** A single recovery attempt, only for authentication errors; never clears credentials. */
internal fun <T> withBjmAuthRecovery(request: () -> T, status: (T) -> Int, refresh: () -> Unit): T {
    val first = request()
    if (status(first) != 401 && status(first) != 403) return first
    refresh()
    return request()
}
