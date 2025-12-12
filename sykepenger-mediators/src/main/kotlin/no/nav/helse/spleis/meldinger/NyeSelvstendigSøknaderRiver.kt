package no.nav.helse.spleis.meldinger

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.NySelvstendigSøknadMessage

internal class NyeSelvstendigSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "ny_søknad_selvstendig"
    override val riverName = "Ny selvstendig søknad"

    override fun precondition(packet: JsonMessage) {
        packet.requireAny("arbeidssituasjon", listOf("SELVSTENDIG_NARINGSDRIVENDE", "BARNEPASSER", "JORDBRUKER", "FISKER", "ANNET"))
    }

    override fun validate(message: JsonMessage) {
        message.requireKey("sykmeldingId", "arbeidssituasjon")
        message.requireValue("status", "NY")
        message.interestedIn("fremtidig_søknad")
    }

    override fun createMessage(packet: JsonMessage) = NySelvstendigSøknadMessage(
        packet = packet,
        meldingsporing = Meldingsporing(
            id = packet.meldingsreferanseId(),
            fødselsnummer = packet["fnr"].asText()
        )
    )
}
