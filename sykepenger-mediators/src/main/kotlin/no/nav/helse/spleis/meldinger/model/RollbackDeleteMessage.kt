package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.RollbackDelete
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

internal class RollbackDeleteMessage(val packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, RollbackDelete(aktørId, fødselsnummer))
    }
}
