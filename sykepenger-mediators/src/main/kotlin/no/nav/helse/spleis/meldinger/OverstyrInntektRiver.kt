package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.OverstyrInntektMessage

internal class OverstyrInntektRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : HendelseRiver(rapidsConnection, messageMediator) {
    override val eventName = "overstyr_inntekt"
    override val riverName = "Overstyr inntekt"

    override fun validate(message: JsonMessage) {
        message.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer", "månedligInntekt", "skjæringstidspunkt")
    }

    override fun createMessage(packet: JsonMessage) = OverstyrInntektMessage(packet)
}
