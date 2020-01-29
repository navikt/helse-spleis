package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.SykdomshistorikkVisitor
import java.time.LocalDateTime

internal class Sykdomshistorikk private constructor(
    private val elementer: MutableList<Element>
) {
    internal constructor() : this(mutableListOf())

    internal val size get() = elementer.size

    internal fun sykdomstidslinje() = elementer.first().beregnetSykdomstidslinje

    internal fun håndter(hendelse: SykdomstidslinjeHendelse) {
        elementer.add(0, Element(hendelse))
    }

    fun accept(visitor: SykdomshistorikkVisitor) {
        visitor.preVisitSykdomshistorikk(this)
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitSykdomshistorikk(this)
    }

    fun kalkulerBeregnetSykdomstidslinje(
        hendelse: SykdomstidslinjeHendelse,
        hendelseSykdomstidslinje: ConcreteSykdomstidslinje
    ) = if (elementer.isEmpty()) {
        hendelse.sykdomstidslinje()
    } else {
        (sykdomstidslinje() + hendelseSykdomstidslinje).also {
            if (it.erUtenforOmfang()) hendelse.error("Ikke støttet dag")
        }
    }

    internal inner class Element private constructor(
        internal val tidsstempel: LocalDateTime,
        private val hendelseSykdomstidslinje: ConcreteSykdomstidslinje,
        internal val beregnetSykdomstidslinje: ConcreteSykdomstidslinje,
        private val hendelse: SykdomstidslinjeHendelse
    ) {
        private constructor(
            hendelse: SykdomstidslinjeHendelse,
            hendelseSykdomstidslinje: ConcreteSykdomstidslinje
        ) : this(
            tidsstempel = LocalDateTime.now(),
            hendelseSykdomstidslinje = hendelseSykdomstidslinje,
            beregnetSykdomstidslinje = kalkulerBeregnetSykdomstidslinje(hendelse, hendelseSykdomstidslinje),
            hendelse = hendelse
        )
        internal constructor(hendelse: SykdomstidslinjeHendelse) : this(hendelse, hendelse.sykdomstidslinje())


        fun accept(visitor: SykdomshistorikkVisitor) {
            visitor.preVisitSykdomshistorikkElement(this)
            visitor.visitHendelse(hendelse)
            visitor.preVisitHendelseSykdomstidslinje()
            hendelseSykdomstidslinje.accept(visitor)
            visitor.postVisitHendelseSykdomstidslinje()
            visitor.preVisitBeregnetSykdomstidslinje()
            beregnetSykdomstidslinje.accept(visitor)
            visitor.postVisitBeregnetSykdomstidslinje()
            visitor.postVisitSykdomshistorikkElement(this)
        }
    }
}
