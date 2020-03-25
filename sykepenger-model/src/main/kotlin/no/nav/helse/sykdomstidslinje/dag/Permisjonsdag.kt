package no.nav.helse.sykdomstidslinje.dag
import no.nav.helse.person.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class Permisjonsdag(gjelder: LocalDate) : Dag(gjelder) {
    override fun toString() = formatter.format(dagen) + "\tPermisjonsdag"

    class Søknad(gjelder: LocalDate) : Permisjonsdag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitPermisjonsdag(this)
        }
    }

    class Aareg(gjelder: LocalDate) : Permisjonsdag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitPermisjonsdag(this)
        }
    }
}
