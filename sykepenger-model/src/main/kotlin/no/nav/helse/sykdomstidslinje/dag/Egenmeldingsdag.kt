package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class Egenmeldingsdag(gjelder: LocalDate) : GradertDag(gjelder, 100.0) {

    override fun toString() = formatter.format(dagen) + "\tEgenmeldingsdag"

    class Søknad(gjelder: LocalDate) : Egenmeldingsdag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitEgenmeldingsdag(this)
        }
    }

    class Inntektsmelding(gjelder: LocalDate) : Egenmeldingsdag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitEgenmeldingsdag(this)
        }
    }
}
