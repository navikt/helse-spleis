package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.DumpVedtaksperioderMessage

internal class DumpVedtaksperioderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "dump_vedtaksperioder"
    override val riverName = "dump_vedtaksperioder"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "aktørId")
    }

    override fun createMessage(packet: JsonMessage) = DumpVedtaksperioderMessage(packet)
}
