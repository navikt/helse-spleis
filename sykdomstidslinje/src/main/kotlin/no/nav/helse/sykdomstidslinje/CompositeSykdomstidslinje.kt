package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class CompositeSykdomstidslinje(
    sykdomstidslinjer: List<Sykdomstidslinje?>
) : Sykdomstidslinje() {

    private val tidslinjer = sykdomstidslinjer.filterNotNull()

    override fun length() = tidslinjer.sumBy { it.length() }

    override fun dag(dato: LocalDate) =
        tidslinjer.map { it.dag(dato) }.firstOrNull { it !is Nulldag } ?: Nulldag(
            dato,
            rapportertDato()
        )


    override fun rapportertDato() = tidslinjer.maxBy { it.rapportertDato() }!!.rapportertDato()

    override fun flatten() = tidslinjer.flatMap { it.flatten() }

    override fun startdato() = tidslinjer.first().startdato()

    override fun sluttdato() = tidslinjer.last().sluttdato()

    override fun antallSykeVirkedager() = tidslinjer.flatMap { it.flatten() }
        .sumBy { it.antallSykeVirkedager() }

    override fun antallSykedager() = tidslinjer.flatMap { it.flatten() }
        .sumBy { it.antallSykedager() }

    override fun toString() = tidslinjer.joinToString(separator = "\n") { it.toString() }
}
