package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.yrkesaktivitetssporing

internal class AnnulleringMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {
    private val utbetalingId = packet["utbetalingId"].asText().trim().toUUID()
    private val saksbehandler = Saksbehandler.fraJson(packet["saksbehandler"])
    private val årsaker = packet["begrunnelser"].map { it.asText() }
    private val begrunnelse = packet["kommentar"].takeUnless { it.isMissingOrNull() }?.asText() ?: ""
    private val behandlingsporing = packet.yrkesaktivitetssporing
    private val annullerUtbetaling
        get() = AnnullerUtbetaling(
            meldingsporing.id,
            behandlingsporing,
            utbetalingId,
            saksbehandler.ident,
            saksbehandler.epostadresse,
            opprettet,
            årsaker,
            begrunnelse
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, annullerUtbetaling, context)
    }

    private class Saksbehandler(
        val epostadresse: String,
        val ident: String,
    ) {
        companion object {
            fun fraJson(jsonNode: JsonNode) = Saksbehandler(
                epostadresse = jsonNode["epostaddresse"].asText(),
                ident = jsonNode["ident"].asText()
            )
        }
    }
}
