package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDate
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing an Inntektsmelding
internal class InntektsmeldingMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing
) : HendelseMessage(packet) {
    private val refusjon = Inntektsmelding.Refusjon(
        beløp = packet["refusjon.beloepPrMnd"].takeUnless(JsonNode::isMissingOrNull)?.asDouble()?.månedlig,
        opphørsdato = packet["refusjon.opphoersdato"].asOptionalLocalDate(),
        endringerIRefusjon = packet["endringIRefusjoner"].map {
            Inntektsmelding.Refusjon.EndringIRefusjon(
                it.path("beloep").asDouble().månedlig,
                it.path("endringsdato").asLocalDate()
            )
        }
    )
    private val orgnummer = packet["virksomhetsnummer"].asText()

    private val mottatt = packet["mottattDato"].asLocalDateTime()
    private val førsteFraværsdag = packet["foersteFravaersdag"].asOptionalLocalDate()
    private val beregnetInntekt = packet["beregnetInntekt"].asDouble()
    private val arbeidsgiverperioder = packet["arbeidsgiverperioder"].map(::asPeriode)
    private val begrunnelseForReduksjonEllerIkkeUtbetalt =
        packet["begrunnelseForReduksjonEllerIkkeUtbetalt"].takeIf(JsonNode::isTextual)?.asText()
    private val opphørAvNaturalytelser = packet["opphoerAvNaturalytelser"].tilOpphørAvNaturalytelser()
    private val harFlereInntektsmeldinger = packet["harFlereInntektsmeldinger"].asBoolean(false)
    private val avsendersystem = avsendersystem(avsendersystem = packet["avsenderSystem"].path("navn").asText(), førsteFraværsdag = førsteFraværsdag)

    private val inntektsmelding get() = Inntektsmelding(
        meldingsreferanseId = meldingsporing.id,
        refusjon = refusjon,
        orgnummer = orgnummer,
        beregnetInntekt = beregnetInntekt.månedlig,
        arbeidsgiverperioder = arbeidsgiverperioder,
        begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
        opphørAvNaturalytelser = opphørAvNaturalytelser,
        harFlereInntektsmeldinger = harFlereInntektsmeldinger,
        avsendersystem = avsendersystem,
        mottatt = mottatt
    )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, inntektsmelding, context)
    }
}

internal fun JsonNode.tilOpphørAvNaturalytelser(): List<Inntektsmelding.OpphørAvNaturalytelse> {
    return map { naturalytelse ->
        Inntektsmelding.OpphørAvNaturalytelse(
            beløp = naturalytelse["beloepPrMnd"].asDouble().månedlig,
            fom = naturalytelse["fom"].asLocalDate(),
            naturalytelse = naturalytelse["naturalytelse"].asText(),
        )
    }
}

internal fun avsendersystem(avsendersystem: String, førsteFraværsdag: LocalDate?): Inntektsmelding.Avsendersystem {
    return if (avsendersystem == "AltinnPortal") Inntektsmelding.Avsendersystem.Altinn(førsteFraværsdag)
    else Inntektsmelding.Avsendersystem.LPS(førsteFraværsdag)
}

