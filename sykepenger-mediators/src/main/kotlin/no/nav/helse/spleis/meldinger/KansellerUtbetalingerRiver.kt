package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.KansellerUtbetalingMessage

internal class KansellerUtbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "kanseller_utbetaling"
    override val riverName = "Kanseller utbetaling"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@id", "aktørId", "fødselsnummer", "organisasjonsnummer", "fagsystemId", "saksbehandler")
    }

    override fun createMessage(packet: JsonMessage) = KansellerUtbetalingMessage(JsonMessageDelegate(packet))
}
