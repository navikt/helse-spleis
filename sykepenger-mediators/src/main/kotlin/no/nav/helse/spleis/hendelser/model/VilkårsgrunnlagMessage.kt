package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Opptjeningvurdering
import no.nav.helse.hendelser.Periode
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
        val (dagpenger, ugyldigeDagpengeperioder) = this["@løsning.${Dagpenger.name}"]
            .map(::asDatePair)
            .partition { it.first <= it.second }
        val (arbeidsavklaringspenger, ugyldigeArbeidsavklaringspengeperioder) = this["@løsning.${Arbeidsavklaringspenger.name}"].map(::asDatePair)
            .partition { it.first <= it.second }
        return Vilkårsgrunnlag(
            vedtaksperiodeId = this["vedtaksperiodeId"].asText(),
            aktørId = this["aktørId"].asText(),
            fødselsnummer = fødselsnummer,
            orgnummer = this["organisasjonsnummer"].asText(),
            inntektsvurdering = Inntektsvurdering(
                perioder = this["@løsning.${Inntektsberegning.name}"].map {
                    it["årMåned"].asYearMonth() to it["inntektsliste"].map { it["beløp"].asDouble() }
                }.associate { it }
            ),
            opptjeningvurdering = Opptjeningvurdering(
                arbeidsforhold = this["@løsning.${Opptjening.name}"].map {
                    Opptjeningvurdering.Arbeidsforhold(
                        orgnummer = it["orgnummer"].asText(),
                        fom = it["ansattSiden"].asLocalDate(),
                        tom = it["ansattTil"].asOptionalLocalDate()
                    )
                }
            ),
            erEgenAnsatt = this["@løsning.${EgenAnsatt.name}"].asBoolean(),
            dagpenger = no.nav.helse.hendelser.Dagpenger(dagpenger.map {
                Periode(
                    it.first,
                    it.second
                )
            }),
            arbeidsavklaringspenger = no.nav.helse.hendelser.Arbeidsavklaringspenger(
                arbeidsavklaringspenger.map { Periode(it.first, it.second) })
        ).also {
            if (ugyldigeDagpengeperioder.isNotEmpty()) it.warn("Arena inneholdt en eller flere Dagpengeperioder med ugyldig fom/tom")
            if (ugyldigeArbeidsavklaringspengeperioder.isNotEmpty()) it.warn("Arena inneholdt en eller flere AAP-perioder med ugyldig fom/tom")
        }
    }

    object Factory : MessageFactory<VilkårsgrunnlagMessage> {
        override fun createMessage(message: String, problems: MessageProblems) =
            VilkårsgrunnlagMessage(message, problems)
    }

    private fun asDatePair(jsonNode: JsonNode) =
        jsonNode.path("fom").asLocalDate() to jsonNode.path("tom").asLocalDate()
}
