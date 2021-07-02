package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(packet: JsonMessage) : BehovMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()

    private val inntekterForSammenligningsgrunnlag = packet["@løsning.${InntekterForSammenligningsgrunnlag.name}"]
        .flatMap { måned ->
            måned["inntektsliste"]
                .groupBy({ inntekt -> inntekt.arbeidsgiver() }) { inntekt ->
                    ArbeidsgiverInntekt.MånedligInntekt.Sammenligningsgrunnlag(
                        yearMonth = måned["årMåned"].asYearMonth(),
                        inntekt = inntekt["beløp"].asDouble().månedlig,
                        type = inntekt["inntektstype"].asInntekttype(),
                        fordel = if (inntekt.path("fordel").isTextual) inntekt["fordel"].asText() else "",
                        beskrivelse = if (inntekt.path("beskrivelse").isTextual) inntekt["beskrivelse"].asText() else ""
                    )
                }.toList()
        }
        .groupBy({ (arbeidsgiver, _) -> arbeidsgiver }) { (_, inntekter) -> inntekter }
        .map { (arbeidsgiver, inntekter) ->
            ArbeidsgiverInntekt(arbeidsgiver, inntekter.flatten())
        }

    private val arbeidsforhold = packet["@løsning.${Opptjening.name}"].map {
        Arbeidsforhold(
            orgnummer = it["orgnummer"].asText(),
            fom = it["ansattSiden"].asLocalDate(),
            tom = it["ansattTil"].asOptionalLocalDate()
        )
    }

    private val medlemskapstatus = when (packet["@løsning.${Medlemskap.name}.resultat.svar"].asText()) {
        "JA" -> Medlemskapsvurdering.Medlemskapstatus.Ja
        "NEI" -> Medlemskapsvurdering.Medlemskapstatus.Nei
        else -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
    }

    private val vilkårsgrunnlag
        get() = Vilkårsgrunnlag(
            meldingsreferanseId = this.id,
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntekterForSammenligningsgrunnlag
            ),
            opptjeningvurdering = Opptjeningvurdering(
                arbeidsforhold = arbeidsforhold
            ),
            medlemskapsvurdering = Medlemskapsvurdering(
                medlemskapstatus = medlemskapstatus
            )
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, vilkårsgrunnlag)
    }

    companion object {

        internal fun JsonNode.asInntekttype() = when (this.asText()) {
            "LOENNSINNTEKT" -> Inntekttype.LØNNSINNTEKT
            "NAERINGSINNTEKT" -> Inntekttype.NÆRINGSINNTEKT
            "PENSJON_ELLER_TRYGD" -> Inntekttype.PENSJON_ELLER_TRYGD
            "YTELSE_FRA_OFFENTLIGE" -> Inntekttype.YTELSE_FRA_OFFENTLIGE
            else -> error("Kunne ikke mappe Inntekttype")
        }

        internal fun JsonNode.arbeidsgiver() = when {
            path("orgnummer").isTextual -> path("orgnummer").asText()
            path("fødselsnummer").isTextual -> path("fødselsnummer").asText()
            path("aktørId").isTextual -> path("aktørId").asText()
            else -> error("Mangler arbeidsgiver for inntekt i hendelse")
        }
    }
}
