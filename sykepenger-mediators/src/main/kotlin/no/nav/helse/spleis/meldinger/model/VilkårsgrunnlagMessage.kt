package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.ArbeidsgiverInntekt.MånedligInntekt.Inntekttype
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsforholdV2
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Medlemskap
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing a Vilkårsgrunnlagsbehov
internal class VilkårsgrunnlagMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()

    private val inntekterForSykepengegrunnlag = mapSkatteopplysninger(packet["@løsning.${InntekterForSykepengegrunnlag.name}"])

    private val inntekterForOpptjeningsvurdering = mapSkatteopplysninger(packet["@løsning.${Aktivitet.Behov.Behovtype.InntekterForOpptjeningsvurdering.name}"])
    private val arbeidsforhold = packet["@løsning.${ArbeidsforholdV2.name}"]
        .filterNot { it["orgnummer"].asText().isBlank() }
        .filter {
            val til = it["ansattTil"].asOptionalLocalDate()
            til == null || it["ansattSiden"].asLocalDate() <= til
        }
        .map {
            Vilkårsgrunnlag.Arbeidsforhold(
                orgnummer = it["orgnummer"].asText(),
                ansattFom = it["ansattSiden"].asLocalDate(),
                ansattTom = it["ansattTil"].asOptionalLocalDate(),
                type = when (it["type"].asText()) {
                    "FORENKLET_OPPGJØRSORDNING" -> Arbeidsforholdtype.FORENKLET_OPPGJØRSORDNING
                    "FRILANSER" -> Arbeidsforholdtype.FRILANSER
                    "MARITIMT" -> Arbeidsforholdtype.MARITIMT
                    "ORDINÆRT" -> Arbeidsforholdtype.ORDINÆRT
                    else -> error("har ikke mappingregel for arbeidsforholdtype: ${it["type"].asText()}")
                }
            )
        }

    private val medlemskapstatus = when (packet["@løsning.${Medlemskap.name}.resultat.svar"].asText()) {
        "JA" -> Medlemskapsvurdering.Medlemskapstatus.Ja
        "NEI" -> Medlemskapsvurdering.Medlemskapstatus.Nei
        "UAVKLART_MED_BRUKERSPORSMAAL" -> Medlemskapsvurdering.Medlemskapstatus.UavklartMedBrukerspørsmål
        else -> Medlemskapsvurdering.Medlemskapstatus.VetIkke
    }

    private val skjæringstidspunkter = listOfNotNull(
        packet["${InntekterForSykepengegrunnlag.name}.skjæringstidspunkt"].asLocalDate(),
        packet["${Aktivitet.Behov.Behovtype.InntekterForOpptjeningsvurdering.name}.skjæringstidspunkt"].asLocalDate(),
        packet["${ArbeidsforholdV2.name}.skjæringstidspunkt"].asLocalDate(),
        packet["${Medlemskap.name}.skjæringstidspunkt"].asLocalDate(),
    )

    private val vilkårsgrunnlag
        get() = Vilkårsgrunnlag(
            meldingsreferanseId = meldingsporing.id,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkter.distinct().single(),
            orgnummer = organisasjonsnummer,
            medlemskapsvurdering = Medlemskapsvurdering(
                medlemskapstatus = medlemskapstatus
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = inntekterForSykepengegrunnlag
            ),
            inntekterForOpptjeningsvurdering = no.nav.helse.hendelser.InntekterForOpptjeningsvurdering(inntekter = inntekterForOpptjeningsvurdering),
            arbeidsforhold = arbeidsforhold
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, vilkårsgrunnlag, context)
    }

    internal companion object {

        private fun JsonNode.asInntekttype() = when (this.asText()) {
            "LOENNSINNTEKT" -> Inntekttype.LØNNSINNTEKT
            "NAERINGSINNTEKT" -> Inntekttype.NÆRINGSINNTEKT
            "PENSJON_ELLER_TRYGD" -> Inntekttype.PENSJON_ELLER_TRYGD
            "YTELSE_FRA_OFFENTLIGE" -> Inntekttype.YTELSE_FRA_OFFENTLIGE
            else -> error("Kunne ikke mappe Inntekttype")
        }

        private fun JsonNode.arbeidsgiver() = when {
            path("orgnummer").isTextual -> path("orgnummer").asText()
            path("fødselsnummer").isTextual -> path("fødselsnummer").asText()
            else -> error("Mangler arbeidsgiver for inntekt i hendelse")
        }

        fun mapSkatteopplysninger(opplysninger: JsonNode) =
            opplysninger.flatMap { måned ->
                måned["inntektsliste"].map { opplysning ->
                    (opplysning as ObjectNode).put("årMåned", måned.path("årMåned").asText())
                }
            }
                .groupBy({ inntekt -> inntekt.arbeidsgiver() }) { inntekt ->
                    ArbeidsgiverInntekt.MånedligInntekt(
                        yearMonth = inntekt["årMåned"].asYearMonth(),
                        inntekt = inntekt["beløp"].asDouble().månedlig,
                        type = inntekt["inntektstype"].asInntekttype(),
                        fordel = if (inntekt.path("fordel").isTextual) inntekt["fordel"].asText() else "",
                        beskrivelse = if (inntekt.path("beskrivelse").isTextual) inntekt["beskrivelse"].asText() else ""
                    )
                }
                .map { (arbeidsgiver, inntekter) ->
                    ArbeidsgiverInntekt(arbeidsgiver, inntekter)
                }
    }
}
