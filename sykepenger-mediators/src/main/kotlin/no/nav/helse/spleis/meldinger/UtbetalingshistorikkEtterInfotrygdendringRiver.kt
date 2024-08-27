package no.nav.helse.spleis.meldinger

import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
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
        message.rejectKey("fagsystemId", "vedtaksperiodeId")
        validerSykepengehistorikk(message)
    }

    override fun createMessage(packet: JsonMessage) = UtbetalingshistorikkEtterInfotrygdendringMessage(packet)
}
