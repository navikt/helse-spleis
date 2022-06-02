package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Opptjening
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import kotlin.properties.Delegates

internal val VilkårsgrunnlagHistorikk.inspektør get() = Vilkårgrunnlagsinspektør(this)

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
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        val teller = vilkårsgrunnlagTeller.getOrDefault(innslag, 0)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
    }

    override fun preVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {
        val teller = vilkårsgrunnlagTeller.getOrDefault(innslag, 0)
        vilkårsgrunnlagTeller[innslag] = teller.inc()
    }
}

internal val VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.inspektør get() = GrunnlagsdataInspektør(this)

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
    internal var harMinimumInntekt: Boolean? = null
        private set
    internal var vurdertOk by Delegates.notNull<Boolean>()
        private set
    init {
        grunnlagsdata.accept(this)
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        sammenligningsgrunnlag: Inntekt,
        avviksprosent: Prosent?,
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        this.sykepengegrunnlag = sykepengegrunnlag
        this.sammenligningsgrunnlag = sammenligningsgrunnlag
        this.avviksprosent = avviksprosent
        this.meldingsreferanseId = meldingsreferanseId
        this.harMinimumInntekt = harMinimumInntekt
        this.vurdertOk = vurdertOk
    }

    override fun postVisitInfotrygdVilkårsgrunnlag(
        infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID
    ) {
        this.sykepengegrunnlag = sykepengegrunnlag
    }

    override fun preVisitOpptjening(opptjening: Opptjening, arbeidsforhold: List<Opptjening.ArbeidsgiverOpptjeningsgrunnlag>, opptjeningsperiode: Periode) {
        this.antallOpptjeningsdagerErMinst = opptjening.opptjeningsdager()
        this.harOpptjening = opptjening.erOppfylt()
    }
}
