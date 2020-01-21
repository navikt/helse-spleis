package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.SykdomshistorikkVisitor
import java.time.LocalDateTime

internal class Sykdomshistorikk {
    private val elementer = mutableListOf<Element>()
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

    internal inner class Element (private val hendelse: SykdomstidslinjeHendelse) {
        internal val tidsstempel = LocalDateTime.now()
        internal val hendelseSykdomstidslinje = hendelse.sykdomstidslinje()
        internal val beregnetSykdomstidslinje: ConcreteSykdomstidslinje =
            if (elementer.isEmpty()) hendelseSykdomstidslinje
            else {
                (sykdomstidslinje() + hendelseSykdomstidslinje).also {
                    if (it.erUtenforOmfang()) hendelse.error("Ikke støttet dag")
                }
            }

        fun accept(visitor: SykdomshistorikkVisitor) {
            visitor.preVisitSykdomshistorikkElement(this)
            visitor.visitHendelse(hendelse)
            hendelseSykdomstidslinje.accept(visitor)
            beregnetSykdomstidslinje.accept(visitor)
            visitor.postVisitSykdomshistorikkElement(this)
        }

    }
}
