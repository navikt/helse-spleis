package no.nav.helse

inline fun <T, R> Iterable<T>.mapWithNext(transform: (nåværende: T, neste: T?) -> R): List<R> {
    if (!iterator().hasNext()) return emptyList()
    return zipWithNext(transform) + transform(last(), null)
}
