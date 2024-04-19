package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplay
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.toUUID
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.meldinger.model.InntektsmeldingMessage.Companion.tilAvsendersystem
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

// Understands a JSON message representing an Inntektsmelding replay
internal class InntektsmeldingerReplayMessage(packet: JsonMessage) : HendelseMessage(packet) {
    private val aktørId = packet["aktørId"].asText()
    override val fødselsnummer: String = packet["fødselsnummer"].asText()
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())
    override val skalDuplikatsjekkes = false

    private val aktivitetslogg = Aktivitetslogg()

    private val inntektsmeldinger = packet["inntektsmeldinger"].map { inntektsmelding -> inntektsmeldingReplay(
        inntektsmelding.path("internDokumentId").asText().toUUID(),
        inntektsmelding.path("inntektsmelding")
    ) }

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, InntektsmeldingerReplay(
            meldingsreferanseId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            aktivitetslogg = aktivitetslogg,
            vedtaksperiodeId = vedtaksperiodeId,
            inntektsmeldinger = inntektsmeldinger
        ), context)
    }

    private fun inntektsmeldingReplay(internDokumentId: UUID, packet: JsonNode): InntektsmeldingReplay {
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
        val arbeidsforholdId = packet["arbeidsforholdId"].takeIf(JsonNode::isTextual)?.asText()
        val orgnummer = packet["virksomhetsnummer"].asText()
        val aktørId = packet["arbeidstakerAktorId"].asText()
        val mottatt = packet["mottattDato"].asLocalDateTime()
        val førsteFraværsdag = packet["foersteFravaersdag"].asOptionalLocalDate()
        val beregnetInntekt = packet["beregnetInntekt"].asDouble()
        val arbeidsgiverperioder = packet["arbeidsgiverperioder"].map(::asPeriode)
        val begrunnelseForReduksjonEllerIkkeUtbetalt = packet["begrunnelseForReduksjonEllerIkkeUtbetalt"].takeIf(JsonNode::isTextual)?.asText()
        val harOpphørAvNaturalytelser = packet["opphoerAvNaturalytelser"].size() > 0
        val harFlereInntektsmeldinger = packet["harFlereInntektsmeldinger"].asBoolean(false)
        val avsendersystem = packet["avsenderSystem"].tilAvsendersystem()
        val inntektsdato = packet["inntektsdato"].asOptionalLocalDate()

        return InntektsmeldingReplay(
            wrapped = Inntektsmelding(
                meldingsreferanseId = internDokumentId,
                refusjon = refusjon,
                orgnummer = orgnummer,
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                førsteFraværsdag = førsteFraværsdag,
                inntektsdato = inntektsdato,
                beregnetInntekt = beregnetInntekt.månedlig,
                arbeidsgiverperioder = arbeidsgiverperioder,
                arbeidsforholdId = arbeidsforholdId,
                begrunnelseForReduksjonEllerIkkeUtbetalt = begrunnelseForReduksjonEllerIkkeUtbetalt,
                harOpphørAvNaturalytelser = harOpphørAvNaturalytelser,
                harFlereInntektsmeldinger = harFlereInntektsmeldinger,
                avsendersystem = avsendersystem,
                mottatt = mottatt,
                aktivitetslogg = aktivitetslogg
            ),
            vedtaksperiodeId = vedtaksperiodeId,
            innsendt = mottatt,
            registrert = opprettet
        )
    }
}
