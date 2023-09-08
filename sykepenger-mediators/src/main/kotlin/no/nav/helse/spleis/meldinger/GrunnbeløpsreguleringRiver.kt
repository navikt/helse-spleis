package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.GrunnbeløpsreguleringMessage

internal class GrunnbeløpsreguleringRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "grunnbeløpsregulering"

    override val riverName = "Grunnbeløpsregulering"

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "skjæringstidspunkt")
    }

    override fun createMessage(packet: JsonMessage) = GrunnbeløpsreguleringMessage(packet)
}
