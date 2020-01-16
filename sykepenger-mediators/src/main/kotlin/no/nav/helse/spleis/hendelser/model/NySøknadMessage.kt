package no.nav.helse.spleis.hendelser.model

import no.nav.helse.person.Problemer
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Søknad
internal class NySøknadMessage(originalMessage: String, private val problems: Problemer) :
    SøknadMessage(originalMessage, problems) {
    init {
        requiredValue("status", "NY")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<NySøknadMessage> {

        override fun createMessage(message: String, problems: Problemer) =
            NySøknadMessage(message, problems)
    }
}
