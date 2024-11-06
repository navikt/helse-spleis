package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage.Companion.arbeidskategorikoder
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage.Companion.inntektshistorikk
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage.Companion.utbetalinger

// Understands a JSON message representing an Ytelserbehov
internal class UtbetalingshistorikkEtterInfotrygdendringMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {

    private val besvart = packet["@besvart"].asLocalDateTime()

    private val utbetalinger = packet.utbetalinger()

    private val arbeidskategorikoder: Map<String, LocalDate> = packet.arbeidskategorikoder()

    private val inntektshistorikk = packet.inntektshistorikk()

    private fun infotrygdhistorikk(meldingsreferanseId: UUID) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = besvart,
            hendelseId = meldingsreferanseId,
            perioder = utbetalinger,
            inntekter = inntektshistorikk,
            arbeidskategorikoder = arbeidskategorikoder
        )

    private fun utbetalingshistorikkEtterInfotrygdendring() =
        UtbetalingshistorikkEtterInfotrygdendring(
            meldingsreferanseId = meldingsporing.id,
            aktørId = meldingsporing.aktørId,
            fødselsnummer = meldingsporing.fødselsnummer,
            element = infotrygdhistorikk(meldingsporing.id),
            besvart = besvart
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, utbetalingshistorikkEtterInfotrygdendring(), context)
    }
}
