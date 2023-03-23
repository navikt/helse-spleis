package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.Infotrygdendring
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator

internal class DødsmeldingMessage(packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer =  packet["fødselsnummer"].asText()
    private val aktørId =  packet["aktørId"].asText()
    private val dødsdato =  packet["dødsdato"].asLocalDate()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, Dødsmelding(
            meldingsreferanseId = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            dødsdato = dødsdato
        ), context)
    }

}
