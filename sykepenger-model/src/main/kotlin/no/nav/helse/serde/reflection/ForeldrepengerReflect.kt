package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelForeldrepenger
import no.nav.helse.hendelser.Periode

internal class ForeldrepengerReflect(foreldrepenger: ModelForeldrepenger) {
    private val foreldrepengeytelse: Periode? = foreldrepenger["foreldrepengeytelse"]
    private val svangerskapsytelse: Periode? = foreldrepenger["svangerskapsytelse"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "foreldrepengeytelse" to foreldrepengeytelse?.let(::periode),
        "svangerskapsytelse" to svangerskapsytelse?.let(::periode)
    )

    private fun periode(periode: Periode) = mutableMapOf<String, Any?>(
        "fom" to periode.start,
        "tom" to periode.endInclusive
    )
}
