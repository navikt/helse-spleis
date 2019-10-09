package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Fylldag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse): Dag(gjelder, hendelse) {
    override fun dagType(): JsonDagType = JsonDagType.FYLLDAG

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitFylldag(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + ":Fylldag"
}
