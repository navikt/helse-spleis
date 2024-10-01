package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.IdentOpphørt
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.IHendelseMediator

internal class IdentOpphørtMessage(packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer =  packet["fødselsnummer"].asText()
    private val aktørId =  packet["aktørId"].asText()
    private val nyttFødselsnummer = packet["nye_identer.fødselsnummer"].asText()
    private val nyAktørId = packet["nye_identer.aktørId"].asText()
    private val gamleIdenter = packet["gamle_identer"].map { Personidentifikator(it.path("ident").asText()) }.toSet()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(Personidentifikator(nyttFødselsnummer), this, IdentOpphørt(
            meldingsreferanseId = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId
        ), nyAktørId, gamleIdenter, context)
    }

}
