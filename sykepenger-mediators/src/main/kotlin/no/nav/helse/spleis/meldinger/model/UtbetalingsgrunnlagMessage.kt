package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Utbetalingsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage.Companion.inntekter

internal class UtbetalingsgrunnlagMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val aktørId = packet["aktørId"].asText()
    private val orgnummer = packet["organisasjonsnummer"].asText()

    private val inntekterForSykepengegrunnlag = inntekter(InntekterForSykepengegrunnlag, packet)

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingsgrunnlag)
    }

    private val utbetalingsgrunnlag
        get() = Utbetalingsgrunnlag(
            meldingsreferanseId = this.id,
            aktørId = aktørId,
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekterForSykepengegrunnlag)
        )
}
