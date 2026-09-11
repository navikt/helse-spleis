package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.økonomi.Inntekt

internal val Inntektsgrunnlag.inspektør get() = InntektsgrunnlagInspektør(this)

internal class InntektsgrunnlagInspektør(inntektsgrunnlag: Inntektsgrunnlag) {
    val sykepengegrunnlag: Inntekt = inntektsgrunnlag.sykepengegrunnlag
    val beregningsgrunnlag = inntektsgrunnlag.beregningsgrunnlag
    val omregnetÅrsinntekt = inntektsgrunnlag.omregnetÅrsinntekt
    val `6G` = inntektsgrunnlag.`6G`
    val deaktiverteArbeidsforhold = inntektsgrunnlag.deaktiverteArbeidsforhold.map { it.orgnummer }
    val arbeidsgiverInntektsopplysningerPerArbeidsgiver = inntektsgrunnlag.arbeidsgiverInntektsopplysninger.associateBy { it.orgnummer }
    val arbeidsgiverInntektsopplysninger = inntektsgrunnlag.arbeidsgiverInntektsopplysninger
}
