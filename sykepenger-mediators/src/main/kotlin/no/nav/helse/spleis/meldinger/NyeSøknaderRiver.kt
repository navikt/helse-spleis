package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.NySøknadMessage

internal class NyeSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "ny_søknad"
    override val riverName = "Ny søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey("sykmeldingId", "arbeidsgiver.orgnummer")
        message.requireValue("status", "NY")
        message.interestedIn("fremtidig_søknad")
    }

    override fun createMessage(packet: JsonMessage) = NySøknadMessage(packet)
}
