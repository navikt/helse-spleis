package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.JsonDag
import java.time.LocalDate

internal open class CompositeSykdomstidslinje internal constructor(
    tidslinjer: List<ConcreteSykdomstidslinje>
) : ConcreteSykdomstidslinje() {

    init {
        assert(tidslinjer.isNotEmpty()) { "En tom tidslinje skal representeres med null og ikke en tom liste" }
    }

    internal val tidslinje = tidslinjer.flatMap { it.flatten() }

    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.preVisitComposite(this)
        tidslinje.forEach { it.accept(visitor) }
        visitor.postVisitComposite(this)
    }

    override fun sisteHendelse() = tidslinje.map { it.sisteHendelse() }.maxBy { it.rapportertdato() }!!

    override fun length() = tidslinje.size

    override fun dag(dato: LocalDate) =
        tidslinje.find { it.dagen == dato }

    override fun hendelser() = tidslinje.flatMapTo(mutableSetOf()) { it.hendelser() }

    override fun flatten() = tidslinje

    override fun førsteDag() = tidslinje.first().dagen

    override fun sisteDag() = tidslinje.last().dagen

    override fun toString() = tidslinje.joinToString(separator = "\n") { it.toString() }

    companion object {
        internal fun fromJsonRepresentation(
            jsonDager: List<JsonDag>,
            hendelseMap: Map<String, SykdomstidslinjeHendelse>
        ): CompositeSykdomstidslinje {
            return CompositeSykdomstidslinje(jsonDager.map { Dag.fromJsonRepresentation(it, hendelseMap) })
        }
    }
}
