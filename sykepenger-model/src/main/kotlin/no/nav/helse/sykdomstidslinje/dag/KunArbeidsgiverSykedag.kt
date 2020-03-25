package no.nav.helse.sykdomstidslinje.dag
import no.nav.helse.person.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class KunArbeidsgiverSykedag(dato: LocalDate, grad: Double) : GradertDag(dato, grad) {

    override fun toString() = formatter.format(dagen) + "\tKunArbeidsgiverSykedag ($grad %)"

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitKunArbeidsgiverSykedag(this)
    }
}
