package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.tournament.historiskDagturnering
import java.time.LocalDateTime

internal class Sykdomshistorikk private constructor(
    private val elementer: MutableList<Element>
) {
    internal constructor() : this(mutableListOf())

    internal val size get() = elementer.size
    internal fun isEmpty() = elementer.isEmpty()

    internal fun   sykdomstidslinje() = elementer.first().beregnetSykdomstidslinje

    internal fun håndter(hendelse: SykdomstidslinjeHendelse) {
        elementer.add(0, Element.opprett(this, hendelse))
    }

    internal fun accept(visitor: SykdomshistorikkVisitor) {
        visitor.preVisitSykdomshistorikk(this)
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitSykdomshistorikk(this)
    }

    private fun kalkulerBeregnetSykdomstidslinje(
        hendelse: SykdomstidslinjeHendelse,
        hendelseSykdomstidslinje: ConcreteSykdomstidslinje
    ) = if (elementer.isEmpty()) {
        hendelse.sykdomstidslinje()
    } else {
        sykdomstidslinje().plus(hendelseSykdomstidslinje, ConcreteSykdomstidslinje.Companion::implisittDag, historiskDagturnering).also {
            if (it.erUtenforOmfang()) hendelse.error("Ikke støttet dag")
        }
    }

    internal class Element private constructor(
        internal val tidsstempel: LocalDateTime,
        private val hendelseSykdomstidslinje: ConcreteSykdomstidslinje,
        internal val beregnetSykdomstidslinje: ConcreteSykdomstidslinje,
        private val hendelse: SykdomstidslinjeHendelse
    ) {
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

        companion object {
            fun opprett(
                historikk: Sykdomshistorikk,
                hendelse: SykdomstidslinjeHendelse
            ): Element {
                val hendelseSykdomstidslinje = hendelse.sykdomstidslinje()
                return Element(
                    tidsstempel = LocalDateTime.now(),
                    hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                    beregnetSykdomstidslinje = historikk.kalkulerBeregnetSykdomstidslinje(
                        hendelse,
                        hendelseSykdomstidslinje
                    ),
                    hendelse = hendelse
                )
            }
        }
    }
}
