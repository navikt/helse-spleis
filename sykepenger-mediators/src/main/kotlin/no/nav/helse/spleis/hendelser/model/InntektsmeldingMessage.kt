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
            "refusjon.beloepPrMnd",
            "endringIRefusjoner", "arbeidsgiverperioder",
            "status", "arkivreferanse", "ferieperioder",
            "foersteFravaersdag", "mottattDato"
        )
    }

    override fun accept(processor: MessageProcessor) {
        processor.process(this, aktivitetslogger)
    }

    internal fun asModelInntektsmelding() = ModelInntektsmelding(
        hendelseId = UUID.randomUUID(),
        refusjon = ModelInntektsmelding.Refusjon(
            this["refusjon.opphoersdato"].asOptionalLocalDate(),
            this["refusjon.beloepPrMnd"].asDouble(),
            this["endringIRefusjoner"].map { it.path("endringsdato").asLocalDate() }),
        orgnummer = this["virksomhetsnummer"].asText(),
        fødselsnummer = this["arbeidstakerFnr"].asText(),
        aktørId = this["arbeidstakerAktorId"].asText(),
        mottattDato = this["mottattDato"].asLocalDateTime(),
        førsteFraværsdag = this["foersteFravaersdag"].asLocalDate(),
        beregnetInntekt = this["beregnetInntekt"].asDouble(),
        aktivitetslogger = aktivitetslogger,
        originalJson = this.toJson(),
        arbeidsgiverperioder = this["arbeidsgiverperioder"].map(::asPeriode).map { (fom, tom) -> fom..tom },
        ferieperioder = this["ferieperioder"].map(::asPeriode).map { (fom, tom) -> fom..tom }
    )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger) =
            InntektsmeldingMessage(message, problems)
    }
}
