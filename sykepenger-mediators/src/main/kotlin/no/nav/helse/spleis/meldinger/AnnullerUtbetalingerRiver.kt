package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.AnnulleringMessage

internal class AnnullerUtbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "annullering"
    override val riverName = "annullering"

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

    override fun createMessage(packet: JsonMessage) = AnnulleringMessage(packet)
}
