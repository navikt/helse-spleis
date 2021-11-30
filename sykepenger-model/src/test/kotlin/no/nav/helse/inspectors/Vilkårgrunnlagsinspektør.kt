package no.nav.helse.inspectors

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

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

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        avviksprosent: Prosent?,
        antallOpptjeningsdagerErMinst: Int,
        harOpptjening: Boolean,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        harMinimumInntekt: Boolean?,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?
    ) {
        val teller = vilkårsgrunnlagTeller.getOrDefault(innslag, 0)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
    }

    override fun preVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag
    ) {
        val teller = vilkårsgrunnlagTeller.getOrDefault(innslag, 0)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
    }
}

internal class GrunnlagsdataInspektør(grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement) : VilkårsgrunnlagHistorikkVisitor {
    internal lateinit var sykepengegrunnlag: Sykepengegrunnlag
        private set
    internal lateinit var sammenligningsgrunnlag: Inntekt
        private set
    internal var avviksprosent: Prosent? = null
        private set
    internal var antallOpptjeningsdagerErMinst by Delegates.notNull<Int>()
        private set
    internal var harOpptjening by Delegates.notNull<Boolean>()
        private set
    internal var meldingsreferanseId: UUID? = null
        private set
    init {
        grunnlagsdata.accept(LocalDate.now(), this)
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        avviksprosent: Prosent?,
        antallOpptjeningsdagerErMinst: Int,
        harOpptjening: Boolean,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        harMinimumInntekt: Boolean?,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?
    ) {
        this.sykepengegrunnlag = sykepengegrunnlag
        this.sammenligningsgrunnlag = sammenligningsgrunnlag
        this.avviksprosent = avviksprosent
        this.antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst
        this.harOpptjening = harOpptjening
        this.meldingsreferanseId = meldingsreferanseId
    }
}
