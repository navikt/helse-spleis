package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.*
import java.util.*

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(originalMessage: String, private val aktivitetslogger: Aktivitetslogger) :
    JsonMessage(originalMessage, aktivitetslogger) {
    init {
        requiredKey(
            "inntektsmeldingId", "arbeidstakerFnr",
            "arbeidstakerAktorId", "virksomhetsnummer",
            "arbeidsgivertype", "beregnetInntekt",
            "endringIRefusjoner", "arbeidsgiverperioder",
            "status", "arkivreferanse", "ferieperioder",
            "foersteFravaersdag", "mottattDato"
        )
        interestedIn("refusjon.beloepPrMnd")
        interestedIn("refusjon.opphoersdato")
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelInntektsmelding() = ModelInntektsmelding(
        hendelseId = UUID.randomUUID(),
        refusjon = this["refusjon.beloepPrMnd"].takeUnless { it.isMissingNode || it.isNull }?.let { beløpPerMåned ->
            ModelInntektsmelding.Refusjon(
                this["refusjon.opphoersdato"].asOptionalLocalDate(),
                beløpPerMåned.asDouble(),
                this["endringIRefusjoner"].map { it.path("endringsdato").asLocalDate() }
            )
        },
        orgnummer = this["virksomhetsnummer"].asText(),
        fødselsnummer = this["arbeidstakerFnr"].asText(),
        aktørId = this["arbeidstakerAktorId"].asText(),
        mottattDato = this["mottattDato"].asLocalDateTime(),
        førsteFraværsdag = this["foersteFravaersdag"].asLocalDate(),
        beregnetInntekt = this["beregnetInntekt"].asDouble(),
        aktivitetslogger = aktivitetslogger,
        arbeidsgiverperioder = this["arbeidsgiverperioder"].map(::asPeriode),
        ferieperioder = this["ferieperioder"].map(::asPeriode)
    )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            InntektsmeldingMessage(message, problems)
    }
}
