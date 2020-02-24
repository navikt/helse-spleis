package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class Sykedag(gjelder: LocalDate) : Dag(gjelder) {

    override fun toString() = formatter.format(dagen) + "\tSykedag"

    class Sykmelding(gjelder: LocalDate) : Sykedag(gjelder) {

        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykedag(this)
        }
    }

    class Søknad(gjelder: LocalDate) : Sykedag(gjelder) {

        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitSykedag(this)
        }
    }
}
