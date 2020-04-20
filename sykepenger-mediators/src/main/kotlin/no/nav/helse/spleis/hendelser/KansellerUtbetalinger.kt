package no.nav.helse.spleis.hendelser

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.hendelser.model.KansellerUtbetalingMessage

internal class KansellerUtbetalinger(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "kanseller_utbetaling"
    override val riverName = "Kanseller utbetaling"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@id", "aktørId", "fødselsnummer", "organisasjonsnummer", "fagsystemId", "saksbehandler")
    }

    override fun createMessage(packet: JsonMessage) = KansellerUtbetalingMessage(packet)
}
