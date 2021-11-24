package no.nav.helse.inspectors

import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDateTime
import java.util.*

internal val Sykdomshistorikk.inspektør get() = SykdomshistorikkInspektør(this)

internal class SykdomshistorikkInspektør(historikk: Sykdomshistorikk) : SykdomshistorikkVisitor {
    private var elementteller = 0
    private val tidslinjer = mutableListOf<Sykdomstidslinje>()

    init {
        historikk.accept(this)
    }

    fun elementer() = elementteller
    fun tidslinje(elementIndeks: Int) = tidslinjer[elementIndeks]

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        elementteller += 1
    }

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        tidslinjer.add(elementteller, tidslinje)
    }
}
