package no.nav.helse.spleis.hendelser.model

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.spleis.hendelser.*
import no.nav.helse.spleis.rest.HendelseDTO

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(
    originalMessage: String,
    private val aktivitetslogger: Aktivitetslogger,
    private val aktivitetslogg: Aktivitetslogg
) :
    JsonMessage(originalMessage, aktivitetslogger, aktivitetslogg) {
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

    val refusjon
        get() = this["refusjon.beloepPrMnd"].takeUnless { it.isMissingNode || it.isNull }?.let { beløpPerMåned ->
            Inntektsmelding.Refusjon(
                this["refusjon.opphoersdato"].asOptionalLocalDate(),
                beløpPerMåned.asDouble(),
                this["endringIRefusjoner"].map { it.path("endringsdato").asLocalDate() }
            )
        }
    val orgnummer get() = this["virksomhetsnummer"].asText()
    val fødselsnummer get() = this["arbeidstakerFnr"].asText()
    val aktørId get() = this["arbeidstakerAktorId"].asText()
    val mottattDato get() = this["mottattDato"].asLocalDateTime()
    val førsteFraværsdag get() = this["foersteFravaersdag"].asLocalDate()
    val beregnetInntekt get() = this["beregnetInntekt"].asDouble()
    val arbeidsgiverperioder get() = this["arbeidsgiverperioder"].map(::asPeriode)
    val ferieperioder get() = this["ferieperioder"].map(::asPeriode)

    internal fun asInntektsmelding() = Inntektsmelding(
        hendelseId = this.id,
        refusjon = refusjon,
        orgnummer = orgnummer,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        mottattDato = mottattDato,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        aktivitetslogger = aktivitetslogger,
        aktivitetslogg = aktivitetslogg,
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder
    )

    fun asSpeilDTO() = HendelseDTO.InntektsmeldingDTO(
        beregnetInntekt = beregnetInntekt,
        førsteFraværsdag = førsteFraværsdag
    )

    object Factory : MessageFactory {

        override fun createMessage(message: String, problems: Aktivitetslogger, aktivitetslogg: Aktivitetslogg) =
            InntektsmeldingMessage(message, problems, aktivitetslogg)
    }
}
