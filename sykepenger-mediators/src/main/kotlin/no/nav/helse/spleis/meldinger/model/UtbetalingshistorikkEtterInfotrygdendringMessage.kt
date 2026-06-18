package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.spleis.BehandlingContext
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage.Companion.utbetalinger

// Understands a JSON message representing an Ytelserbehov
internal class UtbetalingshistorikkEtterInfotrygdendringMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {

    private val besvart = packet["@besvart"].asLocalDateTime()

    private val utbetalinger = packet.utbetalinger()

    private fun infotrygdhistorikk(meldingsreferanseId: MeldingsreferanseId) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = besvart,
            hendelseId = meldingsreferanseId,
            perioder = utbetalinger
        )

    private fun utbetalingshistorikkEtterInfotrygdendring() =
        UtbetalingshistorikkEtterInfotrygdendring(
            meldingsreferanseId = meldingsporing.id,
            element = infotrygdhistorikk(meldingsporing.id),
            besvart = besvart
        )

    override fun behandle(mediator: IHendelseMediator, context: BehandlingContext) {
        mediator.behandle(this, utbetalingshistorikkEtterInfotrygdendring(), context)
    }
}
