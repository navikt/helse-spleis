package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.application.VilkårsprøvingService.GrunnlagResultat
import no.nav.helse.opptjening.application.VilkårsprøvingService.PrøvingResultat
import no.nav.helse.opptjening.application.VurderMedlemskapResultat.HarVurdering
import no.nav.helse.opptjening.application.VurderMedlemskapResultat.TrengerMedlemskap
import no.nav.helse.opptjening.domain.Medlemskapsgrunnlag
import no.nav.helse.opptjening.domain.Medlemskapsprøving
import no.nav.helse.opptjening.domain.Medlemskapssvar
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsvurdering
import no.nav.helse.opptjening.domain.VurderingId
import no.nav.helse.opptjening.infra.db.InMemoryVilkårsprøvingRepository

/**
 * Oversetter medlemskapsspesifikke kommandoer til den generelle prøvingsflyten.
 * Speiler [OpptjeningService] — forskjellen er hvilket grunnlag som innhentes.
 */
internal class MedlemskapService(
    vilkårsvurderingRepository: VilkårsvurderingRepository,
    vilkårsprøvingRepository: VilkårsprøvingRepository = InMemoryVilkårsprøvingRepository()
) {
    private val vilkårsprøving = VilkårsprøvingService(vilkårsvurderingRepository, vilkårsprøvingRepository)

    fun vurderMedlemskap(fødselsnummer: String, skjæringstidspunkt: LocalDate): VurderMedlemskapResultat {
        val resultat = vilkårsprøving.prøv(Vilkår.Medlemskap, fødselsnummer, skjæringstidspunkt) {
            Medlemskapsprøving.start(fødselsnummer, skjæringstidspunkt)
        }
        return when (resultat) {
            is PrøvingResultat.HarVurdering -> HarVurdering(fødselsnummer, skjæringstidspunkt, resultat.vurdering.id)
            is PrøvingResultat.TrengerGrunnlag -> TrengerMedlemskap(fødselsnummer, skjæringstidspunkt)
        }
    }

    fun behandleGrunnlagForMedlemskapsvurdering(
        svar: Medlemskapssvar,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate
    ): BehandleGrunnlagResultat {
        val resultat = vilkårsprøving.behandleGrunnlag(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = Medlemskapsgrunnlag(svar)
        )
        return when (resultat) {
            is GrunnlagResultat.NyVurderingForetatt -> BehandleGrunnlagResultat.NyVurderingForetatt(fødselsnummer, skjæringstidspunkt, resultat.vurdering.id)
            GrunnlagResultat.AlleredeVurdert -> BehandleGrunnlagResultat.AlleredeVurdert
            GrunnlagResultat.IngenPrøvingFunnet -> BehandleGrunnlagResultat.IngenPrøvingFunnet
        }
    }

    sealed class BehandleGrunnlagResultat {
        data class NyVurderingForetatt(val fødselsnummer: String, val skjæringstidspunkt: LocalDate, val vurderingId: VurderingId) : BehandleGrunnlagResultat()
        data object AlleredeVurdert : BehandleGrunnlagResultat()
        data object IngenPrøvingFunnet : BehandleGrunnlagResultat()
    }

    fun finnMedlemskapsvurdering(vurderingId: VurderingId): Vilkårsvurdering =
        vilkårsprøving.finnVurdering(Vilkår.Medlemskap, vurderingId)
}
