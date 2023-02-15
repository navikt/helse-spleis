package no.nav.helse.spleis.meldinger

import java.time.LocalDateTime
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.UtbetalingshistorikkRiver.Companion.validerSykepengehistorikk
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkEtterInfotrygdendringMessage

internal class UtbetalingshistorikkEtterInfotrygdendringRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Sykepengehistorikk)
    override val riverName = "Utbetalingshistorikk etter infotrygdendring"

    override fun validate(message: JsonMessage) {
        message.demand("@behov") { require(it.size() == 1) }
        message.demand("@besvart") { require(it.asLocalDateTime() > LocalDateTime.now().minusHours(1)) }
        message.rejectKey("fagsystemId", "vedtaksperiodeId")
        validerSykepengehistorikk(message)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingshistorikkEtterInfotrygdendringMessage(packet)
}
