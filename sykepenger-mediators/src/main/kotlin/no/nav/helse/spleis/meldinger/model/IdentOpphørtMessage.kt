package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.IdentOpphørt
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.somPersonidentifikator
import no.nav.helse.spleis.IHendelseMediator

internal class IdentOpphørtMessage(packet: JsonMessage) : HendelseMessage(packet) {

    override val fødselsnummer =  packet["fødselsnummer"].asText()
    private val aktørId =  packet["aktørId"].asText()
    private val nyttFødselsnummer = packet["nye_identer.fødselsnummer"].asText()
    private val nyAktørId = packet["nye_identer.aktørId"].asText()
    private val gamleIdenter = packet["gamle_identer"].map { it.path("ident").asText().somPersonidentifikator() }.toSet()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(nyttFødselsnummer.somPersonidentifikator(), this, IdentOpphørt(
            meldingsreferanseId = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId
        ), nyAktørId, gamleIdenter, context)
    }

}
