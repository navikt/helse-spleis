package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Rollback
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class RollbackMessage(val packet: MessageDelegate) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val personVersjon = packet["personVersjon"].asLong()

    override fun behandle(mediator: IHendelseMediator) =
        mediator.behandle(this, Rollback(id, aktørId, fødselsnummer, personVersjon))
}
