package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class KunArbeidsgiverSykedag(dato: LocalDate, override val grad: Double) : DagMedGrad, Dag(dato) {

    override fun toString() = formatter.format(dagen) + "\tKunArbeidsgiverSykedag"

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitKunArbeidsgiverSykedag(this)
    }
}
