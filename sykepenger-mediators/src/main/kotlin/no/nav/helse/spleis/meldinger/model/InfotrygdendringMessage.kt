package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Infotrygdendring
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
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
