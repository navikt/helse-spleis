package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Ubestemtdag internal constructor(dato: LocalDate, hendelse: SykdomstidslinjeHendelse): Dag(dato, hendelse) {
    internal constructor(left: Dag, right: Dag) : this(left.dagen, sykdomshendelse(left, right)) {
        erstatter(left,right)
    }

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitUbestemt(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tUbestemtdag"

    override fun dagType() = JsonDagType.UBESTEMTDAG

    override fun nøkkel(): Nøkkel = Nøkkel.Undecided
}

private fun sykdomshendelse(
    left: Dag,
    right: Dag
) = if (left.hendelse > right.hendelse) left.hendelse else right.hendelse
