package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Godkjenning-behov
internal class UtbetalingsgodkjenningMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val saksbehandler = packet.saksbehandler()
    private val godkjenttidspunkt = packet.godkjenttidspunkt()
    private val utbetalingGodkjent = packet["@løsning.${Godkjenning.name}.godkjent"].asBoolean()

    private val utbetalingsgodkjenning = Utbetalingsgodkjenning(
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        saksbehandler = saksbehandler,
        godkjenttidspunkt = godkjenttidspunkt,
        utbetalingGodkjent = utbetalingGodkjent
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingsgodkjenning)
    }
}

private fun JsonMessage.saksbehandler() =
    (this["@løsning"][Godkjenning.name]["saksbehandlerIdent"]?.takeUnless { it.isMissingOrNull() } ?:
    this["saksbehandlerIdent"])
        .asText()

private fun JsonMessage.godkjenttidspunkt() =
    (this["@løsning"][Godkjenning.name]["godkjenttidspunkt"]?.takeUnless { it.isMissingOrNull() } ?:
    this["godkjenttidspunkt"])
        .asLocalDateTime()
