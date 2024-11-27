package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Sykepengehistorikk
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.UtbetalingshistorikkRiver.Companion.validerSykepengehistorikk
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkEtterInfotrygdendringMessage

internal class UtbetalingshistorikkEtterInfotrygdendringRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {
    override val behov = listOf(Sykepengehistorikk)
    override val riverName = "Utbetalingshistorikk etter infotrygdendring"

    init {
        river.precondition { message ->
            message.require("@behov") { require(it.size() == 1) }
            message.forbid("fagsystemId", "vedtaksperiodeId")
        }
    }

    override fun validate(message: JsonMessage) {
        validerSykepengehistorikk(message)
    }

    override fun createMessage(packet: JsonMessage) =
        UtbetalingshistorikkEtterInfotrygdendringMessage(
            packet, Meldingsporing(
            id = packet["@id"].asText().toUUID(),
            fødselsnummer = packet["fødselsnummer"].asText()
        )
        )
}
