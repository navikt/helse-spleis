package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.spleis.hendelser.MessageFactory
import no.nav.helse.spleis.hendelser.MessageProcessor
import no.nav.helse.spleis.rest.HendelseDTO

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(
    originalMessage: String,
    problems: MessageProblems
) :
    HendelseMessage(originalMessage, problems) {
    init {
        requireValue("@event_name", "inntektsmelding")
        requireKey("@id")
        requireKey(
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

    override val fødselsnummer get() = this["arbeidstakerFnr"].asText()
    private val refusjon
        get() = this["refusjon.beloepPrMnd"].takeUnless { it.isMissingNode || it.isNull }?.let { beløpPerMåned ->
            Inntektsmelding.Refusjon(
                this["refusjon.opphoersdato"].asOptionalLocalDate(),
                beløpPerMåned.asDouble(),
                this["endringIRefusjoner"].map { it.path("endringsdato").asLocalDate() }
            )
        }
    private val orgnummer get() = this["virksomhetsnummer"].asText()
    private val aktørId get() = this["arbeidstakerAktorId"].asText()
    private val mottattDato get() = this["mottattDato"].asLocalDateTime()
    private val førsteFraværsdag get() = this["foersteFravaersdag"].asLocalDate()
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
        ferieperioder = ferieperioder
    )

    fun asSpeilDTO() = HendelseDTO.InntektsmeldingDTO(
        beregnetInntekt = beregnetInntekt,
        førsteFraværsdag = førsteFraværsdag,
        mottattDato = mottattDato
    )

    object Factory : MessageFactory<InntektsmeldingMessage> {
        override fun createMessage(message: String, problems: MessageProblems) = InntektsmeldingMessage(message, problems)
    }
}

