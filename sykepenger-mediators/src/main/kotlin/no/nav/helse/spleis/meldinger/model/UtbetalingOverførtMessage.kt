package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.UtbetalingOverført
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator

internal class UtbetalingOverførtMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val avstemmingsnøkkel = packet["avstemmingsnøkkel"].asLong()
    private val overføringstidspunkt = packet["overføringstidspunkt"].asLocalDateTime()

    private val utbetaling = UtbetalingOverført(
        vedtaksperiodeId = vedtaksperiodeId,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        orgnummer = organisasjonsnummer,
        avstemmingsnøkkel = avstemmingsnøkkel,
        overføringstidspunkt = overføringstidspunkt
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetaling)
    }
}
