package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class Arbeidsdag(gjelder: LocalDate) : Dag(gjelder) {

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"

    class Søknad(gjelder: LocalDate) : Arbeidsdag(gjelder) {

        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitArbeidsdag(this)
        }
    }

    class Inntektsmelding(gjelder: LocalDate) : Arbeidsdag(gjelder) {

        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitArbeidsdag(this)
        }
    }
}
