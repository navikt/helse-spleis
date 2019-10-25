package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Utenlandsdag internal constructor(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) : Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitUtenlandsdag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 0
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tUtenlandsdag"

    override fun dagType() = JsonDagType.UTENLANDSDAG

    override fun nøkkel(): Nøkkel = Nøkkel.DA
}
