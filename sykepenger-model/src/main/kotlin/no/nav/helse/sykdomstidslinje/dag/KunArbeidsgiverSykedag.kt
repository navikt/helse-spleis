package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class KunArbeidsgiverSykedag(dato: LocalDate, grad: Double) : GradertDag(dato, grad) {

    override fun toString() = formatter.format(dagen) + "\tKunArbeidsgiverSykedag ($grad %)"

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitKunArbeidsgiverSykedag(this)
    }

    override fun accept(visitor: NySykdomstidslinjeVisitor) {
        visitor.visitKunArbeidsgiverSykedag(this)
    }
}
