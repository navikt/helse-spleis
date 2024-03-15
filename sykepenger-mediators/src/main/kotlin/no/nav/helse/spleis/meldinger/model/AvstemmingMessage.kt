package no.nav.helse.spleis.meldinger.model

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.IHendelseMediator

internal class AvstemmingMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, fødselsnummer.somPersonidentifikator(), aktørId, context)
    }
}
