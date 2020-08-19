package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.time.YearMonth

internal fun inntektperioder(block: Inntektperioder.() -> Unit) = Inntektperioder(block).toMap()

internal class Inntektperioder(block: Inntektperioder.() -> Unit) {
    private val map = mutableMapOf<YearMonth, MutableList<Pair<String, Inntekt>>>()

    init {
        block()
    }

    internal fun toMap(): Map<YearMonth, List<Pair<String, Inntekt>>> = map

    internal infix fun Periode.inntekter(block: Inntekter.() -> Unit) =
        this.map(YearMonth::from)
            .distinct()
            .forEach { yearMonth -> map.getOrPut(yearMonth) { mutableListOf() }.addAll(Inntekter(block).toList()) }

    internal class Inntekter(block: Inntekter.() -> Unit) {
        private val liste = mutableListOf<Pair<String, Inntekt>>()

        init {
            block()
        }

        internal fun toList() = liste.toList()

        infix fun String.inntekt(inntekt: Int) = this inntekt inntekt.månedlig
        infix fun String.inntekt(inntekt: Inntekt) = liste.add(this to inntekt)
    }
}
