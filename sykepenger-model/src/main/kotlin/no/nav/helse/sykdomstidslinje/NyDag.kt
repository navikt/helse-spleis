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
}

