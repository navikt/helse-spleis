package no.nav.helse.opptjening.application

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.HarVurdering
import no.nav.helse.opptjening.application.VurderOpptjeningResultat.TrengerArbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidssituasjon
import no.nav.helse.opptjening.domain.Opptjening
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering

class OpptjeningService(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) {

    fun vurderOpptjening(fødselsnummer: String, skjæringstidspunkt: LocalDate, arbeidssituasjon: Arbeidssituasjon): VurderOpptjeningResultat {

        val eksisterendeVilkårsvurdering = vilkårsvurderingRepository.finnNyesteVilkårsvurdering<Opptjening>(fødselsnummer, skjæringstidspunkt)
        eksisterendeVilkårsvurdering?.let {

            return if (it.erKomplett) {

                // TODO: I fremtiden bør vi sjekke at logikken på eksisterende vurdering var gjort basert på samme arbeidssituasjon.
                //  Om vi i fremtiden kan endre situasjonen på et skjæringstidspunkt
                HarVurdering(fødselsnummer, skjæringstidspunkt, it.id)
            } else {
                // Nytt behov nedover. Mulig jeg ble påminnet av spleis.
                TrengerArbeidsforhold(fødselsnummer, skjæringstidspunkt)
            }
        }


        return when (arbeidssituasjon) {
            Arbeidssituasjon.Arbeidstaker -> TrengerArbeidsforhold(fødselsnummer)

            Arbeidssituasjon.SelvstendigNæringsdrivende -> {
                val vurdering = Opptjening.AutomatiskVurdering.nyAutomatiskVurdering(
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    versjonAvKildekode = "",
                )
                vurdering.fullfør(grunnlagForAutomatiskVurdering = OpptjeningsgrunnlagForAutomatiskVurdering.ForSelvstendigNæringsdrivende)
                vilkårsvurderingRepository.lagre(vurdering)
                HarVurdering(fødselsnummer, skjæringstidspunkt, vurdering.id)
            }
        }
    }

    fun behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(
        arbeidsforhold: List<Arbeidsforhold>,
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): BehandleGrunnlagResultat {

        val eksisterendeVilkårsvurdering = vilkårsvurderingRepository.finnNyesteVilkårsvurdering<Opptjening.AutomatiskVurdering>(fødselsnummer, skjæringstidspunkt)

        if (eksisterendeVilkårsvurdering == null) {
            // TODO log feil her
            return BehandleGrunnlagResultat.IngenVurderingFunnet
        }


        if (eksisterendeVilkårsvurdering.erKomplett) {
            return BehandleGrunnlagResultat.AlleredeVurdert
        }

        eksisterendeVilkårsvurdering.fullfør(grunnlagForAutomatiskVurdering = OpptjeningsgrunnlagForAutomatiskVurdering.ForArbeidstaker(arbeidsforhold = arbeidsforhold))
        vilkårsvurderingRepository.lagre(eksisterendeVilkårsvurdering)
        return BehandleGrunnlagResultat.NyVurderingForetatt(fødselsnummer, skjæringstidspunkt, eksisterendeVilkårsvurdering.id)
    }

    sealed class BehandleGrunnlagResultat {
        data class NyVurderingForetatt(val fødselsnummer: String, val skjæringstidspunkt: LocalDate, val vurderingId: UUID) : BehandleGrunnlagResultat()
        object AlleredeVurdert : BehandleGrunnlagResultat()
        object IngenVurderingFunnet : BehandleGrunnlagResultat()
    }

    fun finnOpptjeningsvurderingResultat(opptjeningsvurderingId: UUID): Opptjening {
        return vilkårsvurderingRepository.finn(opptjeningsvurderingId)
            ?: error("Fant ikke opptjeningsvurdering med id $opptjeningsvurderingId")
    }
}
