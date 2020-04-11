package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(originalMessage: String, problems: MessageProblems) : BehovMessage(originalMessage, problems) {
    init {
        requireAll("@behov", Inntektsberegning, EgenAnsatt, Opptjening)
        requireKey("@løsning.${Inntektsberegning.name}")
        requireKey("@løsning.${EgenAnsatt.name}")
        requireKey("@løsning.${Opptjening.name}")
        requireKey("@løsning.${Dagpenger.name}")
        requireKey("@løsning.${Arbeidsavklaringspenger.name}")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asVilkårsgrunnlag(): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = fødselsnummer,
            orgnummer = this["organisasjonsnummer"].asText(),
            inntektsvurdering = Vilkårsgrunnlag.Inntektsvurdering(
                perioder = this["@løsning.${Inntektsberegning.name}"].map {
                    it["årMåned"].asYearMonth() to it["inntektsliste"].map { it["beløp"].asDouble() }
                }.associate { it }
            ),
            arbeidsforhold = this["@løsning.${Opptjening.name}"].map {
                Vilkårsgrunnlag.Arbeidsforhold(
                    orgnummer = it["orgnummer"].asText(),
                    fom = it["ansattSiden"].asLocalDate(),
                    tom = it["ansattTil"].asOptionalLocalDate()
                )
            },
            erEgenAnsatt = this["@løsning.${EgenAnsatt.name}"].asBoolean()
        )
    }

    object Factory : MessageFactory<VilkårsgrunnlagMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            VilkårsgrunnlagMessage(message, problems)
    }
}
