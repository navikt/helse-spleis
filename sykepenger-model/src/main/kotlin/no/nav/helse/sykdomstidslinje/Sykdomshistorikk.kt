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

    companion object {
        internal fun merge(historikker: List<Sykdomshistorikk>) = Sykdomshistorikk().also { result ->
            historikker
                .flatMap { it.elementer }
                .sorted()
                .fold(Element.empty) { forrige, nåværende -> forrige.merge(nåværende).also { result.elementer.add(0, it) }
                }
        }
    }

    internal val size get() = elementer.size

    internal fun isEmpty() = elementer.isEmpty()

    internal fun sykdomstidslinje() = Element.sykdomstidslinje(elementer)

    internal fun håndter(hendelse: SykdomstidslinjeHendelse) {
        elementer.add(
            0, Element.opprett(
                this,
                hendelse,
                if (elementer.isEmpty()) LocalDate.MAX else sykdomstidslinje().sisteDag()
            )
        )
    }

    internal fun nyHåndter(hendelse: SykdomstidslinjeHendelse) {
        elementer.add(
            0, Element.opprett(this, hendelse)
        )
    }

    internal fun fjernTidligereDager(periode: Periode) {
        // TODO: Remove size == 0 whenever migration is done
        if (size == 0 || sykdomstidslinje().length() == 0) return
        periode.endInclusive.plusDays(1).also { førsteDagViBeholder ->
            if (førsteDagViBeholder <= sykdomstidslinje().førsteDag()) return
            elementer.add(0, Element.opprettReset(this, førsteDagViBeholder))
        }
    }

    internal fun accept(visitor: SykdomshistorikkVisitor) {
        visitor.preVisitSykdomshistorikk(this)
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitSykdomshistorikk(this)
    }

    private fun sammenslåttTidslinje(
        hendelse: SykdomstidslinjeHendelse,
        hendelseSykdomstidslinje: Sykdomstidslinje
    ): Sykdomstidslinje {
        val tidslinje = if (elementer.isEmpty())
            hendelseSykdomstidslinje
        else
            sykdomstidslinje().merge(hendelseSykdomstidslinje, dagturnering::beste)
        return tidslinje.also { it.valider(hendelse) }
    }

    internal class Element private constructor(
        private val hendelseId: UUID?,
        private val tidsstempel: LocalDateTime,
        private val hendelseSykdomstidslinje: Sykdomstidslinje,
        private val beregnetSykdomstidslinje: Sykdomstidslinje
    ) : Comparable<Element> {

        fun accept(visitor: SykdomshistorikkVisitor) {
            visitor.preVisitSykdomshistorikkElement(this, hendelseId, tidsstempel)
            visitor.preVisitHendelseSykdomstidslinje(hendelseSykdomstidslinje)
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
            this.beregnetSykdomstidslinje.merge(other.beregnetSykdomstidslinje, { venstre: Dag, høyre: Dag -> høyre })
        )

        override fun toString() = beregnetSykdomstidslinje.toString()

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
                    beregnetSykdomstidslinje = historikk.sammenslåttTidslinje(
                        hendelse,
                        hendelseSykdomstidslinje
                    )
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
                    beregnetSykdomstidslinje = historikk.sammenslåttTidslinje(
                        hendelse,
                        hendelseSykdomstidslinje
                    )
                )
            }

            internal fun opprettReset(
                historikk: Sykdomshistorikk,
                førsteDatoViBeholder: LocalDate
            ) : Element {
                return Element(
                    hendelseId = null,
                    tidsstempel = LocalDateTime.now(),
                    hendelseSykdomstidslinje = Sykdomstidslinje(),
                    beregnetSykdomstidslinje = historikk.sykdomstidslinje().trimLeft(førsteDatoViBeholder)
                )
            }
        }
    }
}

internal fun List<Sykdomshistorikk>.merge() = Sykdomshistorikk.merge(this)
