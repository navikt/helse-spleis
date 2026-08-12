package no.nav.helse.opptjening.application

import java.util.UUID
import no.nav.helse.opptjening.domain.Arbeidsforhold
import no.nav.helse.opptjening.domain.Arbeidssituasjon
import no.nav.helse.opptjening.domain.Opptjening
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering

class OpptjeningService(private val vilkårsvurderingRepository: VilkårsvurderingRepository, private val opptjeningsbehovRepository: OpptjeningsbehovRepository) {
    fun vurderOpptjening(opptjeningsbehov: Opptjeningsbehov, meldingssender: Meldingssender) {
        val fødselsnummer = opptjeningsbehov.fødselsnummer
        val skjæringstidspunkt = opptjeningsbehov.skjæringstidspunkt
        vilkårsvurderingRepository.finnVilkårsvurdering<Opptjening>(fødselsnummer, skjæringstidspunkt)?.let {
            // do something with the vilkårsvurdering
            meldingssender.sendOpptjeningsløsning(fødselsnummer, skjæringstidspunkt, it)
        } ?: run {
            // lagre mottatt behov
            when (opptjeningsbehov.arbeidssituasjon) {
                Arbeidssituasjon.Arbeidstaker -> {
                    opptjeningsbehovRepository.lagre(opptjeningsbehov)
                    meldingssender.sendOpptjeningsgrunnlagBehov(fødselsnummer, skjæringstidspunkt)
                }
                Arbeidssituasjon.SelvstendigNæringsdrivende -> {
                    val vurdering = Opptjening.AutomatiskVurdering.nyAutomatiskVurdering(
                        fødselsnummer = fødselsnummer,
                        skjæringstidspunkt = skjæringstidspunkt,
                        versjonAvKildekode = "",
                        grunnlagForAutomatiskVurdering = OpptjeningsgrunnlagForAutomatiskVurdering.ForSelvstendigNæringsdrivende
                    )
                    vilkårsvurderingRepository.lagre(vurdering)
                    meldingssender.sendOpptjeningsløsning(fødselsnummer, skjæringstidspunkt, vurdering)
                }
            }
        }
    }

    fun behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(arbeidsforhold: List<Arbeidsforhold>, behovId: UUID) {
        val behov = opptjeningsbehovRepository.finnUbesvart(behovId) ?: return

        val vurdering = Opptjening.AutomatiskVurdering.nyAutomatiskVurdering(
            fødselsnummer = behov.fødselsnummer,
            skjæringstidspunkt = behov.skjæringstidspunkt,
            versjonAvKildekode = "",
            grunnlagForAutomatiskVurdering = OpptjeningsgrunnlagForAutomatiskVurdering.ForArbeidstaker(arbeidsforhold = arbeidsforhold)
        )
    }
}
