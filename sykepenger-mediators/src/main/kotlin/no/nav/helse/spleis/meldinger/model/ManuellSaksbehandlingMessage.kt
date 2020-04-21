package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.ManuellSaksbehandling
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Manuell saksbehandling-behov
internal class ManuellSaksbehandlingMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val saksbehandler = packet["saksbehandlerIdent"].asText()
    private val godkjenttidspunkt = packet["godkjenttidspunkt"].asLocalDateTime()
    private val utbetalingGodkjent = packet["@løsning.${Godkjenning.name}.godkjent"].asBoolean()

    private val manuellSaksbehandling = ManuellSaksbehandling(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        saksbehandler = saksbehandler,
        godkjenttidspunkt = godkjenttidspunkt,
        utbetalingGodkjent = utbetalingGodkjent
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, manuellSaksbehandling)
    }
}
