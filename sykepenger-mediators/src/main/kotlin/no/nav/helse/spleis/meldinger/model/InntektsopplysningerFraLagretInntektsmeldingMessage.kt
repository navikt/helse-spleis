package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.InntektsopplysningerFraLagretInnteksmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class InntektsopplysningerFraLagretInntektsmeldingMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().toUUID()
    private val inntektsmeldingMeldingsreferanseId = MeldingsreferanseId(packet["inntektsmeldingMeldingsreferanseId"].asText().toUUID())
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val inntektsmeldingOrganisasjonsnummer = packet["inntektsmeldingOrganisasjonsnummer"].takeUnless { it.isMissingOrNull() }?.asText() ?: organisasjonsnummer

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, inntektsmeldingMeldingsreferanseId, context)
    }

    internal fun inntektsopplysningerFraLagretInntektsmelding(inntektsmeldingJson: String): InntektsopplysningerFraLagretInnteksmelding? {
        val inntektsmeldingJsonNode = objectmapper.readTree(inntektsmeldingJson)

        if (inntektsmeldingJsonNode.path("virksomhetsnummer").asText() != inntektsmeldingOrganisasjonsnummer) return null

        return InntektsopplysningerFraLagretInnteksmelding(
            meldingsreferanseId = meldingsporing.id,
            behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
            vedtaksperiodeId = vedtaksperiodeId,
            inntektsmeldingMeldingsreferanseId = inntektsmeldingMeldingsreferanseId,
            inntekt = inntektsmeldingJsonNode.path("beregnetInntekt").asDouble().månedlig,
            refusjon = inntektsmeldingJsonNode.path("refusjon").path("beloepPrMnd").asDouble().månedlig,
            inntektsmeldingMottatt = inntektsmeldingJsonNode.path("mottattDato").asLocalDateTime()
        )
    }

    private companion object {
        private val objectmapper = jacksonObjectMapper()
    }
}
