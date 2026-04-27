package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asYearMonth
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.Behov.Behovstype.Arbeidsforhold
import no.nav.helse.spleis.Behov.Behovstype.InntekterForOpptjeningsvurdering
import no.nav.helse.spleis.Behov.Behovstype.InntekterForSykepengegrunnlag
import no.nav.helse.spleis.Behov.Behovstype.Medlemskap
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage

internal class VilkårsgrunnlagRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : ArbeidsgiverBehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(
        InntekterForSykepengegrunnlag,
        InntekterForOpptjeningsvurdering,
        Arbeidsforhold,
        Medlemskap
    )

    override val riverName = "Vilkårsgrunnlag"

    override fun validate(message: JsonMessage) {
        message.requireKey("vedtaksperiodeId")
        message.require("${InntekterForSykepengegrunnlag.utgåendeNavn}.skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("${InntekterForOpptjeningsvurdering.utgåendeNavn}.skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("${Arbeidsforhold.utgåendeNavn}.skjæringstidspunkt", JsonNode::asLocalDate)
        message.require("${Medlemskap.utgåendeNavn}.skjæringstidspunkt", JsonNode::asLocalDate)
        message.interestedIn("@løsning.${Medlemskap.utgåendeNavn}.resultat.svar") {
            require(it.asText() in listOf("JA", "NEI", "UAVKLART", "UAVKLART_MED_BRUKERSPORSMAAL")) { "svar (${it.asText()}) er ikke JA, NEI, UAVKLART, eller UAVKLART_MED_BRUKERSPORSMAAL" }
        }
        message.requireArrayEllerObjectMedArray("@løsning.${InntekterForSykepengegrunnlag.utgåendeNavn}", "inntekter") {
            require("årMåned", JsonNode::asYearMonth)
            requireArray("inntektsliste") {
                requireKey("beløp")
                requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                interestedIn("orgnummer", "fødselsnummer", "fordel", "beskrivelse")
            }
        }
        message.requireArrayEllerObjectMedArray("@løsning.${InntekterForOpptjeningsvurdering.utgåendeNavn}", "inntekter") {
            require("årMåned", JsonNode::asYearMonth)
            requireArray("inntektsliste") {
                requireKey("beløp")
                requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                interestedIn("orgnummer", "fødselsnummer", "fordel", "beskrivelse")
            }
        }
        message.requireArrayEllerObjectMedArray("@løsning.${Arbeidsforhold.utgåendeNavn}", "arbeidsforhold") {
            requireKey("orgnummer")
            requireAny("type", listOf("FORENKLET_OPPGJØRSORDNING", "FRILANSER", "MARITIMT", "ORDINÆRT"))
            require("ansattSiden", JsonNode::asLocalDate)
            interestedIn("ansattTil", JsonNode::asLocalDate)
        }
    }

    override fun createMessage(packet: JsonMessage) = VilkårsgrunnlagMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fødselsnummer"].asText()
        )
    )
}
