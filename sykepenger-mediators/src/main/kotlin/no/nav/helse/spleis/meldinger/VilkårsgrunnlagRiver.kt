package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage

internal class VilkårsgrunnlagRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(InntekterForSammenligningsgrunnlag, InntekterForSykepengegrunnlag, ArbeidsforholdV2, Medlemskap)

    override val riverName = "Vilkårsgrunnlag"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId", "tilstand")
        message.requireArray("@løsning.${InntekterForSammenligningsgrunnlag.name}") {
            require("årMåned", JsonNode::asYearMonth)
            requireArray("inntektsliste") {
                requireKey("beløp")
                requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                interestedIn("orgnummer", "fødselsnummer", "aktørId", "fordel", "beskrivelse")
            }
        }
        message.interestedIn("@løsning.${Medlemskap.name}.resultat.svar") {
            require(it.asText() in listOf("JA", "NEI", "UAVKLART")) { "svar (${it.asText()}) er ikke JA, NEI eller UAVKLART" }
        }
        message.requireArray("@løsning.${InntekterForSykepengegrunnlag.name}") {
            require("årMåned", JsonNode::asYearMonth)
            requireArray("inntektsliste") {
                requireKey("beløp")
                requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                interestedIn("orgnummer", "fødselsnummer", "aktørId", "fordel", "beskrivelse")
            }
            requireArray("arbeidsforholdliste") {
                requireKey("orgnummer", "type")
            }
        }
        message.requireArray("@løsning.${ArbeidsforholdV2.name}"){
            requireKey("orgnummer")
            require("ansattSiden", JsonNode::asLocalDate)
            interestedIn("ansattTil", JsonNode::asLocalDate)
        }
    }

    override fun createMessage(packet: JsonMessage) = VilkårsgrunnlagMessage(packet)
}
