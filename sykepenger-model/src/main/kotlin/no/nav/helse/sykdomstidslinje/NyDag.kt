package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.NySykdomstidslinjeVisitor
import java.time.LocalDate

internal typealias BesteStrategy = (NyDag, NyDag) -> NyDag

internal sealed class NyDag(
    protected val dato: LocalDate,
    protected val kilde: SykdomstidslinjeHendelse.Hendelseskilde
) {
    companion object {
        internal val default: BesteStrategy = { venstre: NyDag, høyre: NyDag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            if (venstre == høyre) venstre else ProblemDag(høyre.dato, høyre.kilde)
        }

        internal val noOverlap: BesteStrategy = { venstre: NyDag, høyre: NyDag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato" }
            ProblemDag(høyre.dato, høyre.kilde)
        }
    }

    internal fun problem(): NyDag = ProblemDag(dato, kilde)

    override fun equals(other: Any?) =
        other != null && this::class == other::class && this.equals(other as NyDag)

    protected open fun equals(other: NyDag) = this.dato == other.dato

    override fun hashCode() = dato.hashCode() * 37 + this::class.hashCode()

    internal abstract fun accept(visitor: NySykdomstidslinjeVisitor)

    internal class NyUkjentDag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NyArbeidsdag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NyArbeidsgiverdag(
        dato: LocalDate,
        private val grad: Grad = Grad.sykdom(100),
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        internal constructor(
            dato: LocalDate,
            grad: Number = 100,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) : this(dato, Grad.sykdom(grad), kilde)

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, grad, kilde)
    }

    internal class NyFeriedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NyFriskHelgedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NyArbeidsgiverHelgedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NySykedag(
        dato: LocalDate,
        private val grad: Grad = Grad.sykdom(100),
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        internal constructor(
            dato: LocalDate,
            grad: Number = 100,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) : this(dato, Grad.sykdom(grad), kilde)

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, grad, kilde)
    }

    internal class NyKunArbeidsgiverdag(
        dato: LocalDate,
        private val grad: Grad = Grad.sykdom(100),
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        internal constructor(
            dato: LocalDate,
            grad: Number = 100,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) : this(dato, Grad.sykdom(grad), kilde)

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, grad, kilde)
    }

    internal class NySykHelgedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NyPermisjonsdag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NyStudiedag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class NyUtenlandsdag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

    internal class ProblemDag(
        dato: LocalDate,
        kilde: SykdomstidslinjeHendelse.Hendelseskilde
    ) : NyDag(dato, kilde) {

        override fun accept(visitor: NySykdomstidslinjeVisitor) =
            visitor.visitDag(this, dato, kilde)
    }

}

