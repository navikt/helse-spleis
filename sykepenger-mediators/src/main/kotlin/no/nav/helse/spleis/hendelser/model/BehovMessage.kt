package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Behov
internal class BehovMessage(originalMessage: String, private val problems: MessageProblems) : JsonMessage(originalMessage, problems) {
    init {
        requiredKey("@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "hendelse", "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId")
        requiredValue("@final", true)
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<BehovMessage> {

        override fun createMessage(message: String, problems: MessageProblems) =
            BehovMessage(message, problems)
    }
}
