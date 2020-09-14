package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.RollbackDelete
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class RollbackDeleteMessage(val packet: MessageDelegate) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, RollbackDelete(id, aktørId, fødselsnummer))
    }
}
