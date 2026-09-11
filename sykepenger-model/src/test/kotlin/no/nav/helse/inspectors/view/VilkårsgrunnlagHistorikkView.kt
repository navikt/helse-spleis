package no.nav.helse.inspectors.view

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikk.Grunnlagsdata
import no.nav.helse.person.VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement

internal data class VilkårsgrunnlagHistorikkView(val innslag: List<VilkårsgrunnlagInnslagView>)
internal data class VilkårsgrunnlagInnslagView(val vilkårsgrunnlag: List<VilkårsgrunnlagView>)

internal sealed interface VilkårsgrunnlagView {
    val vilkårsgrunnlagId: UUID
    val skjæringstidspunkt: LocalDate
    val inntektsgrunnlag: InntektsgrunnlagView
}

internal data class GrunnlagsdataView(
    override val vilkårsgrunnlagId: UUID,
    override val skjæringstidspunkt: LocalDate,
    override val inntektsgrunnlag: InntektsgrunnlagView,
    val medlemskapstatus: MedlemskapstatusView,
    val meldingsreferanseId: MeldingsreferanseId?,
    val opptjening: ArbeidstakerOpptjeningView?
): VilkårsgrunnlagView {
    enum class MedlemskapstatusView { Ja, Nei, VetIkke, UavklartMedBrukerspørsmål }
}

internal data class InfotrygdView(
    override val vilkårsgrunnlagId: UUID,
    override val skjæringstidspunkt: LocalDate,
    override val inntektsgrunnlag: InntektsgrunnlagView,
): VilkårsgrunnlagView

internal fun VilkårsgrunnlagHistorikk.view() = VilkårsgrunnlagHistorikkView(innslag = historikk().map { it.view() })

internal fun VilkårsgrunnlagHistorikk.Innslag.view() = VilkårsgrunnlagInnslagView(vilkårsgrunnlag = vilkårsgrunnlag.map { it.value.view() })

internal fun VilkårsgrunnlagElement.view(): VilkårsgrunnlagView = when (this) {
    is Grunnlagsdata -> this.view()
    is InfotrygdVilkårsgrunnlag -> this.view()
}

internal fun Grunnlagsdata.view() = GrunnlagsdataView(
    vilkårsgrunnlagId = vilkårsgrunnlagId,
    skjæringstidspunkt = skjæringstidspunkt,
    meldingsreferanseId = meldingsreferanseId,
    inntektsgrunnlag = inntektsgrunnlag.view(),
    opptjening = opptjening?.view(),
    medlemskapstatus = when (medlemskapstatus) {
        Medlemskapsvurdering.Medlemskapstatus.Ja -> GrunnlagsdataView.MedlemskapstatusView.Ja
        Medlemskapsvurdering.Medlemskapstatus.Nei -> GrunnlagsdataView.MedlemskapstatusView.Nei
        Medlemskapsvurdering.Medlemskapstatus.VetIkke -> GrunnlagsdataView.MedlemskapstatusView.VetIkke
        Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål -> GrunnlagsdataView.MedlemskapstatusView.UavklartMedBrukerspørsmål
    },
)

internal fun InfotrygdVilkårsgrunnlag.view() = InfotrygdView(
    vilkårsgrunnlagId = vilkårsgrunnlagId,
    inntektsgrunnlag = inntektsgrunnlag.view(),
    skjæringstidspunkt = skjæringstidspunkt
)
