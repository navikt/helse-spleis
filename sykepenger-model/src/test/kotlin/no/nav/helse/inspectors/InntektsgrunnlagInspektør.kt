package no.nav.helse.inspectors

import no.nav.helse.person.aktivitetslogg.UtbetalingInntektskilde
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.InntektsgrunnlagView
import no.nav.helse.økonomi.Inntekt

internal val Inntektsgrunnlag.inspektør get() = view().inspektør
internal val InntektsgrunnlagView.inspektør get() = InntektsgrunnlagInspektør(this)

internal class InntektsgrunnlagInspektør(view: InntektsgrunnlagView) {
    val minsteinntekt: Inntekt = view.minsteinntekt
    val oppfyllerMinsteinntektskrav = view.oppfyllerMinsteinntektskrav
    val sykepengegrunnlag: Inntekt = view.sykepengegrunnlag
    val beregningsgrunnlag = view.beregningsgrunnlag
    val omregnetÅrsinntekt = view.omregnetÅrsinntekt
    val `6G` = view.`6G`
    val deaktiverteArbeidsforhold = view.deaktiverteArbeidsforhold
    val arbeidsgiverInntektsopplysningerPerArbeidsgiver = view.arbeidsgiverInntektsopplysninger.associateBy { it.orgnummer }
    val inntektskilde = if (view.arbeidsgiverInntektsopplysninger.size > 1) UtbetalingInntektskilde.FLERE_ARBEIDSGIVERE else UtbetalingInntektskilde.EN_ARBEIDSGIVER
    val arbeidsgiverInntektsopplysninger = view.arbeidsgiverInntektsopplysninger
}
