package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Utbetalingpåminnelse
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import java.util.UUID

// Understands a JSON message representing a UtbetalingpPåminnelse
internal class UtbetalingpåminnelseMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing
) : HendelseMessage(packet) {
    private val utbetalingId = UUID.fromString(packet["utbetalingId"].asText())
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val antallGangerPåminnet = packet["antallGangerPåminnet"].asInt()
    private val status = packet["status"].asText()
    private val endringstidspunkt = packet["endringstidspunkt"].asLocalDateTime()
    private val påminnelsestidspunkt = packet["@opprettet"].asLocalDateTime()

    private val påminnelse
        get() =
            Utbetalingpåminnelse(
                meldingsreferanseId = meldingsporing.id,
                organisasjonsnummer = organisasjonsnummer,
                utbetalingId = utbetalingId,
                antallGangerPåminnet = antallGangerPåminnet,
                status = Utbetalingstatus.valueOf(status),
                endringstidspunkt = endringstidspunkt,
                påminnelsestidspunkt = påminnelsestidspunkt
            )

    override fun behandle(
        mediator: IHendelseMediator,
        context: MessageContext
    ) {
        mediator.behandle(this, påminnelse, context)
    }
}
