package no.nav.helse

interface Builder
interface BuilderFeature<A : Any, B : Builder> {
    fun build(builder: B.() -> Unit): A
}

fun <A : Any, B : Builder, BF : BuilderFeature<A, B>> builder(feature: BF, builder: B.() -> Unit) =
    feature.build(builder)

interface Appender
interface AppenderFeature<A : Any, B : Appender> {
    fun append(a: A, appender: B.() -> Unit)
}

fun <A : Any, B : Appender, BF : AppenderFeature<A, B>> A.appender(feature: BF, appender: B.() -> Unit) =
    this.apply { feature.append(this, appender) }
