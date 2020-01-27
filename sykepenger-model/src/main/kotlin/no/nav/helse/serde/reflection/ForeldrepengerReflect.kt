package no.nav.helse.serde.reflection

import no.nav.helse.hendelser.ModelForeldrepenger
import no.nav.helse.hendelser.Periode

internal class ForeldrepengerReflect(foreldrepenger: ModelForeldrepenger) {
    private val foreldrepengeytelse: Periode? = foreldrepenger.getProp("foreldrepengeytelse")
    private val svangerskapsytelse: Periode? = foreldrepenger.getProp("svangerskapsytelse")

    internal fun toMap() = mutableMapOf<String, Any?>()
}
