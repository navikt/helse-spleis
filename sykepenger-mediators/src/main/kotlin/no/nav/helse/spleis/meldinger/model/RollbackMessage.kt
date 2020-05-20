package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Rollback
import no.nav.helse.hendelser.RollbackDelete
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

internal class RollbackMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val type: String = packet["type"].asText()

    override fun behandle(mediator: IHendelseMediator) {
        when (type) {
            "Rollback" -> {
                val personVersjon = packet["personVersjon"].asLong()
                mediator.behandle(this, Rollback(aktørId, fødselsnummer, personVersjon))
            }
            "Delete" -> mediator.behandle(this, RollbackDelete(aktørId, fødselsnummer))
            else -> throw IllegalArgumentException("Ukjent rollbacktype: $type")
        }
    }
}
