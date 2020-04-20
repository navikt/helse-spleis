package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.KansellerUtbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

internal class KansellerUtbetalingMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val fagsystemId = packet["fagsystemId"].asText()
    private val saksbehandler = packet["saksbehandler"].asText()
    private val kansellerUtbetaling = KansellerUtbetaling(
        aktørId,
        fødselsnummer,
        organisasjonsnummer,
        fagsystemId,
        saksbehandler
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, kansellerUtbetaling)
    }
}
