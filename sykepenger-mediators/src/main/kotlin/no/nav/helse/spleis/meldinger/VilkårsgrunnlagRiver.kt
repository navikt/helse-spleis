package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.Toggles.vilkårshåndteringInfotrygd
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage

internal class VilkårsgrunnlagRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov =
        if (!vilkårshåndteringInfotrygd) listOf(InntekterForSammenligningsgrunnlag, EgenAnsatt, Opptjening, Dagpenger, Arbeidsavklaringspenger, Medlemskap)
        else listOf(InntekterForSammenligningsgrunnlag, Opptjening, Dagpenger, Arbeidsavklaringspenger, Medlemskap)

    override val riverName = "Vilkårsgrunnlag"

    override fun validate(packet: JsonMessage) {
        packet.requireArray("@løsning.${InntekterForSammenligningsgrunnlag.name}") {
            require("årMåned", JsonNode::asYearMonth)
            requireArray("inntektsliste") {
                requireKey("beløp")
                requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                interestedIn("orgnummer", "fødselsnummer", "aktørId", "fordel", "beskrivelse")
            }
        }
        packet.interestedIn("@løsning.${Medlemskap.name}.resultat.svar") {
            require(it.asText() in listOf("JA", "NEI", "UAVKLART")) { "svar (${it.asText()}) er ikke JA, NEI eller UAVKLART" }
        }
        if (!vilkårshåndteringInfotrygd) packet.requireKey("@løsning.${EgenAnsatt.name}")
        packet.requireArray("@løsning.${Opptjening.name}") {
            requireKey("orgnummer")
            require("ansattSiden", JsonNode::asLocalDate)
            interestedIn("ansattTil") { it.asLocalDate() }
        }
        packet.requireArray("@løsning.${Dagpenger.name}.meldekortperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        packet.requireArray("@løsning.${Arbeidsavklaringspenger.name}.meldekortperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
    }

    override fun createMessage(packet: JsonMessage) = VilkårsgrunnlagMessage(JsonMessageDelegate(packet))
}
