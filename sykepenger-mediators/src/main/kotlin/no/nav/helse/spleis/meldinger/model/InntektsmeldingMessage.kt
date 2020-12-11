package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.MessageDelegate
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing an Inntektsmelding
internal open class InntektsmeldingMessage(packet: MessageDelegate) : HendelseMessage(packet) {
    override val fødselsnummer = packet["arbeidstakerFnr"].asText()
    private val refusjon = Inntektsmelding.Refusjon(
        inntekt = packet["refusjon.beloepPrMnd"].takeUnless(JsonNode::isMissingOrNull)?.asDouble()?.månedlig,
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
    private val begrunnelseForReduksjonEllerIkkeUtbetalt =
        packet["begrunnelseForReduksjonEllerIkkeUtbetalt"].takeIf(JsonNode::isTextual)?.asText()
    private val harOpphørAvNaturalytelser = packet["opphoerAvNaturalytelser"].size() > 0

    protected val inntektsmelding get() = Inntektsmelding(
        meldingsreferanseId = this.id,
        refusjon = refusjon,
        orgnummer = orgnummer,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        førsteFraværsdag = førsteFraværsdag,
        beregnetInntekt = beregnetInntekt.månedlig,
        arbeidsgiverperioder = arbeidsgiverperioder,
        ferieperioder = ferieperioder,
        arbeidsforholdId = arbeidsforholdId,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        harOpphørAvNaturalytelser = harOpphørAvNaturalytelser
    )

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, inntektsmelding)
    }
}

