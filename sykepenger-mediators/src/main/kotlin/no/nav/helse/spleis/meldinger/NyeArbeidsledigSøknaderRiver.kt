package no.nav.helse.spleis.meldinger

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.meldinger.model.NyFrilansSøknadMessage
import no.nav.helse.spleis.meldinger.model.NySøknadMessage

internal class NyeArbeidsledigSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "ny_søknad_arbeidsledig"
    override val riverName = "Ny arbeidsledig søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey("sykmeldingId")
        message.requireValue("status", "NY")
        message.interestedIn("fremtidig_søknad")
        message.forbid("arbeidsgiver.orgnummer")
    }

    override fun createMessage(packet: JsonMessage) = NyFrilansSøknadMessage(packet)
}
