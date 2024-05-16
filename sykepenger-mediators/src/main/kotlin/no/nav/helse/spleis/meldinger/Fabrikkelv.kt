package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.HendelseMessage

internal abstract class Fabrikkelv<in F: Meldingsfabrikk<T>, T : HendelseMessage>(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator,
    private val fabrikk: F
) : HendelseRiver(rapidsConnection, messageMediator) {

    override fun validate(message: JsonMessage) {
        fabrikk.validate(message)
    }
    override fun createMessage(packet: JsonMessage) = fabrikk.lagMelding(packet)
}

internal interface Meldingsfabrikk<out T : HendelseMessage> {
    fun validate(message: JsonMessage)
    fun lagMelding(message: JsonMessage): T
}