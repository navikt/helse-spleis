package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.Arbeidsforhold
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Utbetalingsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.ArbeidsforholdV2
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.InntekterForSykepengegrunnlag
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import no.nav.helse.rapids_rivers.asYearMonth
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage.Companion.arbeidsgiver
import no.nav.helse.spleis.meldinger.model.VilkårsgrunnlagMessage.Companion.asInntekttype
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.util.*

internal class UtbetalingsgrunnlagMessage(packet: JsonMessage) : BehovMessage(packet) {
    private val aktørId = packet["aktørId"].asText()
    private val orgnummer = packet["organisasjonsnummer"].asText()
    private val vedtaksperiodeId = UUID.fromString(packet["vedtaksperiodeId"].asText())

    private val inntekterForSykepengegrunnlag = packet["@løsning.${InntekterForSykepengegrunnlag.name}"]
        .flatMap { måned ->
            måned["inntektsliste"]
                .groupBy({ inntekt -> inntekt.arbeidsgiver() }) { inntekt ->
                    ArbeidsgiverInntekt.MånedligInntekt.Sykepengegrunnlag(
                        yearMonth = måned["årMåned"].asYearMonth(),
                        inntekt = inntekt["beløp"].asDouble().månedlig,
                        type = inntekt["inntektstype"].asInntekttype(),
                        fordel = if (inntekt.path("fordel").isTextual) inntekt["fordel"].asText() else "",
                        beskrivelse = if (inntekt.path("beskrivelse").isTextual) inntekt["beskrivelse"].asText() else ""
                    )
                }.toList()
        }
        .groupBy({ (arbeidsgiver, _) -> arbeidsgiver }) { (_, inntekter) -> inntekter }
        .map { (arbeidsgiver, inntekter) ->
            ArbeidsgiverInntekt(arbeidsgiver, inntekter.flatten())
        }

    private val arbeidsforhold = packet["@løsning.${ArbeidsforholdV2.name}"]
        .map {
            Arbeidsforhold(
                orgnummer = it["orgnummer"].asText(),
                fom = it["ansattSiden"].asLocalDate(),
                tom = it["ansattTil"].asOptionalLocalDate()
            )
         }

    override fun behandle(mediator: IHendelseMediator) {
        mediator.behandle(this, utbetalingsgrunnlag)
    }

    private val utbetalingsgrunnlag
        get() = Utbetalingsgrunnlag(
            meldingsreferanseId = this.id,
            aktørId = aktørId,
            orgnummer = orgnummer,
            fødselsnummer = fødselsnummer,
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(inntekter = inntekterForSykepengegrunnlag, arbeidsforhold = emptyList()),
            vedtaksperiodeId = vedtaksperiodeId,
            arbeidsforhold = arbeidsforhold
        )
}
