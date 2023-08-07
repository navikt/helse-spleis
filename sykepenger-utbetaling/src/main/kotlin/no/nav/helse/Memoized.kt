package no.nav.helse

import java.util.concurrent.ConcurrentHashMap

class Memoized<T>(private val supplier: () -> T) {
    private val value by lazy(supplier)
    operator fun invoke() = value
}

fun <T> (() -> T).memoized(): () -> T = Memoized(this)::invoke
fun <T> T.memoized(): () -> T = Memoized { this }::invoke

fun <P, R> ((P) -> R).memoize(): (P) -> R = object : (P) -> R {
    private val map = ConcurrentHashMap<P, R>()
    override fun invoke(parameter: P) = map[parameter] ?: run {
        this@memoize(parameter).also { map.putIfAbsent(parameter, it) }
    }
}
