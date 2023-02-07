package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal val Sykdomshistorikk.inspektør get() = SykdomshistorikkInspektør(this)

internal class SykdomshistorikkInspektør(historikk: Sykdomshistorikk) : SykdomshistorikkVisitor {
    private var elementteller = 0
    private val tidslinjer = mutableListOf<Sykdomstidslinje>()
    private val perioderPerHendelse = mutableMapOf<UUID, MutableList<Periode>>()

    init {
        historikk.accept(this)
    }

    fun elementer() = elementteller
    fun perioderPerHendelse() = perioderPerHendelse.toMap()
    fun tidslinje(elementIndeks: Int) = tidslinjer[elementIndeks]

    override fun postVisitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime
    ) {
        elementteller += 1
        hendelseId?.let {
            perioderPerHendelse.getOrPut(it) { mutableListOf() }.add(element.inspektør.periode)
        }
    }


    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        tidslinjer.add(elementteller, tidslinje)
    }
}

internal val Sykdomshistorikk.Element.inspektør get() = SykdomshistorikkElementInspektør(this)

internal class SykdomshistorikkElementInspektør(element: Sykdomshistorikk.Element) : SykdomshistorikkVisitor {

    lateinit var periode: Periode

    init {
        element.accept(this)
    }

    override fun preVisitHendelseSykdomstidslinje(
        tidslinje: Sykdomstidslinje,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime
    ) {
        periode = tidslinje.periode()!!
    }
}
