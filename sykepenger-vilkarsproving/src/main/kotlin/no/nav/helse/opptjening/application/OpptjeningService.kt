package no.nav.helse.opptjening.application

import java.time.LocalDate
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.HarVurdering
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.TrengerArbeidsforhold
import no.nav.helse.opptjening.bootstrap.sikkerLogg
import no.nav.helse.opptjening.domain.Arbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidssituasjon
import no.nav.helse.opptjening.domain.Opptjeningsgrunnlag
import no.nav.helse.opptjening.domain.Opptjeningsprøving
import no.nav.helse.opptjening.domain.Opptjeningsvurdering
import no.nav.helse.opptjening.domain.VurderingId
import no.nav.helse.opptjening.infra.db.InMemoryVilkårsprøvingRepository

/**
 * Orkestrerer prøvingen: slår opp om vi allerede har et svar, starter prøvinger, og tar imot
 * grunnlag etter hvert som det kommer inn. Selve reglene ligger i domenet.
 */
internal class OpptjeningService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val vilkårsprøvingRepository: VilkårsprøvingRepository = InMemoryVilkårsprøvingRepository()
) {

    fun vurderOpptjening(fødselsnummer: String, skjæringstidspunkt: LocalDate, arbeidssituasjon: Arbeidssituasjon): VurderOpptjeningResultat {
        // TODO: I fremtiden bør vi sjekke at eksisterende vurdering ble gjort på samme arbeidssituasjon,
        //  dersom situasjonen på et skjæringstidspunkt kan endre seg.
        vilkårsvurderingRepository.gjeldende(fødselsnummer, skjæringstidspunkt)?.let { vurdering ->
            sikkerLogg.info("Har allerede vurdering for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${vurdering.id}.")
            return HarVurdering(fødselsnummer, skjæringstidspunkt, vurdering.id)
        }

        vilkårsprøvingRepository.finnSiste(fødselsnummer, skjæringstidspunkt)?.takeUnless { it.erAvsluttet }?.let { pågående ->
            sikkerLogg.info("Prøving ${pågående.id} pågår allerede for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. Etterspør grunnlaget på nytt.")
            return TrengerArbeidsforhold(fødselsnummer, skjæringstidspunkt)
        }

        val (prøving, vurdering) = Opptjeningsprøving.start(
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidssituasjon = arbeidssituasjon
        )
        vilkårsprøvingRepository.opprett(prøving)

        if (vurdering == null) {
            sikkerLogg.info("Startet prøving ${prøving.id} for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. Venter på ${prøving.uteståendeBehov}.")
            return TrengerArbeidsforhold(fødselsnummer, skjæringstidspunkt)
        }

        vilkårsvurderingRepository.lagre(vurdering)
        sikkerLogg.info("Prøving ${prøving.id} fullført uten innhenting for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${vurdering.id}.")
        return HarVurdering(fødselsnummer, skjæringstidspunkt, vurdering.id)
    }

    fun behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
        arbeidsforhold: List<Arbeidsforhold>,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate
    ): BehandleGrunnlagResultat {
        val prøving = vilkårsprøvingRepository.finnSiste(fødselsnummer, skjæringstidspunkt)

        if (prøving == null) {
            sikkerLogg.error("Mottatt løsning på behov for ArbeidsforholdV2 for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt, men fant ingen prøving.")
            return BehandleGrunnlagResultat.IngenPrøvingFunnet
        }

        if (prøving.erAvsluttet) {
            sikkerLogg.info("Mottatt løsning på behov for ArbeidsforholdV2 for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt, men prøving ${prøving.id} er allerede avsluttet.")
            return BehandleGrunnlagResultat.AlleredeVurdert
        }

        val vurdering = prøving.motta(Opptjeningsgrunnlag.Arbeidstaker(arbeidsforhold))
        vilkårsvurderingRepository.lagre(vurdering)
        vilkårsprøvingRepository.oppdater(prøving)
        sikkerLogg.info("Prøving ${prøving.id} fullført for fødselsnummer $fødselsnummer med skjæringstidspunkt $skjæringstidspunkt. VurderingId: ${vurdering.id}.")
        return BehandleGrunnlagResultat.NyVurderingForetatt(fødselsnummer, skjæringstidspunkt, vurdering.id)
    }

    sealed class BehandleGrunnlagResultat {
        data class NyVurderingForetatt(val fødselsnummer: String, val skjæringstidspunkt: LocalDate, val vurderingId: VurderingId) : BehandleGrunnlagResultat()
        data object AlleredeVurdert : BehandleGrunnlagResultat()
        data object IngenPrøvingFunnet : BehandleGrunnlagResultat()
    }

    fun finnOpptjeningsvurdering(vurderingId: VurderingId): Opptjeningsvurdering =
        vilkårsvurderingRepository.finn(vurderingId) ?: error("Fant ikke opptjeningsvurdering med id $vurderingId")
}
