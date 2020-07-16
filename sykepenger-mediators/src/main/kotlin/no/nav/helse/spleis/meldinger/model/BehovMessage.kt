package no.nav.helse.spleis.meldinger.model

import no.nav.helse.spleis.MessageDelegate

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(packet: MessageDelegate) : HendelseMessage(packet) {
    override val fødselsnummer = packet["fødselsnummer"].asText()
}
