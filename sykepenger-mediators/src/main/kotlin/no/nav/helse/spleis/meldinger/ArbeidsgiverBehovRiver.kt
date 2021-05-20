
package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator

internal abstract class ArbeidsgiverBehovRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : BehovRiver(rapidsConnection, messageMediator) {

    init {
        river.validate(::validateBehov)
    }

    private fun validateBehov(packet: JsonMessage) {
        packet.requireKey("organisasjonsnummer")
    }
}
