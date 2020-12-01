package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.UtbetalingOverført
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class UtbetalingOverførtMessage(packet: MessageDelegate) : BehovMessage(packet) {
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val aktørId = packet["aktørId"].asText()
    private val fagsystemId = packet["${Utbetaling.name}.fagsystemId"].asText()
    private val utbetalingId = packet["utbetalingId"].asText()
    private val avstemmingsnøkkel = packet["@løsning.${Utbetaling.name}.avstemmingsnøkkel"].asLong()
    private val overføringstidspunkt = packet["@løsning.${Utbetaling.name}.overføringstidspunkt"].asLocalDateTime()

    private val utbetaling
        get() = UtbetalingOverført(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId,
            avstemmingsnøkkel = avstemmingsnøkkel,
            overføringstidspunkt = overføringstidspunkt
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetaling)
    }
}
