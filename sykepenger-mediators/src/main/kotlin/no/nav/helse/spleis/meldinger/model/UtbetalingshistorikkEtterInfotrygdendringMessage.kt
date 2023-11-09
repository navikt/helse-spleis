package no.nav.helse.spleis.meldinger.model

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage.Companion.arbeidskategorikoder
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage.Companion.inntektshistorikk
import no.nav.helse.spleis.meldinger.model.UtbetalingshistorikkMessage.Companion.utbetalinger

// Understands a JSON message representing an Ytelserbehov
internal class UtbetalingshistorikkEtterInfotrygdendringMessage(packet: JsonMessage) : BehovMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
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
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            element = infotrygdhistorikk(id),
            aktivitetslogg = Aktivitetslogg(),
            besvart = besvart
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, utbetalingshistorikkEtterInfotrygdendring(), context)
    }
}
