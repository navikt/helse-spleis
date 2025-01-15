package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDateTime
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing an Inntektsmelding
internal class NavNoInntektsmeldingMessage(
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
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().toUUID()
    private val orgnummer = packet["virksomhetsnummer"].asText()
    private val mottatt = packet["mottattDato"].asLocalDateTime()
    private val beregnetInntekt = packet["beregnetInntekt"].takeUnless(JsonNode::isMissingOrNull)?.asDouble()
    private val arbeidsgiverperioder = packet["arbeidsgiverperioder"].map(::asPeriode)
    private val begrunnelseForReduksjonEllerIkkeUtbetalt = packet["begrunnelseForReduksjonEllerIkkeUtbetalt"].takeIf(JsonNode::isTextual)?.asText()
    private val opphørAvNaturalytelser = packet["opphoerAvNaturalytelser"].tilOpphørAvNaturalytelser()

    private val arbeidsgiveropplysninger get() = Arbeidsgiveropplysninger(
        meldingsreferanseId = meldingsporing.id,
        innsendt = mottatt,
        registrert = LocalDateTime.now(),
        organisasjonsnummer = orgnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        opplysninger = Arbeidsgiveropplysning.fraInntektsmelding(
            beregnetInntekt = beregnetInntekt?.månedlig,
            arbeidsgiverperioder = arbeidsgiverperioder,
            refusjon = refusjon,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            opphørAvNaturalytelser = opphørAvNaturalytelser
        )
    )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, arbeidsgiveropplysninger, context)
    }
}

