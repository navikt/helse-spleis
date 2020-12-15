package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Avstemming
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate

internal class AvstemmingMessage(packet: MessageDelegate) : HendelseMessage(packet) {

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
