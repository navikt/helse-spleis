package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
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
            "utbetalingId"
        )
    }

    override fun createMessage(packet: JsonMessage) = AnnulleringMessage(packet)
}
