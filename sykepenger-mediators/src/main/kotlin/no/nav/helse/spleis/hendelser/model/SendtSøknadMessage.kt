package no.nav.helse.spleis.hendelser.model

import no.nav.helse.person.Problemer
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Søknad
internal class SendtSøknadMessage(originalMessage: String, private val problems: Problemer) :
    SøknadMessage(originalMessage, problems) {
    init {
        requiredValue("status", "SENDT")
        requiredKey("sendtNav")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<SendtSøknadMessage> {

        override fun createMessage(message: String, problems: Problemer) =
            SendtSøknadMessage(message, problems)
    }
}
