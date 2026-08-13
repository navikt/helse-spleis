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

        vilkårsvurderingRepository.finnNyesteVilkårsvurdering<Opptjening>(fødselsnummer, skjæringstidspunkt)?.let {
            // do something with the vilkårsvurdering
            // TODO . Vi bør sjekke at logikken på eksisterende vurdering var gjort basert på samme arbeidssiturasjon. Om vi i fremtiden kan endre situasjonen på et skjæringstidspunkt
            meldingssender.sendOpptjeningsvurderingReferanse(fødselsnummer, skjæringstidspunkt, it.id)
        } ?: run {

            // lagre mottatt behov
            opptjeningsbehovRepository.lagre(opptjeningsbehov)

            when (opptjeningsbehov.arbeidssituasjon) {

                Arbeidssituasjon.Arbeidstaker -> {

                    meldingssender.sendArbeidsforholdBehov(fødselsnummer)
                }
                Arbeidssituasjon.SelvstendigNæringsdrivende -> {
                    val vurdering = Opptjening.AutomatiskVurdering.nyAutomatiskVurdering(
                        fødselsnummer = fødselsnummer,
                        skjæringstidspunkt = skjæringstidspunkt,
                        versjonAvKildekode = "",
                        grunnlagForAutomatiskVurdering = OpptjeningsgrunnlagForAutomatiskVurdering.ForSelvstendigNæringsdrivende,
                    )
                    opptjeningsbehov.kvitterUt(vurdering.id)
                    opptjeningsbehovRepository.lagre(opptjeningsbehov)

                    vilkårsvurderingRepository.lagre(vurdering)
                    meldingssender.sendOpptjeningsvurderingReferanse(fødselsnummer, skjæringstidspunkt, vurdering.id)
                }
            }
        }
    }

    fun behandleGrunnlagForAutomatiskArbeidstakerOpptjeningsvurdering(arbeidsforhold: List<Arbeidsforhold>, behovId: UUID, meldingssender: Meldingssender) {
        val behov = opptjeningsbehovRepository.finnUbesvart(behovId) ?: return

        val vurdering = Opptjening.AutomatiskVurdering.nyAutomatiskVurdering(
            fødselsnummer = behov.fødselsnummer,
            skjæringstidspunkt = behov.skjæringstidspunkt,
            versjonAvKildekode = "", //TODO: hent versjon av kildekode
            grunnlagForAutomatiskVurdering = OpptjeningsgrunnlagForAutomatiskVurdering.ForArbeidstaker(arbeidsforhold = arbeidsforhold)
        )

        behov.kvitterUt(vurdering.id)

        vilkårsvurderingRepository.lagre(vurdering)
        opptjeningsbehovRepository.lagre(behov)

        meldingssender.sendOpptjeningsvurderingReferanse(behov.fødselsnummer, behov.skjæringstidspunkt, vurdering.id)

    }
}
