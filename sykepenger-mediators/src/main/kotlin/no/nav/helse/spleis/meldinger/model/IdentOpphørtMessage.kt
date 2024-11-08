package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.IdentOpphørt
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class IdentOpphørtMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val nyttFødselsnummer = packet["nye_identer.fødselsnummer"].asText()
    private val gamleIdenter = packet["gamle_identer"].map { Personidentifikator(it.path("ident").asText()) }.toSet()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(
            Personidentifikator(nyttFødselsnummer), this, IdentOpphørt(
                meldingsreferanseId = meldingsporing.id
            ), gamleIdenter, context
        )
    }

}
