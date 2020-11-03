package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.EtterbetalingMessage

internal class EtterbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "Etterbetalingskandidat_v1"
    override val riverName = "Kandidat for etterbetaling"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("aktørId", "fødselsnummer", "fagsystemId", "organisasjonsnummer", "gyldighetsdato")
    }

    override fun createMessage(packet: JsonMessage) = EtterbetalingMessage(JsonMessageDelegate(packet))
}
