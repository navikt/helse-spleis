package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.tournament.dagturnering
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjeberegning
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
        if (isEmpty() || !elementer.first().harHåndtert(hendelse)) {
            elementer.add(0, Element.opprett(this, hendelse))
        }
        return sykdomstidslinje()
    }

    internal fun tøm() {
        elementer.add(0, Element.empty)
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
        hendelse: SykdomstidslinjeHendelse,
        hendelseSykdomstidslinje: Sykdomstidslinje
    ): Sykdomstidslinje {
        val tidslinje = if (elementer.isEmpty())
            hendelseSykdomstidslinje
        else
            sykdomstidslinje().merge(hendelseSykdomstidslinje, dagturnering::beste)
        return tidslinje.also { it.valider(hendelse) }
    }

    internal fun fjernDagerFør(nyFørsteDag: LocalDate) {
        elementer.add(0, Element.opprettUtenDagerFør(nyFørsteDag, this))
    }

    internal fun lagUtbetalingstidslinjeberegning(organisasjonsnummer: String, utbetalingstidslinje: Utbetalingstidslinje) =
        Element.lagUtbetalingstidslinjeberegning(elementer, organisasjonsnummer, utbetalingstidslinje)


    internal class Element private constructor(
        private val id: UUID = UUID.randomUUID(),
        private val hendelseId: UUID? = null,
        private val tidsstempel: LocalDateTime = LocalDateTime.now(),
        private val hendelseSykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje(),
        private val beregnetSykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje(),
    ) : Comparable<Element> {

        fun accept(visitor: SykdomshistorikkVisitor) {
            visitor.preVisitSykdomshistorikkElement(this, id, hendelseId, tidsstempel)
            visitor.preVisitHendelseSykdomstidslinje(hendelseSykdomstidslinje, hendelseId, tidsstempel)
            hendelseSykdomstidslinje.accept(visitor)
            visitor.postVisitHendelseSykdomstidslinje(hendelseSykdomstidslinje)
            visitor.preVisitBeregnetSykdomstidslinje(beregnetSykdomstidslinje)
            beregnetSykdomstidslinje.accept(visitor)
            visitor.postVisitBeregnetSykdomstidslinje(beregnetSykdomstidslinje)

            visitor.postVisitSykdomshistorikkElement(this, id, hendelseId, tidsstempel)
        }

        override fun compareTo(other: Element) = this.tidsstempel.compareTo(other.tidsstempel)

        override fun toString() = beregnetSykdomstidslinje.toString()

        internal fun harHåndtert(hendelse: SykdomstidslinjeHendelse) = hendelseId == hendelse.meldingsreferanseId()

        internal fun lagUtbetalingstidslinjeberegning(organisasjonsnummer: String, utbetalingstidslinje: Utbetalingstidslinje) =
            Utbetalingstidslinjeberegning(id, organisasjonsnummer, utbetalingstidslinje)


        companion object {

            internal val empty get() = Element()

            internal fun sykdomstidslinje(elementer: List<Element>) = elementer.first().beregnetSykdomstidslinje

            internal fun lagUtbetalingstidslinjeberegning(
                elementer: List<Element>,
                organisasjonsnummer: String,
                utbetalingstidslinje: Utbetalingstidslinje
            ) = elementer.first().lagUtbetalingstidslinjeberegning(organisasjonsnummer, utbetalingstidslinje)

            internal fun opprett(
                historikk: Sykdomshistorikk,
                hendelse: SykdomstidslinjeHendelse
            ): Element {
                val hendelseSykdomstidslinje = hendelse.sykdomstidslinje()
                return Element(
                    hendelseId = hendelse.meldingsreferanseId(),
                    hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                    beregnetSykdomstidslinje = historikk.sammenslåttTidslinje(
                        hendelse,
                        hendelseSykdomstidslinje
                    )
                )
            }

            internal fun opprettReset(
                historikk: Sykdomshistorikk,
                periode: Periode
            ) : Element {
                return Element(beregnetSykdomstidslinje = historikk.sykdomstidslinje().trim(periode))
            }
            internal fun opprettUtenDagerFør(
                fraOgMed: LocalDate,
                historikk: Sykdomshistorikk
            ) : Element {
                return Element(beregnetSykdomstidslinje = historikk.sykdomstidslinje().fraOgMed(fraOgMed))
            }
        }
    }
}
