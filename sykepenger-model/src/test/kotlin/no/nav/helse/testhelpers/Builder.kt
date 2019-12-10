package no.nav.helse.testhelpers

internal fun <Type, Builder> build(
    buildertype: Buildertype<Type, Builder>,
    block: Builder.() -> Unit
) = buildertype.build(block)

internal interface Buildertype<Type, Builder> {
    fun build(block: Builder.() -> Unit): Type
}
