package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal class Arbeidsdag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse, 20) {
    override fun antallSykeVirkedager() = 0

    override fun antallSykedager() = 0

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"
}
