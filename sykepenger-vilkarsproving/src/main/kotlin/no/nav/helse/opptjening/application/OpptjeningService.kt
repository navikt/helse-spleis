package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.application.VilkårsprøvingService.GrunnlagResultat
import no.nav.helse.opptjening.application.VilkårsprøvingService.PrøvingResultat
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.HarVurdering
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.TrengerArbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidssituasjon
import no.nav.helse.opptjening.domain.Kodeverkkode.OPPTJENING_MINST_4_UKER
import no.nav.helse.opptjening.domain.Opptjeningsgrunnlag
import no.nav.helse.opptjening.domain.Opptjeningsprøving
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsvurdering
import no.nav.helse.opptjening.domain.VurderingId
import no.nav.helse.opptjening.infra.db.InMemoryVilkårsprøvingRepository

/**
 * Oversetter opptjeningsspesifikke kommandoer til den generelle prøvingsflyten.
 * All orkestrering ligger i [VilkårsprøvingService]; her er kun det som er særegent for opptjening.
 */
internal class OpptjeningService(
    vilkårsvurderingRepository: VilkårsvurderingRepository,
    vilkårsprøvingRepository: VilkårsprøvingRepository = InMemoryVilkårsprøvingRepository()
) {
    private val vilkårsprøving = VilkårsprøvingService(vilkårsvurderingRepository, vilkårsprøvingRepository)

    fun vurderOpptjening(fødselsnummer: String, skjæringstidspunkt: LocalDate, arbeidssituasjon: Arbeidssituasjon): VurderOpptjeningResultat {
        // TODO: I fremtiden bør vi sjekke at eksisterende vurdering ble gjort på samme arbeidssituasjon,
        //  dersom situasjonen på et skjæringstidspunkt kan endre seg.
        val resultat = vilkårsprøving.prøv(Vilkår.Opptjening, fødselsnummer, skjæringstidspunkt) {
            Opptjeningsprøving.start(fødselsnummer, skjæringstidspunkt, arbeidssituasjon)
        }
        return when (resultat) {
            is PrøvingResultat.HarVurdering -> HarVurdering(fødselsnummer, skjæringstidspunkt, resultat.vurdering.id)
            is PrøvingResultat.TrengerGrunnlag -> TrengerArbeidsforhold(fødselsnummer, skjæringstidspunkt)
        }
    }

    fun behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
        arbeidsforhold: List<Arbeidsforhold>,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate
    ): BehandleGrunnlagResultat {
        val resultat = vilkårsprøving.behandleGrunnlag(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = Opptjeningsgrunnlag.Arbeidstaker(arbeidsforhold)
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

    fun finnOpptjeningsvurdering(vurderingId: VurderingId): Vilkårsvurdering =
        vilkårsprøving.finnVurdering(Vilkår.Opptjening, vurderingId)

    /**
     * Oppretter en manuell saksbehandleroverstyring av § 8-2 (opptjeningskravet) fra avslag til innvilgelse.
     * Det foreligger ikke noe faktabasert grunnlag; saksbehandlerens begrunnelse er dokumentert i [begrunnelse].
     *
     * Den nye vurderingen blir gjeldende og vil returneres ved neste spørring mot [finnOpptjeningsvurdering].
     */
    fun overstyrOpptjening(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        saksbehandlerIdent: String,
        begrunnelse: String,
    ): VurderingId {
        val vurdering = vilkårsprøving.overstyr(
            vilkår = Vilkår.Opptjening,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            grunnlag = Opptjeningsgrunnlag.ManuellOverstyring,
            kodeverkkode = OPPTJENING_MINST_4_UKER,
            saksbehandlerIdent = saksbehandlerIdent,
            fritekstbegrunnelse = begrunnelse
        )
        return vurdering.id
    }
}
