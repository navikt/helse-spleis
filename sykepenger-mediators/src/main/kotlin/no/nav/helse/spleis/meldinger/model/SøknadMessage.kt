package no.nav.helse.spleis.meldinger.model

import no.nav.helse.spleis.MessageDelegate

// Understands a JSON message representing a Søknad
internal abstract class SøknadMessage(packet: MessageDelegate) :
    HendelseMessage(packet) {

    override val fødselsnummer = packet["fnr"].asText()
}
