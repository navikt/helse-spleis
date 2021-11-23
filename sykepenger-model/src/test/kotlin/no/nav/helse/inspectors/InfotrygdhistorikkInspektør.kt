package no.nav.helse.inspectors

import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import java.time.LocalDateTime
import java.util.*

internal val Infotrygdhistorikk.inspektør get() = InfotrygdhistorikkInspektør(this)

internal class InfotrygdhistorikkInspektør(historikk: Infotrygdhistorikk) : InfotrygdhistorikkVisitor {
    private var elementer = mutableListOf<Triple<UUID, LocalDateTime, LocalDateTime>>()

    init {
        historikk.accept(this)
    }

    fun elementer() = elementer.size
    fun opprettet(indeks: Int) = elementer.elementAt(indeks).second
    fun oppdatert(indeks: Int) = elementer.elementAt(indeks).third

    override fun preVisitInfotrygdhistorikkElement(
        id: UUID,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        hendelseId: UUID?,
        lagretInntekter: Boolean,
        lagretVilkårsgrunnlag: Boolean,
        harStatslønn: Boolean
    ) {
        elementer.add(Triple(id, tidsstempel, oppdatert))
    }
}
