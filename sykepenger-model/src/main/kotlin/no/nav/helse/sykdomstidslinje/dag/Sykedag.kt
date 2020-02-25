package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class Sykedag(gjelder: LocalDate, val grad: Double) : Dag(gjelder) {

    override fun toString() = formatter.format(dagen) + "\tSykedag"

    class Sykmelding(gjelder: LocalDate, grad: Double) : Sykedag(gjelder, grad) {

        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykedag(this)
        }
    }

    class Søknad(gjelder: LocalDate, grad: Double) : Sykedag(gjelder, grad) {

        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykedag(this)
        }
    }
}
