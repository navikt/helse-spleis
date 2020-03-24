package no.nav.helse.sykdomstidslinje.dag
import no.nav.helse.person.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal sealed class Permisjonsdag(gjelder: LocalDate) : Dag(gjelder) {
    override fun toString() = formatter.format(dagen) + "\tPermisjonsdag"

    class Søknad(gjelder: LocalDate) : Permisjonsdag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitPermisjonsdag(this)
        }

        override fun accept(visitor: NySykdomstidslinjeVisitor) {
            visitor.visitPermisjonsdag(this)
        }
    }

    class Aareg(gjelder: LocalDate) : Permisjonsdag(gjelder) {
        override fun accept(visitor: SykdomstidslinjeVisitor) {
            visitor.visitPermisjonsdag(this)
        }

        override fun accept(visitor: NySykdomstidslinjeVisitor) {
            visitor.visitPermisjonsdag(this)
        }
    }
}
