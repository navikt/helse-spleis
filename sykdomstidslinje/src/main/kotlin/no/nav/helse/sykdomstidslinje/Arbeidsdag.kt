package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

class Arbeidsdag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse, 20) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitArbeidsdag(this)
    }

    override fun antallSykedagerMedHelg() = 0

    override fun antallSykedagerUtenHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"
}
