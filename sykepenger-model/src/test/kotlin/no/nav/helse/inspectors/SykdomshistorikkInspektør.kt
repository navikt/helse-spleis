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
    private val hendelseIder = mutableListOf<UUID>()

    init {
        historikk.accept(this)
    }

    fun elementer() = elementteller
    fun hendelseIder() = hendelseIder.toList()
    fun tidslinje(elementIndeks: Int) = tidslinjer[elementIndeks]

    override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
        elementteller += 1
        hendelseId?.let { hendelseIder.add(it) }
    }

    override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
        tidslinjer.add(elementteller, tidslinje)
    }
}
