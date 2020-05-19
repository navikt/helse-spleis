package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Rollback
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

internal class RollbackMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val personVersjon : Long = packet["personVersjon"].asLong()
    private val rollback get() = Rollback(
        aktørId,
        fødselsnummer,
        personVersjon
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, rollback)
    }
}
