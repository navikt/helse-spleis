package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class SykHelgedag internal constructor(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse): Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykHelgedag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 1
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tSykedag helg"

    override fun dagType() = JsonDagType.SYK_HELGEDAG

    override fun nøkkel(): Nøkkel = Nøkkel.SW
}
