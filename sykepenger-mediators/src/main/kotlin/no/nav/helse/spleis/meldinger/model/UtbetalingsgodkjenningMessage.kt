package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.spleis.Behov.Behovstype.Godkjenning
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.yrkesaktivitetssporing

// Understands a JSON message representing a Godkjenning-behov
internal class UtbetalingsgodkjenningMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {
    private val utbetalingId = packet["utbetalingId"].asText().toUUID()
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().toUUID()
    private val behandlingId = packet["behandlingId"].asText().toUUID()
    private val behandlingsporing = packet.yrkesaktivitetssporing
    private val saksbehandler = packet["@løsning.${Godkjenning.utgåendeNavn}.saksbehandlerIdent"].asText()
    private val saksbehandlerEpost = packet["@løsning.${Godkjenning.utgåendeNavn}.saksbehandlerEpost"].asText()
    private val godkjenttidspunkt = packet["@løsning.${Godkjenning.utgåendeNavn}.godkjenttidspunkt"].asLocalDateTime()
    private val utbetalingGodkjent = packet["@løsning.${Godkjenning.utgåendeNavn}.godkjent"].asBoolean()
    private val automatiskBehandling = packet["@løsning.${Godkjenning.utgåendeNavn}.automatiskBehandling"].asBoolean()

    private val utbetalingsgodkjenning
        get() = Utbetalingsgodkjenning(
                meldingsreferanseId = meldingsporing.id,
                behandlingsporing = behandlingsporing,
                utbetalingId = utbetalingId,
                vedtaksperiodeId = vedtaksperiodeId,
                behandlingId = behandlingId,
                saksbehandler = saksbehandler,
                saksbehandlerEpost = saksbehandlerEpost,
                utbetalingGodkjent = utbetalingGodkjent,
                godkjenttidspunkt = godkjenttidspunkt,
                automatiskBehandling = automatiskBehandling
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, utbetalingsgodkjenning, context)
    }
}
