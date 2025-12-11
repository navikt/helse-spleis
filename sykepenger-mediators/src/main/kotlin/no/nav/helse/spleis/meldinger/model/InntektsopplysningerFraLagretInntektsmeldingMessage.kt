package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.InntektsopplysningerFraLagretInnteksmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class InntektsopplysningerFraLagretInntektsmeldingMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {
    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().toUUID()
    private val inntektsmeldingMeldingsreferanseId = MeldingsreferanseId(packet["inntektsmeldingMeldingsreferanseId"].asText().toUUID())
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()
    private val inntektsmeldingOrganisasjonsnummer = packet["inntektsmeldingOrganisasjonsnummer"].takeUnless { it.isMissingOrNull() }?.asText() ?: organisasjonsnummer

    private val builder get() = InntektsopplysningerFraLagretInnteksmelding.Builder(
        meldingsreferanseId = meldingsporing.id,
        behandlingsporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer),
        vedtaksperiodeId = vedtaksperiodeId,
        inntektsmeldingMeldingsreferanseId = inntektsmeldingMeldingsreferanseId,
        inntektsmeldingOrganisasjonsnummer = inntektsmeldingOrganisasjonsnummer
    )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, builder, context)
    }
}
