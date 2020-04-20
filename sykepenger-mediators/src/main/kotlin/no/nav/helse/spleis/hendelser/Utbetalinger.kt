package no.nav.helse.spleis.hendelser

import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.hendelser.model.UtbetalingMessage

internal class Utbetalinger(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Utbetaling)
    override val riverName = "Utbetaling"

    override fun validate(packet: JsonMessage) {
        packet.requireKey("@løsning.${Utbetaling.name}")
        // skip OVERFØRT; we don't need to react to it (yet)
        packet.requireAny("@løsning.${Utbetaling.name}.status", Oppdragstatus.values().filterNot { it == Oppdragstatus.OVERFØRT }.map(Enum<*>::name))
        packet.requireKey("@løsning.${Utbetaling.name}.beskrivelse")
        packet.requireKey("fagsystemId")
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingMessage(packet)
}
