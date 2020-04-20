package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.KansellerUtbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.hendelser.MessageProcessor

internal class KansellerUtbetalingMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val fagsystemId = packet["fagsystemId"].asText()
    private val saksbehandler = packet["saksbehandler"].asText()

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asKansellerUtbetaling() = KansellerUtbetaling(
        aktørId,
        fødselsnummer,
        organisasjonsnummer,
        fagsystemId,
        saksbehandler
    )
}
