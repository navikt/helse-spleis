package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.FeriepengeutbetalingHendelse
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Feriepengeutbetaling
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.utbetalingslinjer.Oppdragstatus

internal class FeriepengeutbetalingMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val fagsystemId = packet["${Feriepengeutbetaling.name}.fagsystemId"].asText().trim()
    private val utbetalingId = packet["utbetalingId"].asText().toUUID()

    private val status: Oppdragstatus = enumValueOf(packet["@løsning.${Feriepengeutbetaling.name}.status"].asText())
    private val beskrivelse = packet["@løsning.${Feriepengeutbetaling.name}.beskrivelse"].asText()
    private val avstemmingsnøkkel = packet["@løsning.${Feriepengeutbetaling.name}.avstemmingsnøkkel"].asLong()
    private val overføringstidspunkt = packet["@løsning.${Feriepengeutbetaling.name}.overføringstidspunkt"].asLocalDateTime()

    private val utbetaling
        get() = FeriepengeutbetalingHendelse(
            meldingsreferanseId = meldingsporing.id,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
            fagsystemId = fagsystemId,
            utbetalingId = utbetalingId,
            status = status,
            melding = beskrivelse,
            avstemmingsnøkkel = avstemmingsnøkkel,
            overføringstidspunkt = overføringstidspunkt
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        if (status == Oppdragstatus.OVERFØRT) return // sender bare inn kvitteringer til modellen
        mediator.behandle(this, utbetaling, context)
    }
}
