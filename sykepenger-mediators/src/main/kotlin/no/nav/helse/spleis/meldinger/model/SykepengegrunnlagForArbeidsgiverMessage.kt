package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class SykepengegrunnlagForArbeidsgiverMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText().let { UUID.fromString(it) }
    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()

    private val inntekterForSykepengegrunnlag = VilkårsgrunnlagMessage.mapSkatteopplysninger(packet["@løsning.${InntekterForSykepengegrunnlagForArbeidsgiver.name}"])

    private val skjæringstidspunkter = packet["${InntekterForSykepengegrunnlagForArbeidsgiver.name}.skjæringstidspunkt"].asLocalDate()

    private val sykepengegrunnlagForArbeidsgiver get() = SykepengegrunnlagForArbeidsgiver(
        meldingsreferanseId = meldingsporing.id,
        vedtaksperiodeId = vedtaksperiodeId,
        skjæringstidspunkt = skjæringstidspunkter,
        aktørId = meldingsporing.aktørId,
        personidentifikator = Personidentifikator(meldingsporing.fødselsnummer),
        orgnummer = organisasjonsnummer,
        inntekter = inntekterForSykepengegrunnlag.single()
    )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, sykepengegrunnlagForArbeidsgiver, context)
    }
}
