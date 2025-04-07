package no.nav.helse.spleis.meldinger.model

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlagForArbeidsgiver
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing

internal class SykepengegrunnlagForArbeidsgiverMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {

    private val organisasjonsnummer = packet["organisasjonsnummer"].asText()

    private val inntekterForSykepengegrunnlag = VilkårsgrunnlagMessage.mapSkatteopplysninger(packet["@løsning.${InntekterForSykepengegrunnlagForArbeidsgiver.name}"])

    private val skjæringstidspunkt = packet["${InntekterForSykepengegrunnlagForArbeidsgiver.name}.skjæringstidspunkt"].asLocalDate()

    private val sykepengegrunnlagForArbeidsgiver
        get() = SykepengegrunnlagForArbeidsgiver(
            meldingsreferanseId = meldingsporing.id,
            skjæringstidspunkt = skjæringstidspunkt,
            orgnummer = organisasjonsnummer,
            inntekter = if (inntekterForSykepengegrunnlag.isEmpty()) ArbeidsgiverInntekt(organisasjonsnummer, emptyList()) else inntekterForSykepengegrunnlag.single()
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, sykepengegrunnlagForArbeidsgiver, context)
    }
}
