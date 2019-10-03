package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

class Arbeidsdag internal constructor(gjelder: LocalDate, hendelse: KildeHendelse): Dag(gjelder, hendelse, 20) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitArbeidsdag(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"
}
