package no.nav.helse.util.interval

import java.time.LocalDate

internal class CompositeSykdomstidslinje(
    sykdomstidslinjer: List<Sykdomstidslinje?>
) : Sykdomstidslinje() {
    override fun dag(dato: LocalDate) =
        tidslinjer.map { it.dag(dato) }.firstOrNull { it !is Nulldag } ?: Nulldag(dato, rapportertDato())

    private val tidslinjer = sykdomstidslinjer.filterNotNull()

    override fun rapportertDato() = tidslinjer.maxBy { it.rapportertDato() }!!.rapportertDato()

    override fun flatten() = tidslinjer.flatMap { it.flatten() }

    override fun startdato() = tidslinjer.first().startdato()

    override fun sluttdato() = tidslinjer.last().sluttdato()

    override fun antallSykedager() = tidslinjer.sumBy { it.antallSykedager() }

    override fun toString() = tidslinjer.joinToString(separator = "\n") { it.toString() }
}
