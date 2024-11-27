package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilAvsendersystem
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.util.UUID

// Understands a JSON message representing an Inntektsmelding replay
internal class InntektsmeldingerReplayMessage(
    packet: JsonMessage,
    override val meldingsporing: Meldingsporing
) : HendelseMessage(packet) {
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    override val skalDuplikatsjekkes = false

    private val inntektsmeldinger = mutableListOf<Inntektsmelding>()

    private val inntektsmeldingerReplay =
        InntektsmeldingerReplay(
            meldingsreferanseId = meldingsporing.id,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            inntektsmeldinger = inntektsmeldinger
        )

    init {
        packet["inntektsmeldinger"].forEach { inntektsmelding ->
            inntektsmeldinger.add(
                inntektsmeldingReplay(
                    inntektsmelding.path("internDokumentId").asText().toUUID(),
                    inntektsmelding.path("inntektsmelding")
                )
            )
        }
    }

    override fun behandle(
        mediator: IHendelseMediator,
        context: MessageContext
    ) {
        mediator.behandle(this, inntektsmeldingerReplay, context)
    }

    private fun inntektsmeldingReplay(
        internDokumentId: UUID,
        packet: JsonNode
    ): Inntektsmelding {
        val refusjon =
            Inntektsmelding.Refusjon(
                beløp =
                    packet
                        .path("refusjon")
                        .path("beloepPrMnd")
                        .takeUnless(JsonNode::isMissingOrNull)
                        ?.asDouble()
                        ?.månedlig,
                opphørsdato = packet.path("refusjon").path("opphoersdato").asOptionalLocalDate(),
                endringerIRefusjon =
                    packet["endringIRefusjoner"].map {
                        Inntektsmelding.Refusjon.EndringIRefusjon(
                            it.path("beloep").asDouble().månedlig,
                            it.path("endringsdato").asLocalDate()
                        )
                    }
            )
        val orgnummer = packet.path("virksomhetsnummer").asText()
        val mottatt = packet.path("mottattDato").asLocalDateTime()
        val førsteFraværsdag = packet.path("foersteFravaersdag").asOptionalLocalDate()
        val beregnetInntekt = packet.path("beregnetInntekt").asDouble()
        val arbeidsgiverperioder = packet.path("arbeidsgiverperioder").map(::asPeriode)
        val begrunnelseForReduksjonEllerIkkeUtbetalt = packet.path("begrunnelseForReduksjonEllerIkkeUtbetalt").takeIf(JsonNode::isTextual)?.asText()
        val harOpphørAvNaturalytelser = packet.path("opphoerAvNaturalytelser").size() > 0
        val harFlereInntektsmeldinger = packet.path("harFlereInntektsmeldinger").asBoolean(false)
        val avsendersystem = packet.path("avsenderSystem").tilAvsendersystem(null, null, førsteFraværsdag) // Vi skal ikke replaye portalIM så om det feiler her er noe gæli

        return Inntektsmelding(
            meldingsreferanseId = internDokumentId,
            refusjon = refusjon,
            orgnummer = orgnummer,
            beregnetInntekt = beregnetInntekt.månedlig,
            arbeidsgiverperioder = arbeidsgiverperioder,
            begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
            harFlereInntektsmeldinger = harFlereInntektsmeldinger,
            avsendersystem = avsendersystem,
            mottatt = mottatt
        )
    }
}
