package no.nav.helse.util.interval

import java.time.LocalDate

internal class CompositeInterval(
    interval: List<Interval?>
) : Interval() {
    override fun dag(dato: LocalDate): Dag {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val interval = interval.filterNotNull()

    override fun rapportertDato() = interval.maxBy { it.rapportertDato() }!!.rapportertDato()

    override fun flatten(): List<Dag> {
        return interval.flatMap { it.flatten() }
    }

    override fun startdato() = interval.first().startdato()

    override fun sluttdato() = interval.last().sluttdato()

    override fun antallSykedager() = interval.sumBy { it.antallSykedager() }

}
