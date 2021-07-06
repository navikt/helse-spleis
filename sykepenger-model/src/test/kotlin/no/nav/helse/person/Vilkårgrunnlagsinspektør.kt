package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Vilkårgrunnlagsinspektør(historikk: VilkårsgrunnlagHistorikk) : VilkårsgrunnlagHistorikkVisitor {
    val vilkårsgrunnlagTeller = mutableMapOf<Int, Int>()
    var innslag = -1

    init {
        historikk.accept(this)
    }

    override fun preVisitVilkårsgrunnlagHistorikk() {
        vilkårsgrunnlagTeller.clear()
    }

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        this.innslag += 1
    }

    override fun visitGrunnlagsdata(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        val teller = vilkårsgrunnlagTeller.getOrDefault(innslag, 0)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
    }

    override fun visitInfotrygdVilkårsgrunnlag(skjæringstidspunkt: LocalDate, infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag) {
        val teller = vilkårsgrunnlagTeller.getOrDefault(innslag, 0)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
    }

}
