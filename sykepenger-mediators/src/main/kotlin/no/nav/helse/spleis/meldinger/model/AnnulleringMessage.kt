package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class AnnulleringMessage(packet: MessageDelegate) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val fagsystemId = packet["fagsystemId"].asText()
    private val saksbehandler = Saksbehandler.fraJson(packet["saksbehandler"])
    private val annullerUtbetaling
        get() = AnnullerUtbetaling(
            id,
            aktørId,
            fødselsnummer,
            organisasjonsnummer,
            fagsystemId,
            saksbehandler.ident,
            saksbehandler.epostadresse,
            opprettet
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, annullerUtbetaling)
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
