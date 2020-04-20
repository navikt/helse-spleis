package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.ManuellSaksbehandling
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Manuell saksbehandling-behov
internal class ManuellSaksbehandlingMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val saksbehandler = packet["saksbehandlerIdent"].asText()
    private val godkjenttidspunkt = packet["godkjenttidspunkt"].asLocalDateTime()
    private val utbetalingGodkjent = packet["@løsning.${Godkjenning.name}.godkjent"].asBoolean()

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asManuellSaksbehandling() =
        ManuellSaksbehandling(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandler = saksbehandler,
            godkjenttidspunkt = godkjenttidspunkt,
            utbetalingGodkjent = utbetalingGodkjent
        )
}
