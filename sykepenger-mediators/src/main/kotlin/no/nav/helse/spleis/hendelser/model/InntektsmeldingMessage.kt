package no.nav.helse.spleis.hendelser.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(packet: JsonMessage) : HendelseMessage(packet) {
    override val fødselsnummer = packet["arbeidstakerFnr"].asText()
    private val refusjon = Inntektsmelding.Refusjon(
        beløpPrMåned = packet["refusjon.beloepPrMnd"].takeUnless(JsonNode::isMissingOrNull)?.asDouble(),
        opphørsdato = packet["refusjon.opphoersdato"].asOptionalLocalDate(),
        endringerIRefusjon = packet["endringIRefusjoner"].map { it.path("endringsdato").asLocalDate() }
    )
    private val arbeidsforholdId = packet["arbeidsforholdId"].takeIf (JsonNode::isTextual)?.asText()
    private val orgnummer = packet["virksomhetsnummer"].asText()
    private val aktørId = packet["arbeidstakerAktorId"].asText()
    private val førsteFraværsdag = packet["foersteFravaersdag"].asOptionalLocalDate()
    private val beregnetInntekt = packet["beregnetInntekt"].asDouble()
    private val arbeidsgiverperioder = packet["arbeidsgiverperioder"].map(::asPeriode)
    private val ferieperioder = packet["ferieperioder"].map(::asPeriode)
    private val begrunnelseForReduksjonEllerIkkeUtbetalt = packet["begrunnelseForReduksjonEllerIkkeUtbetalt"].takeIf (JsonNode::isTextual)?.asText()

    private val inntektsmelding = Inntektsmelding(
        meldingsreferanseId = this.id,
        refusjon = refusjon,
        orgnummer = orgnummer,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt,
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder,
        arbeidsforholdId = arbeidsforholdId,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, inntektsmelding)
    }
}

