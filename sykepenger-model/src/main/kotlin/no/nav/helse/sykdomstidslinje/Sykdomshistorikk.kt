package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
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

    internal fun sykdomstidslinje() = Element.sykdomstidslinje(elementer)

    internal fun håndter(hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
        elementer.add(
            0, Element.opprett(
                this,
                hendelse,
                if (elementer.isEmpty()) LocalDate.MAX else sykdomstidslinje().sisteDag()
            )
        )
        return sykdomstidslinje()
    }

    internal fun nyHåndter(hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
        if (isEmpty() || !elementer.first().harHåndtert(hendelse)) {
            elementer.add(0, Element.opprett(this, hendelse))
        }
        return sykdomstidslinje()
    }

    internal fun tøm() {
        elementer.add(0, Element.opprettTom())
    }

    internal fun fjernDager(periode: Periode) {
        // TODO: Remove size == 0 whenever migration is done
        if (size == 0 || sykdomstidslinje().length() == 0) return
        if (sykdomstidslinje().periode()?.overlapperMed(periode) != true) return
        elementer.add(0, Element.opprettReset(this, periode))
    }

    internal fun accept(visitor: SykdomshistorikkVisitor) {
        visitor.preVisitSykdomshistorikk(this)
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitSykdomshistorikk(this)
    }

    private fun sammenslåttTidslinje(
        hendelseSykdomstidslinje: Sykdomstidslinje
    ): Sykdomstidslinje {
        return if (elementer.isEmpty())
            hendelseSykdomstidslinje
        else
            sykdomstidslinje().merge(hendelseSykdomstidslinje, dagturnering::beste)
    }

    internal class Element private constructor(
        private val hendelseId: UUID?,
        private val tidsstempel: LocalDateTime,
        private val hendelseSykdomstidslinje: Sykdomstidslinje,
        private val beregnetSykdomstidslinje: Sykdomstidslinje
    ) : Comparable<Element> {

        fun accept(visitor: SykdomshistorikkVisitor) {
            visitor.preVisitSykdomshistorikkElement(this, hendelseId, tidsstempel)
            visitor.preVisitHendelseSykdomstidslinje(hendelseSykdomstidslinje, hendelseId, tidsstempel)
            hendelseSykdomstidslinje.accept(visitor)
            visitor.postVisitHendelseSykdomstidslinje(hendelseSykdomstidslinje)
            visitor.preVisitBeregnetSykdomstidslinje(beregnetSykdomstidslinje)
            beregnetSykdomstidslinje.accept(visitor)
            visitor.postVisitBeregnetSykdomstidslinje(beregnetSykdomstidslinje)

            visitor.postVisitSykdomshistorikkElement(this, hendelseId, tidsstempel)
        }

        override fun compareTo(other: Element) = this.tidsstempel.compareTo(other.tidsstempel)

        internal fun merge(other: Element) = Element(
            other.hendelseId,
            other.tidsstempel,
            other.hendelseSykdomstidslinje,
            this.beregnetSykdomstidslinje.merge(other.beregnetSykdomstidslinje) { _: Dag, høyre: Dag -> høyre }
        )

        override fun toString() = beregnetSykdomstidslinje.toString()

        internal fun harHåndtert(hendelse: SykdomstidslinjeHendelse) = hendelseId == hendelse.meldingsreferanseId()

        companion object {

            internal val empty = Element(null, LocalDateTime.now(), Sykdomstidslinje(), Sykdomstidslinje())

            internal fun sykdomstidslinje(elementer: List<Element>) = elementer.first().beregnetSykdomstidslinje

            internal fun opprett(historikk: Sykdomshistorikk,
                          hendelse: SykdomstidslinjeHendelse): Element {
                val hendelseSykdomstidslinje = hendelse.sykdomstidslinje()
                return Element(
                    hendelseId = hendelse.meldingsreferanseId(),
                    tidsstempel = LocalDateTime.now(),
                    hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                    beregnetSykdomstidslinje = historikk.sammenslåttTidslinje(hendelseSykdomstidslinje)
                )
            }

            internal fun opprett(
                historikk: Sykdomshistorikk,
                hendelse: SykdomstidslinjeHendelse,
                tom: LocalDate
            ): Element {
                if (!historikk.isEmpty()) hendelse.padLeft(historikk.sykdomstidslinje().førsteDag())
                val hendelseSykdomstidslinje = hendelse.sykdomstidslinje(tom)
                return Element(
                    hendelseId = hendelse.meldingsreferanseId(),
                    tidsstempel = LocalDateTime.now(),
                    hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                    beregnetSykdomstidslinje = historikk.sammenslåttTidslinje(hendelseSykdomstidslinje)
                )
            }

            internal fun opprettTom() : Element {
                return Element(
                    hendelseId = null,
                    tidsstempel = LocalDateTime.now(),
                    hendelseSykdomstidslinje = Sykdomstidslinje(),
                    beregnetSykdomstidslinje = Sykdomstidslinje()
                )
            }

            internal fun opprettReset(
                historikk: Sykdomshistorikk,
                periode: Periode
            ) : Element {
                return Element(
                    hendelseId = null,
                    tidsstempel = LocalDateTime.now(),
                    hendelseSykdomstidslinje = Sykdomstidslinje(),
                    beregnetSykdomstidslinje = historikk.sykdomstidslinje().trim(periode)
                )
            }
        }
    }
}
