package no.nav.helse.spleis.meldinger

import java.util.UUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.AnmodningOmForkastingMessage

internal open class AnmodningOmForkastingRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : Fabrikkelv<AnmodningOmForkastingMessagefabrikk, AnmodningOmForkastingMessage>(
    rapidsConnection = rapidsConnection,
    messageMediator = messageMediator,
    fabrikk = AnmodningOmForkastingMessagefabrikk()
) {
    override val eventName = "anmodning_om_forkasting"
    override val riverName = "anmodningOmForkasting"
}

internal class AnmodningOmForkastingMessagefabrikk: Meldingsfabrikk<AnmodningOmForkastingMessage> {
    override fun validate(message: JsonMessage) {
        message.requireKey(
            "organisasjonsnummer",
            "fødselsnummer",
            "aktørId"
        )
        message.require("vedtaksperiodeId") { UUID.fromString(it.asText()) }
        message.interestedIn("force")
    }

    override fun lagMelding(message: JsonMessage): AnmodningOmForkastingMessage {
        val vedtaksperiodeId = message["vedtaksperiodeId"].asText().let { UUID.fromString(it) }
        val organisasjonsnummer = message["organisasjonsnummer"].asText()
        val aktørId = message["aktørId"].asText()
        val fødselsnummer: String = message["fødselsnummer"].asText()
        val force = message["force"].takeIf { it.isBoolean }?.asBoolean() ?: false
        return AnmodningOmForkastingMessage(message, vedtaksperiodeId, organisasjonsnummer, aktørId, fødselsnummer, force)
    }
}

