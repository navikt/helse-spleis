package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.Infotrygdendring
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
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
