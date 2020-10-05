package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

// Understands a JSON message representing a Godkjenning-behov
internal class UtbetalingsgodkjenningMessage(packet: MessageDelegate) : BehovMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val saksbehandler = packet["@løsning.${Godkjenning.name}.saksbehandlerIdent"].asText()
    private val godkjenttidspunkt = packet["@løsning.${Godkjenning.name}.godkjenttidspunkt"].asLocalDateTime()
    private val utbetalingGodkjent = packet["@løsning.${Godkjenning.name}.godkjent"].asBoolean()
    private val automatiskBehandling = packet["@løsning.${Godkjenning.name}.automatiskBehandling"].asBoolean()

    private val utbetalingsgodkjenning
        get() = Utbetalingsgodkjenning(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandler = saksbehandler,
            godkjenttidspunkt = godkjenttidspunkt,
            utbetalingGodkjent = utbetalingGodkjent,
            automatiskBehandling = automatiskBehandling
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingsgodkjenning)
    }
}
