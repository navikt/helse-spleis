package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.util.UUID
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsmelding.BegrunnelseForReduksjonEllerIkkeUtbetalt.Companion.fraInnteksmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing an Inntektsmelding replay
internal class InntektsmeldingerReplayMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())

    private val inntektsmeldinger = mutableListOf<Inntektsmelding>()

    private val inntektsmeldingerReplay = InntektsmeldingerReplay(
        meldingsreferanseId = meldingsporing.id,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
            organisasjonsnummer = organisasjonsnummer
        ),
        vedtaksperiodeId = vedtaksperiodeId,
        inntektsmeldinger = inntektsmeldinger
    )

    init {
        packet["inntektsmeldinger"].forEach { inntektsmelding ->
            inntektsmeldinger.add(
                inntektsmeldingReplay(
                    MeldingsreferanseId(inntektsmelding.path("internDokumentId").asText().toUUID()),
                    inntektsmelding.path("inntektsmelding")
                )
            )
        }
    }

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, inntektsmeldingerReplay, context)
    }

    private fun inntektsmeldingReplay(internDokumentId: MeldingsreferanseId, packet: JsonNode): Inntektsmelding {
        val refusjon = Inntektsmelding.Refusjon(
            beløp = packet.path("refusjon").path("beloepPrMnd").takeUnless(JsonNode::isMissingOrNull)?.asDouble()?.månedlig,
            opphørsdato = packet.path("refusjon").path("opphoersdato").asOptionalLocalDate(),
            endringerIRefusjon = packet["endringIRefusjoner"].map {
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
        val opphørAvNaturalytelser = packet.path("opphoerAvNaturalytelser").tilOpphørAvNaturalytelser()

        return Inntektsmelding(
            meldingsreferanseId = internDokumentId,
            refusjon = refusjon,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(
                organisasjonsnummer = orgnummer
            ),
            beregnetInntekt = beregnetInntekt.månedlig,
            arbeidsgiverperioder = arbeidsgiverperioder,
            begrunnelseForReduksjonEllerIkkeUtbetalt = fraInnteksmelding(begrunnelseForReduksjonEllerIkkeUtbetalt),
            opphørAvNaturalytelser = opphørAvNaturalytelser,
            førsteFraværsdag = førsteFraværsdag,
            mottatt = mottatt
        )
    }
}
