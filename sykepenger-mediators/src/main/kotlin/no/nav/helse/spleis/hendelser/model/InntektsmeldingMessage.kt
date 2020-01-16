package no.nav.helse.spleis.hendelser.model

import no.nav.helse.person.Problemer
import no.nav.helse.spleis.hendelser.JsonMessage
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(originalMessage: String, private val problems: Problemer) :
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

    override fun accept(processor: MessageProcessor) {
        processor.process(this, problems)
    }

    object Factory : MessageFactory<InntektsmeldingMessage> {

        override fun createMessage(message: String, problems: Problemer) =
            InntektsmeldingMessage(message, problems)
    }
}
