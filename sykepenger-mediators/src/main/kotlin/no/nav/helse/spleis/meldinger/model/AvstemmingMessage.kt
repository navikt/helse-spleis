package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Avstemming
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

internal class AvstemmingMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    private val avstemming
        get() = Avstemming(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, avstemming)
    }
}
