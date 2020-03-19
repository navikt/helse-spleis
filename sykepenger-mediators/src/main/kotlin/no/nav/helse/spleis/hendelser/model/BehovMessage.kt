package no.nav.helse.spleis.hendelser.model

import no.nav.helse.rapids_rivers.MessageProblems

// Understands a JSON message representing a Need with solution
internal abstract class BehovMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    HendelseMessage(originalMessage, problems) {
    init {
        requireKey(
            "@behov", "@id", "@opprettet",
            "@final", "@løsning", "@besvart",
            "aktørId", "fødselsnummer",
            "organisasjonsnummer", "vedtaksperiodeId"
        )
        requireValue("@final", true)
    }

    override val fødselsnummer: String get() = this["fødselsnummer"].asText()
}
