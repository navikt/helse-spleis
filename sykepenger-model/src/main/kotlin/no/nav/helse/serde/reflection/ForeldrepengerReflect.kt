package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.Foreldrepermisjon
import no.nav.helse.hendelser.Periode
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal class ForeldrepengerReflect(foreldrepermisjon: Foreldrepermisjon) {
    private val foreldrepengeytelse: Periode? = foreldrepermisjon["foreldrepengeytelse"]
    private val svangerskapsytelse: Periode? = foreldrepermisjon["svangerskapsytelse"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "foreldrepengeytelse" to foreldrepengeytelse?.let(::periode),
        "svangerskapsytelse" to svangerskapsytelse?.let(::periode)
    )

    private fun periode(periode: Periode) = mutableMapOf<String, Any?>(
        "fom" to periode.start,
        "tom" to periode.endInclusive
    )
}
