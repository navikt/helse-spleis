package no.nav.helse.inspectors

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal val Sykdomshistorikk.inspektør get() = SykdomshistorikkInspektør(this)

internal class SykdomshistorikkInspektør(historikk: Sykdomshistorikk) : SykdomshistorikkVisitor {
    private val tidslinjer = mutableListOf<Sykdomstidslinje>()
    private val perioderPerHendelse = mutableMapOf<UUID, MutableList<Periode>>()

    init {
        historikk.accept(this)
    }

    fun elementer() = tidslinjer.size
    fun perioderPerHendelse() = perioderPerHendelse.toMap()
    fun tidslinje(elementIndeks: Int) = tidslinjer[elementIndeks]

    override fun visitSykdomshistorikkElement(
        element: Sykdomshistorikk.Element,
        id: UUID,
        hendelseId: UUID?,
        tidsstempel: LocalDateTime,
        hendelseSykdomstidslinje: Sykdomstidslinje,
        beregnetSykdomstidslinje: Sykdomstidslinje
    ) {
        hendelseId?.let {
            perioderPerHendelse.getOrPut(it) { mutableListOf() }.add(hendelseSykdomstidslinje.periode()!!)
        }
        tidslinjer.add(beregnetSykdomstidslinje)
    }
}
