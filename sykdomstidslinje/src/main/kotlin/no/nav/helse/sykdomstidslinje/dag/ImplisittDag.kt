package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class ImplisittDag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse) :
    Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitImplisittDag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun antallSykedagerHvorViIkkeTellerMedHelg(): Int = 0

    override fun toString() = formatter.format(dagen) + "\tImplisitt dag"

    override fun dagType(): JsonDagType = JsonDagType.IMPLISITT_DAG

    override fun nøkkel(): Nøkkel = Nøkkel.I
}
