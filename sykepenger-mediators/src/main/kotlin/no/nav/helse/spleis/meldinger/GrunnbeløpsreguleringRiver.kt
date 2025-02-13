package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.GrunnbeløpsreguleringMessage

internal class GrunnbeløpsreguleringRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "grunnbeløpsregulering"

    override val riverName = "Grunnbeløpsregulering"

    override fun validate(message: JsonMessage) {
        message.requireKey("fødselsnummer", "skjæringstidspunkt")
    }

    override fun createMessage(packet: JsonMessage) = GrunnbeløpsreguleringMessage(
        packet, Meldingsporing(
        id = packet.meldingsreferanseId(),
        fødselsnummer = packet["fødselsnummer"].asText()
    )
    )
}
