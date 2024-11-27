package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class AvstemmingMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing,
) : HendelseMessage(packet) {
    override fun behandle(
        mediator: IHendelseMediator,
        context: MessageContext,
    ) {
        mediator.behandle(this, Personidentifikator(meldingsporing.fødselsnummer), context)
    }
}
