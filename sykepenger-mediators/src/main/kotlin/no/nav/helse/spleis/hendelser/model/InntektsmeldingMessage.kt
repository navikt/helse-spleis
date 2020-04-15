package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.rapids_rivers.*
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    HendelseMessage(originalMessage, problems) {
    init {
        requireValue("@event_name", "inntektsmelding")
        requireKey(
            "inntektsmeldingId", "arbeidstakerFnr",
            "arbeidstakerAktorId", "virksomhetsnummer",
            "arbeidsgivertype", "beregnetInntekt",
            "status", "arkivreferanse"
        )
        requireArray("arbeidsgiverperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        requireArray("ferieperioder") {
            require("fom", JsonNode::asLocalDate)
            require("tom", JsonNode::asLocalDate)
        }
        requireArray("endringIRefusjoner") {
            require("endringsdato", JsonNode::asLocalDate)
        }
        require("mottattDato", JsonNode::asLocalDateTime)
        interestedIn("foersteFravaersdag", JsonNode::asLocalDate)
        interestedIn("refusjon.beloepPrMnd")
        interestedIn("refusjon.opphoersdato", JsonNode::asLocalDate)
        interestedIn("arbeidsforholdId", "begrunnelseForReduksjonEllerIkkeUtbetalt")
    }

    override val fødselsnummer get() = this["arbeidstakerFnr"].asText()
    private val refusjon
        get() = Inntektsmelding.Refusjon(
            beløpPrMåned = this["refusjon.beloepPrMnd"].takeUnless(JsonNode::isMissingOrNull)?.asDouble(),
            opphørsdato = this["refusjon.opphoersdato"].asOptionalLocalDate(),
            endringerIRefusjon = this["endringIRefusjoner"].map { it.path("endringsdato").asLocalDate() }
        )
    private val orgnummer get() = this["virksomhetsnummer"].asText()
    private val aktørId get() = this["arbeidstakerAktorId"].asText()
    private val mottattDato get() = this["mottattDato"].asLocalDateTime()
    private val førsteFraværsdag get() = this["foersteFravaersdag"].asOptionalLocalDate()
    private val beregnetInntekt get() = this["beregnetInntekt"].asDouble()
    private val arbeidsgiverperioder get() = this["arbeidsgiverperioder"].map(::asPeriode)
    private val ferieperioder get() = this["ferieperioder"].map(::asPeriode)

    override fun accept(processor: MessageProcessor) {
        processor.process(this)
    }

    internal fun asInntektsmelding() = Inntektsmelding(
        meldingsreferanseId = this.id,
        refusjon = refusjon,
        orgnummer = orgnummer,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder,
        arbeidsforholdId = this["arbeidsforholdId"].takeIf (JsonNode::isTextual)?.asText(),
        begrunnelseForReduksjonEllerIkkeUtbetalt = this["begrunnelseForReduksjonEllerIkkeUtbetalt"].takeIf (JsonNode::isTextual)?.asText()
    )

    object Factory : MessageFactory<InntektsmeldingMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = InntektsmeldingMessage(message, problems)
    }
}

