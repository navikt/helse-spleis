package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.spleis.IHendelseMediator

internal class InfotrygdendringMessage(packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer =  packet["fødselsnummer"].asText()
    val aktørId =  packet["aktørId"].asText()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, Infotrygdendring(
            meldingsreferanseId = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId
        ), context)
    }

}
