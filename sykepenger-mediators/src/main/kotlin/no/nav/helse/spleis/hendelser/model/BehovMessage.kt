package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageDirector
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageRecognizer

// Understands a JSON message representing a Behov
internal class BehovMessage(originalMessage: String, problems: MessageProblems) : JsonMessage(originalMessage, problems) {
    init {
        requiredKey("@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "hendelse", "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId")
        requiredValue("@final", true)
    }

    class Recognizer(director: MessageDirector<BehovMessage>) :
        MessageRecognizer<BehovMessage>(director) {

        override fun createMessage(message: String, problems: MessageProblems) =
            BehovMessage(message, problems)
    }
}
