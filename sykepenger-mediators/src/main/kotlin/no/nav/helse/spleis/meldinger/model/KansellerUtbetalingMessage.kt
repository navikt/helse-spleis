package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.KansellerUtbetaling
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class KansellerUtbetalingMessage(packet: MessageDelegate) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val fagsystemId = packet["fagsystemId"].asText()
    private val saksbehandler = packet["saksbehandler"].asText()
    private val kansellerUtbetaling get() = KansellerUtbetaling(
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
