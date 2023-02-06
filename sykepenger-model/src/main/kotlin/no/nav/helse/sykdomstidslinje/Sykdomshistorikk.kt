package no.nav.helse.sykdomstidslinje

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding.BitAvInntektsmelding
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk.Element.Companion.nyesteId
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk.Element.Companion.uhåndtertBit
import no.nav.helse.tournament.Dagturnering

internal class Sykdomshistorikk private constructor(
    private val elementer: MutableList<Element>
) {
    internal constructor() : this(mutableListOf())

    internal val size get() = elementer.size

    internal fun isEmpty() = elementer.isEmpty()

    internal fun harSykdom() = !isEmpty() && !elementer.first().isEmpty()

    internal fun sykdomstidslinje() = Element.sykdomstidslinje(elementer)

    internal fun nyesteId()= elementer.nyesteId()

    private fun håndterSykdomstidslinjeHendelse(hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
        if (elementer.none { it.harHåndtert(hendelse) }) {
            elementer.add(0, Element.opprett(this, hendelse))
        }
        return sykdomstidslinje()
    }


    private fun håndterBitAvInntektsmelding(bitAvInntektsmelding: BitAvInntektsmelding): Sykdomstidslinje {
        val nyesteElement = elementer.firstOrNull() ?: return håndterSykdomstidslinjeHendelse(bitAvInntektsmelding)
        val uhåndtertBit = elementer.uhåndtertBit(bitAvInntektsmelding) ?: return sykdomstidslinje()
        if (nyesteElement.harHåndtert(bitAvInntektsmelding)) {
            val utvidetElement = Element.utvidNyesteElementMedBitAvInntektsmelding(this, bitAvInntektsmelding, uhåndtertBit, nyesteElement)
            elementer[0] = utvidetElement
            return sykdomstidslinje()
        }
        val nyttElement = Element.nyttElementMedBitAvInntektsmelding(this, bitAvInntektsmelding, uhåndtertBit)
        elementer.add(0, nyttElement)
        return sykdomstidslinje()
    }

    internal fun håndter(hendelse: SykdomstidslinjeHendelse): Sykdomstidslinje {
        if (hendelse is BitAvInntektsmelding) return håndterBitAvInntektsmelding(hendelse)
        return håndterSykdomstidslinjeHendelse(hendelse)
    }

    internal fun fyllUtPeriodeMedForventedeDager(hendelse: PersonHendelse, periode: Periode) {
        val sykdomstidslinje = if (isEmpty()) Sykdomstidslinje() else this.sykdomstidslinje()
        val utvidetTidslinje = sykdomstidslinje.forsøkUtvidelse(periode) ?: return
        val arbeidsdager = Sykdomstidslinje().forsøkUtvidelse(periode)
        elementer.add(0, Element.opprett(hendelse, arbeidsdager!!, utvidetTidslinje))
    }

    internal fun fjernDager(perioder: List<Periode>) {
        if (perioder.isEmpty()) return
        if (isEmpty()) return
        val periode = sykdomstidslinje().periode() ?: return
        if (perioder.none { periode.overlapperMed(it) }) return
        elementer.add(0, Element.opprettReset(this, perioder))
    }

    internal fun accept(visitor: SykdomshistorikkVisitor) {
        visitor.preVisitSykdomshistorikk(this)
        elementer.forEach { it.accept(visitor) }
        visitor.postVisitSykdomshistorikk(this)
    }

    private fun sammenslåttTidslinje(
        hendelse: IAktivitetslogg,
        hendelseSykdomstidslinje: Sykdomstidslinje
    ): Sykdomstidslinje {
        val tidslinje = if (elementer.isEmpty())
            hendelseSykdomstidslinje
        else
            sykdomstidslinje().merge(hendelseSykdomstidslinje, Dagturnering.TURNERING::beste)
        return tidslinje.also { it.valider(hendelse) }
    }

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

        internal fun isEmpty(): Boolean = !beregnetSykdomstidslinje.iterator().hasNext()

        companion object {

            private val empty get() = Element()

            internal fun List<Element>.nyesteId(): UUID = (this.firstOrNull() ?: empty).id

            internal fun List<Element>.uhåndtertBit(bitAvInntektsmelding: BitAvInntektsmelding) = fold(Sykdomstidslinje()) { sammenslåttTidslinje, element ->
                if (element.harHåndtert(bitAvInntektsmelding)) sammenslåttTidslinje + element.hendelseSykdomstidslinje
                else sammenslåttTidslinje
            }.let { håndtertBit ->
                bitAvInntektsmelding.sykdomstidslinje() - håndtertBit
            }.takeUnless { it.periode() == null }

            internal fun sykdomstidslinje(elementer: List<Element>) = elementer.first().beregnetSykdomstidslinje

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

            internal fun utvidNyesteElementMedBitAvInntektsmelding(
                historikk: Sykdomshistorikk,
                bitAvInntektsmelding: BitAvInntektsmelding,
                uhåndtertBit: Sykdomstidslinje,
                nyesteElement: Element
            ): Element {
                return Element(
                    id = UUID.randomUUID(), // TODO: Burde vi beholdt id fra nyesteElement? Men da fungerer ikke cachen til arbeidsgiverperiode 🤕
                    hendelseId = nyesteElement.hendelseId,
                    hendelseSykdomstidslinje = nyesteElement.hendelseSykdomstidslinje + uhåndtertBit,
                    beregnetSykdomstidslinje = historikk.sammenslåttTidslinje(
                        bitAvInntektsmelding,
                        uhåndtertBit
                    )
                )
            }
            internal fun nyttElementMedBitAvInntektsmelding(
                historikk: Sykdomshistorikk,
                bitAvInntektsmelding: BitAvInntektsmelding,
                uhåndtertBit: Sykdomstidslinje
            ): Element {
                return Element(
                    hendelseId = bitAvInntektsmelding.meldingsreferanseId(),
                    hendelseSykdomstidslinje = uhåndtertBit,
                    beregnetSykdomstidslinje = historikk.sammenslåttTidslinje(
                        bitAvInntektsmelding,
                        uhåndtertBit
                    )
                )
            }

            internal fun opprett(hendelse: PersonHendelse, hendelseSykdomstidslinje: Sykdomstidslinje, sykdomstidslinje: Sykdomstidslinje) = Element(
                hendelseId = hendelse.meldingsreferanseId(),
                hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                beregnetSykdomstidslinje = sykdomstidslinje
            )

            internal fun opprettReset(
                historikk: Sykdomshistorikk,
                perioder: List<Periode>
            ): Element {
                return Element(beregnetSykdomstidslinje = historikk.sykdomstidslinje().trim(perioder))
            }

            internal fun ferdigSykdomshistorikkElement(
                id: UUID,
                hendelseId: UUID?,
                tidsstempel: LocalDateTime,
                hendelseSykdomstidslinje: Sykdomstidslinje,
                beregnetSykdomstidslinje: Sykdomstidslinje
            ): Element = Element(
                id = id,
                hendelseId = hendelseId,
                tidsstempel = tidsstempel,
                hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                beregnetSykdomstidslinje = beregnetSykdomstidslinje
            )

        }
    }

    internal companion object {
        internal fun ferdigSykdomshistorikk(historikk: List<Element>): Sykdomshistorikk =
            Sykdomshistorikk(historikk.toMutableList())
    }
}
