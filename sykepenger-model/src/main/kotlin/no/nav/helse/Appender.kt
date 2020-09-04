package no.nav.helse

interface Appender
interface AppenderFeature<A : Any, B : Appender> {
    fun append(a: A, appender: B.() -> Unit)
}

fun <A : Any, B : Appender, BF : AppenderFeature<A, B>> A.appender(feature: BF, appender: B.() -> Unit) =
    this.apply { feature.append(this, appender) }
