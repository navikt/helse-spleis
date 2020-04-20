package no.nav.helse.spleis.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.MessageMediator
import no.nav.helse.spleis.hendelser.model.UtbetalingMessage

internal class Utbetalinger(rapidsConnection: RapidsConnection, private val messageMediator: MessageMediator) :
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

                it.demandAll("@behov", Utbetaling)
                it.requireKey("@løsning.${Utbetaling.name}")
                // skip OVERFØRT; we don't need to react to it
                it.requireAny("@løsning.${Utbetaling.name}.status", Oppdragstatus.values().filterNot { it == Oppdragstatus.OVERFØRT }.map(Enum<*>::name))
                it.requireKey("@løsning.${Utbetaling.name}.beskrivelse")
                it.requireKey("fagsystemId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        messageMediator.onRecognizedMessage(UtbetalingMessage(packet), context)
    }

    override fun onSevere(error: MessageProblems.MessageException, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverSevere("Utbetalinger", error, context)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        messageMediator.onRiverError("Utbetalinger", problems, context)
    }
}
