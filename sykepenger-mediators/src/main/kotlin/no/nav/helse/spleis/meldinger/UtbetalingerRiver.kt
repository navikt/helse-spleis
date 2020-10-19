package no.nav.helse.spleis.meldinger

import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.JsonMessageDelegate
import no.nav.helse.spleis.meldinger.model.UtbetalingMessage

internal class UtbetalingerRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Utbetaling)
    override val riverName = "Utbetaling"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@løsning.${Utbetaling.name}")
        // skip OVERFØRT; we don't need to react to it (yet)
        packet.requireAny(
            "@løsning.${Utbetaling.name}.status",
            Oppdragstatus.values().filterNot { it == Oppdragstatus.OVERFØRT }.map(Enum<*>::name)
        )
        packet.requireKey(
            "@løsning.${Utbetaling.name}.beskrivelse",
            "saksbehandler",
            "saksbehandlerEpost",
            "godkjenttidspunkt",
            "annullering"
        )
        packet.requireKey("fagsystemId")
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingMessage(JsonMessageDelegate(packet))
}
