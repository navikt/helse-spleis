package no.nav.helse.spleis.hendelser.model

import no.nav.helse.rapids_rivers.JsonMessage

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(packet: JsonMessage) :
    HendelseMessage(packet) {

    override val fødselsnummer = packet["fnr"].asText()
}
