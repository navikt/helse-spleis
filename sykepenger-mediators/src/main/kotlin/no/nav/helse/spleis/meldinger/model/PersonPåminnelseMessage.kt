package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing a Påminnelse
internal class PersonPåminnelseMessage(packet: JsonMessage) : HendelseMessage(packet) {

    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    private val påminnelse
        get() = PersonPåminnelse(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer
        )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, påminnelse)
    }
}
