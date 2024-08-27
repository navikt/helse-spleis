
package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
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
