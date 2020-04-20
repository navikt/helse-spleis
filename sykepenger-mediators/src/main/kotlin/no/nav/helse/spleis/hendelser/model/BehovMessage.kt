package no.nav.helse.spleis.hendelser.model

import no.nav.helse.rapids_rivers.JsonMessage

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override val fødselsnummer = packet["fødselsnummer"].asText()
}
