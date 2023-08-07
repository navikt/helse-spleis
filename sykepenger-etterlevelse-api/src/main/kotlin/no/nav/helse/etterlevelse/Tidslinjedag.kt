package no.nav.helse.etterlevelse

import java.time.LocalDate

class Tidslinjedag(
    private val dato: LocalDate,
    private val dagtype: String,
    private val grad: Int?
) {
    private fun hørerTil(tidslinjeperiode: Tidslinjeperiode) = tidslinjeperiode.hørerTil(dato, dagtype, grad)

    fun erRettFør(dato: LocalDate) = this.dato.plusDays(1) == dato

    fun erAvvistDag() = dagtype == "AVVISTDAG"

    companion object {
        fun List<Tidslinjedag>.dager(periode: ClosedRange<LocalDate>? = null): List<Map<String, Any?>> {
            return this
                .filter { it.dato >= (periode?.start ?: LocalDate.MIN) && it.dato <= (periode?.endInclusive ?: LocalDate.MAX) }
                .sortedBy { it.dato }
                .fold(mutableListOf<Tidslinjeperiode>()) { acc, nesteDag ->
                    if (acc.isNotEmpty() && nesteDag.hørerTil(acc.last())) {
                        acc.last().utvid(nesteDag.dato)
                    } else {
                        acc.add(Tidslinjeperiode(nesteDag.dato, nesteDag.dato, nesteDag.dagtype, nesteDag.grad))
                    }
                    acc
                }.dager()
        }
        private fun List<Tidslinjeperiode>.dager() = map {
            mapOf(
                "fom" to it.fom,
                "tom" to it.tom,
                "dagtype" to it.dagtype,
                "grad" to it.grad
            )
        }
    }

    private class Tidslinjeperiode(
        val fom: LocalDate,
        var tom: LocalDate,
        val dagtype: String,
        val grad: Int?
    ) {
        fun utvid(dato: LocalDate) {
            this.tom = dato
        }

        fun hørerTil(dato: LocalDate, dagtype: String, grad: Int?) = tom.plusDays(1) == dato && this.dagtype == dagtype && this.grad == grad
    }

}