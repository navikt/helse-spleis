package no.nav.helse.spleis.meldinger.model

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDateTime

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(packet: JsonMessage) :
    HendelseMessage(packet) {

    protected val søknadOpprettet = packet["opprettet"].asLocalDateTime()
    override val fødselsnummer = packet["fnr"].asText()
}
