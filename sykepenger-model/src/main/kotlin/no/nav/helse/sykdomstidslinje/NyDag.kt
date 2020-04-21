package no.nav.helse.sykdomstidslinje

import java.time.LocalDate

internal sealed class NyDag(private val dato: LocalDate) {
    internal class NyUkjentDag(dato: LocalDate) : NyDag(dato)
    internal class NyArbeidsdag(dato: LocalDate) : NyDag(dato)
    internal class NyArbeidsgiverdag(dato: LocalDate, private val grad: Number = 100.0) : NyDag(dato)
    internal class NyFeriedag(dato: LocalDate) : NyDag(dato)
    internal class NyFriskHelgedag(dato: LocalDate) : NyDag(dato)
    internal class NyArbeidsgiverHelgedag(dato: LocalDate) : NyDag(dato)
    internal class NySykedag(dato: LocalDate, private val grad: Number = 100.0) : NyDag(dato)
    internal class NySykHelgedag(dato: LocalDate) : NyDag(dato)
    internal class ProblemDag(dato: LocalDate) : NyDag(dato)

    override fun equals(other: Any?) =
        other != null && this::class == other::class && this.equals(other as NyDag)

    protected open fun equals(other: NyDag) = this.dato == other.dato

    override fun hashCode() = dato.hashCode() * 37 + this::class.hashCode()

    companion object {
        internal val default: BesteStrategy = { venstre: NyDag, høyre: NyDag ->
            require(venstre.dato == høyre.dato) { "Støtter kun sammenlikning av dager med samme dato"}
            if (venstre == høyre) venstre else ProblemDag(venstre.dato)
        }
    }
}

internal typealias BesteStrategy = (NyDag, NyDag) -> NyDag
