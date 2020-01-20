package no.nav.helse.spleis.hendelser.model

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Søknad
internal class FremtidigSøknadMessage(originalMessage: String, private val problems: Aktivitetslogger) :
    SøknadMessage(originalMessage, problems) {
    init {
        requiredValue("status", "FREMTIDIG")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            FremtidigSøknadMessage(message, problems)
    }
}
