package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.*
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.VilkårsgrunnlagMessage

internal class Vilkårsgrunnlag(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("@id", "@behov", "@final", "@løsning",
                    "aktørId", "fødselsnummer",
                    "organisasjonsnummer", "vedtaksperiodeId",
                    "tilstand"
                )
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.require("@besvart", JsonNode::asLocalDateTime)
                it.requireValue("@final", true)

                it.demandAll("@behov", Inntektsberegning, EgenAnsatt, Opptjening, Dagpenger, Arbeidsavklaringspenger)
                it.requireArray("@løsning.${Inntektsberegning.name}") {
                    require("årMåned", JsonNode::asYearMonth)
                    requireArray("inntektsliste") {
                        requireKey("beløp")
                        requireAny("inntektstype", listOf("LOENNSINNTEKT", "NAERINGSINNTEKT", "PENSJON_ELLER_TRYGD", "YTELSE_FRA_OFFENTLIGE"))
                        interestedIn("orgnummer")
                    }
                }
                it.requireKey("@løsning.${EgenAnsatt.name}")
                it.requireArray("@løsning.${Opptjening.name}") {
                    requireKey("orgnummer")
                    require("ansattSiden", JsonNode::asLocalDate)
                    interestedIn("ansattTil") { it.asLocalDate() }
                }
                it.requireArray("@løsning.${Dagpenger.name}") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                }
                it.requireArray("@løsning.${Arbeidsavklaringspenger.name}") {
                    require("fom", JsonNode::asLocalDate)
                    require("tom", JsonNode::asLocalDate)
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(VilkårsgrunnlagMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Vilkårsgrunnlag", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Vilkårsgrunnlag", problems, context)
    }
}
