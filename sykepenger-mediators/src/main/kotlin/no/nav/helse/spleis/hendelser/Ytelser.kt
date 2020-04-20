package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.YtelserMessage

internal class Ytelser(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
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

                it.demandAll("@behov", Sykepengehistorikk, Foreldrepenger)
                it.requireKey("@løsning.${Foreldrepenger.name}")
                it.requireKey("@løsning.${Sykepengehistorikk.name}")
                it.interestedIn("@løsning.${Foreldrepenger.name}.Foreldrepengeytelse")
                it.interestedIn("@løsning.${Foreldrepenger.name}.Svangerskapsytelse")
                it.requireArray("@løsning.${Sykepengehistorikk.name}") {
                    requireArray("inntektsopplysninger") {
                        require("sykepengerFom", JsonNode::asLocalDate)
                        requireKey("inntekt", "orgnummer")
                    }
                    requireArray("utbetalteSykeperioder") {
                        interestedIn("fom") { it.asLocalDate() }
                        interestedIn("tom") { it.asLocalDate() }
                        requireAny("typeKode", listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "O", "S", ""))
                    }
                }
            }

        }.register(this)
    }


    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(YtelserMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Ytelser", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Ytelser", problems, context)
    }
}
