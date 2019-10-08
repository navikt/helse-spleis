package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor

class Ubestemtdag internal constructor(left: Dag, right: Dag): Dag(left.dagen,
    sykdomshendelse(left, right)
) {
    init { erstatter(left,right) }

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitUbestemt(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tUbestemtdag"
}

private fun sykdomshendelse(
    left: Dag,
    right: Dag
) = if (left.hendelse > right.hendelse) left.hendelse else right.hendelse
