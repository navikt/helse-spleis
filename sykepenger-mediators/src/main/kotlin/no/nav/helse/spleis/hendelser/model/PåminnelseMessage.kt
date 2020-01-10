package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageDirector
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageRecognizer

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(originalMessage: String, problems: MessageProblems) :
    JsonMessage(originalMessage, problems) {
    init {
        requiredKey(
            "antallGangerPåminnet", "tilstand",
            "tilstandsendringstidspunkt", "påminnelsestidspunkt",
            "nestePåminnelsestidspunkt", "vedtaksperiodeId",
            "organisasjonsnummer", "fødselsnummer", "aktørId"
        )
    }

    class Recognizer(director: MessageDirector<PåminnelseMessage>) :
        MessageRecognizer<PåminnelseMessage>(director) {

        override fun createMessage(message: String, problems: MessageProblems) =
            PåminnelseMessage(message, problems)
    }
}
