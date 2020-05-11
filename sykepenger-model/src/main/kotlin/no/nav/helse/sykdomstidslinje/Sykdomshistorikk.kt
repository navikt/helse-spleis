package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.tournament.dagturnering
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Sykdomshistorikk private constructor(
    private val elementer: MutableList<Element>
) {
    internal constructor() : this(mutableListOf())

    internal val size get() = elementer.size

    internal fun isEmpty() = elementer.isEmpty()

    internal fun sykdomstidslinje() = Element.nySykdomstidslinje(elementer)

    internal fun håndter(hendelse: SykdomstidslinjeHendelse) {
        elementer.add(
            0, Element.opprett(
                this,
                hendelse,
                if (elementer.isEmpty()) LocalDate.MAX else sykdomstidslinje().sisteDag()
            )
        )
    }

    internal fun accept(visitor: SykdomshistorikkVisitor) {
        visitor.preVisitSykdomshistorikk(this)
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitSykdomshistorikk(this)
    }

    private fun kalkulerBeregnetSykdomstidslinje(
        hendelse: SykdomstidslinjeHendelse,
        hendelseSykdomstidslinje: NySykdomstidslinje
    ): NySykdomstidslinje {
        val tidslinje = if (elementer.isEmpty())
            hendelseSykdomstidslinje
        else
            sykdomstidslinje().merge(hendelseSykdomstidslinje, dagturnering::beste)
        return tidslinje.also { it.valider(hendelse) }
    }

    internal class Element private constructor(
        private val hendelseId: UUID,
        private val tidsstempel: LocalDateTime,
        private val nyHendelseSykdomstidslinje: NySykdomstidslinje,
        private val nyBeregnetSykdomstidslinje: NySykdomstidslinje
    ) {
        fun accept(visitor: SykdomshistorikkVisitor) {
            visitor.preVisitSykdomshistorikkElement(this, hendelseId, tidsstempel)
            visitor.preVisitHendelseSykdomstidslinje(nyHendelseSykdomstidslinje)
            nyHendelseSykdomstidslinje.accept(visitor)
            visitor.postVisitHendelseSykdomstidslinje(nyHendelseSykdomstidslinje)
            visitor.preVisitBeregnetSykdomstidslinje(nyBeregnetSykdomstidslinje)
            nyBeregnetSykdomstidslinje.accept(visitor)
            visitor.postVisitBeregnetSykdomstidslinje(nyBeregnetSykdomstidslinje)

            visitor.postVisitSykdomshistorikkElement(this, hendelseId, tidsstempel)
        }

        companion object {

            fun nySykdomstidslinje(elementer: List<Element>) = elementer.first().nyBeregnetSykdomstidslinje

            fun opprett(
                historikk: Sykdomshistorikk,
                hendelse: SykdomstidslinjeHendelse,
                tom: LocalDate
            ): Element {
                if (!historikk.isEmpty()) hendelse.nyPadLeft(historikk.sykdomstidslinje().førsteDag())
                val nyHendelseSykdomstidslinje = hendelse.nySykdomstidslinje(tom)
                return Element(
                    hendelseId = hendelse.meldingsreferanseId(),
                    tidsstempel = LocalDateTime.now(),
                    nyHendelseSykdomstidslinje = nyHendelseSykdomstidslinje,
                    nyBeregnetSykdomstidslinje = historikk.kalkulerBeregnetSykdomstidslinje(
                        hendelse,
                        nyHendelseSykdomstidslinje
                    )
                )
            }
        }
    }
}
