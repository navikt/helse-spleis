package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.KansellerUtbetaling
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class KansellerUtbetalingMessage(packet: MessageDelegate) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val fagsystemId = packet["fagsystemId"].asText()
    private val saksbehandler = packet["saksbehandler"].asText()
    private val saksbehandlerEpost = packet["saksbehandlerEpost"].asText()
    private val kansellerUtbetaling get() = KansellerUtbetaling(
        aktørId,
        fødselsnummer,
        organisasjonsnummer,
        fagsystemId,
        saksbehandler,
        saksbehandlerEpost,
        opprettet
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, kansellerUtbetaling)
    }
}
