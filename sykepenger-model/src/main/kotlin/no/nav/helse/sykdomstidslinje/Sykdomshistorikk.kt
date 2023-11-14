package no.nav.helse.sykdomstidslinje

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk.Element.Companion.uhåndtertSykdomstidslinje
import no.nav.helse.tournament.Dagturnering

class Sykdomshistorikk private constructor(
    private val elementer: MutableList<Element>
) {
    internal constructor() : this(mutableListOf())

    internal val size get() = elementer.size

    internal fun isEmpty() = elementer.isEmpty()

    internal fun harSykdom() = !isEmpty() && !elementer.first().isEmpty()

    internal fun sykdomstidslinje() = Element.sykdomstidslinje(elementer)

    internal fun håndter(hendelse: SykdomshistorikkHendelse): Sykdomstidslinje {
        val nyttElement = hendelse.element()
        val uhåndtertSykdomstidslinje = elementer.uhåndtertSykdomstidslinje(hendelse) ?: return sykdomstidslinje()
        elementer.add(0, nyttElement.merge(this, uhåndtertSykdomstidslinje))
        return sykdomstidslinje()
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

    class Element private constructor(
        private val id: UUID = UUID.randomUUID(),
        private val hendelseId: UUID? = null,
        private val tidsstempel: LocalDateTime = LocalDateTime.now(),
        private val hendelseSykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje(),
        private val beregnetSykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje(),
    ) : Comparable<Element> {

        internal fun merge(historikk: Sykdomshistorikk, uhåndtertSykdomstidslinje: Sykdomstidslinje): Element {
            val beregnetSykdomstidslinje = mergeTidslinje(historikk.elementer.firstOrNull(), uhåndtertSykdomstidslinje)
            return Element(
                id = this.id,
                hendelseId = this.hendelseId,
                tidsstempel = this.tidsstempel,
                hendelseSykdomstidslinje = uhåndtertSykdomstidslinje,
                beregnetSykdomstidslinje = beregnetSykdomstidslinje
            )
        }

        private fun mergeTidslinje(forrige: Element?, uhåndtertSykdomstidslinje: Sykdomstidslinje) =
            forrige?.beregnetSykdomstidslinje?.merge(uhåndtertSykdomstidslinje, Dagturnering.TURNERING::beste) ?: uhåndtertSykdomstidslinje

        internal fun accept(visitor: SykdomshistorikkVisitor) {
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

        internal fun harHåndtert(hendelse: SykdomshistorikkHendelse) = hendelseId == hendelse.element().hendelseId

        internal fun isEmpty(): Boolean = !beregnetSykdomstidslinje.iterator().hasNext()

        companion object {
            internal fun List<Element>.uhåndtertSykdomstidslinje(hendelse: SykdomshistorikkHendelse) : Sykdomstidslinje? {
                if (hendelse.element().hendelseSykdomstidslinje.periode() == null) return null // tom sykdomstidslinje
                val tidligere = filter { it.harHåndtert(hendelse) }.takeUnless { it.isEmpty() } ?: return hendelse.element().hendelseSykdomstidslinje // Første gang vi ser hendelsen
                val alleredeHåndtertSykdomstidslinje = tidligere.fold(Sykdomstidslinje()) { tidligereHåndtert, element ->
                    tidligereHåndtert + element.hendelseSykdomstidslinje
                }
                val uhåndtertSykdomstidslinje = hendelse.element().hendelseSykdomstidslinje - alleredeHåndtertSykdomstidslinje
                if (uhåndtertSykdomstidslinje.periode() == null) return null // Tom sykdomstidslinje, ikke noe nytt
                return uhåndtertSykdomstidslinje.also {
                    hendelse.info("Legger til bit nummer ${tidligere.size +1 } for ${it.periode()} i sykdomshistorikken")
                }
            }

            internal fun sykdomstidslinje(elementer: List<Element>) = elementer.first().beregnetSykdomstidslinje

            internal fun opprett(
                meldingsreferanseId: UUID,
                hendelseSykdomstidslinje: Sykdomstidslinje
            ): Element {
                return Element(
                    hendelseId = meldingsreferanseId,
                    hendelseSykdomstidslinje = hendelseSykdomstidslinje,
                )
            }

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
