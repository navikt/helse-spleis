package no.nav.helse.spleis.hendelser.model

import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageDirector
import no.nav.helse.spleis.hendelser.MessageProblems
import no.nav.helse.spleis.hendelser.MessageRecognizer

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(originalMessage: String, problems: MessageProblems) :
    JsonMessage(originalMessage, problems) {
    init {
        requiredKey(
            "inntektsmeldingId", "arbeidstakerFnr",
            "arbeidstakerAktorId", "virksomhetsnummer",
            "arbeidsgivertype", "beregnetInntekt", "refusjon",
            "endringIRefusjoner", "arbeidsgiverperioder",
            "status", "arkivreferanse", "ferieperioder",
            "foersteFravaersdag", "mottattDato"
        )
    }

    class Recognizer(director: MessageDirector<InntektsmeldingMessage>) :
        MessageRecognizer<InntektsmeldingMessage>(director) {

        override fun createMessage(message: String, problems: MessageProblems) =
            InntektsmeldingMessage(message, problems)
    }
}
