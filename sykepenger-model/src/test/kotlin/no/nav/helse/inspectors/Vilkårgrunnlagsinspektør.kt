package no.nav.helse.inspectors

import java.time.LocalDate
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikkView
import no.nav.helse.person.VilkårsgrunnlagView
import org.junit.jupiter.api.fail

internal val VilkårsgrunnlagHistorikk.inspektør get() = this.view().inspektør
internal val VilkårsgrunnlagHistorikkView.inspektør get() = Vilkårgrunnlagsinspektør(this)

internal class Vilkårgrunnlagsinspektør(view: VilkårsgrunnlagHistorikkView) {
    val vilkårsgrunnlagTeller = view.innslag.mapIndexed { index, innslag -> index to innslag.vilkårsgrunnlag.size }.toMap()
    internal val aktiveSpleisSkjæringstidspunkt = view.innslag.getOrNull(0)?.vilkårsgrunnlag?.map { it.skjæringstidspunkt }?.toSet() ?: emptySet()

    private val grunnlagsdata = view.innslag.flatMap { it.vilkårsgrunnlag.map { it.skjæringstidspunkt to it } }
    private val vilkårsgrunnlagHistorikkInnslag = view.innslag

    internal fun antallGrunnlagsdata() = vilkårsgrunnlagTeller.map(Map.Entry<*, Int>::value).sum()
    internal fun vilkårsgrunnlagHistorikkInnslag() = vilkårsgrunnlagHistorikkInnslag.toList()
    internal fun grunnlagsdata(skjæringstidspunkt: LocalDate) = grunnlagsdata.firstOrNull { it.first == skjæringstidspunkt }?.second ?: fail("Fant ikke grunnlagsdata på skjæringstidspunkt $skjæringstidspunkt")
}

internal val VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement.inspektør get() = view().inspektør
internal val VilkårsgrunnlagView.inspektør get() = GrunnlagsdataInspektør(this)

internal class GrunnlagsdataInspektør(view: VilkårsgrunnlagView) {
    val vilkårsgrunnlagId = view.vilkårsgrunnlagId
    val skjæringstidspunkt = view.skjæringstidspunkt
    val infotrygd = view.type == VilkårsgrunnlagView.VilkårsgrunnlagTypeView.INFOTRYGD
    val meldingsreferanseId = view.meldingsreferanseId
    val inntektsgrunnlag = view.inntektsgrunnlag
    val opptjening = view.opptjening
    val vurdertOk = view.vurdertOk
}
