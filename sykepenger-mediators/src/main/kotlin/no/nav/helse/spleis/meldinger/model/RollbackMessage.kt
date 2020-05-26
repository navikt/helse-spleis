package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Rollback
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

internal class RollbackMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val personVersjon = packet["personVersjon"].asLong()

    override fun behandle(mediator: IHendelseMediator) =
        mediator.behandle(this, Rollback(aktørId, fødselsnummer, personVersjon))
}
