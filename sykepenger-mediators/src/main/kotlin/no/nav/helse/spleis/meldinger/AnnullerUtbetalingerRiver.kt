package no.nav.helse.spleis.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.toUUID
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.AnnulleringMessage

internal class AnnullerUtbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : Fabrikkelv<Annulleringsmeldingsfabrikk, AnnulleringMessage>(rapidsConnection, messageMediator, Annulleringsmeldingsfabrikk()) {
    override val eventName = "annullering"
    override val riverName = "annullering"
}

internal class Annulleringsmeldingsfabrikk() : Meldingsfabrikk<AnnulleringMessage> {
    override fun validate(message: JsonMessage) {
        message.requireKey(
            "@id",
            "aktørId",
            "fødselsnummer",
            "organisasjonsnummer",
            "saksbehandler",
            "saksbehandler.epostaddresse",
            "saksbehandler.ident",
        )
        message.interestedIn("fagsystemId", "utbetalingId")
    }

    override fun lagMelding(message: JsonMessage): AnnulleringMessage = AnnulleringMessage(
        message,
        aktørId = message["aktørId"].asText(),
        fødselsnummer = message["fødselsnummer"].asText(),
        organisasjonsnummer = message["organisasjonsnummer"].asText(),
        fagsystemId = message["fagsystemId"].takeUnless(JsonNode::isMissingOrNull)?.asText()?.trim(),
        utbetalingId = message["utbetalingId"].takeUnless(JsonNode::isMissingOrNull)?.asText()?.trim()?.toUUID(),
        saksbehandler = AnnulleringMessage.Saksbehandler.fraJson(message["saksbehandler"])
    )
}
