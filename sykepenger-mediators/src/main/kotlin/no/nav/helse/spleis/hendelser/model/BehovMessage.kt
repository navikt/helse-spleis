package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDateTime

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    HendelseMessage(originalMessage, problems) {
    init {
        requireValue("@event_name", "behov")
        requireKey(
            "@behov", "@final", "@løsning",
            "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId",
            "tilstand"
        )
        require("@opprettet", JsonNode::asLocalDateTime)
        require("@besvart", JsonNode::asLocalDateTime)
        requireValue("@final", true)
    }

    override val fødselsnummer: String get() = this["fødselsnummer"].asText()
}
