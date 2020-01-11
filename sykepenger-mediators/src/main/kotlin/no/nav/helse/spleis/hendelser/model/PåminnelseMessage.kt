package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing a Påminnelse
internal class PåminnelseMessage(originalMessage: String, private val problems: MessageProblems) :
    JsonMessage(originalMessage, problems) {
    init {
        requiredKey(
            "antallGangerPåminnet", "tilstand",
            "tilstandsendringstidspunkt", "påminnelsestidspunkt",
            "nestePåminnelsestidspunkt", "vedtaksperiodeId",
            "organisasjonsnummer", "fødselsnummer", "aktørId"
        )
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<PåminnelseMessage> {

        override fun createMessage(message: String, problems: MessageProblems) =
            PåminnelseMessage(message, problems)
    }
}
