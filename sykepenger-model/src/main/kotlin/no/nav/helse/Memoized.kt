package no.nav.helse

internal class Memoized<T>(private val supplier: () -> T) {
    private val value by lazy(supplier)
    operator fun invoke() = value
}

fun <T> (() -> T).memoized(): () -> T = Memoized(this)::invoke
fun <T> T.memoized(): () -> T = Memoized { this }::invoke
