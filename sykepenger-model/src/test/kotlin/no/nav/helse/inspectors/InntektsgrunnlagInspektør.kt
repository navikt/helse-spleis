package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.inspectors.view.InntektsgrunnlagView
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.inspectors.view.view

internal val Inntektsgrunnlag.inspektør get() = view().inspektør
internal val InntektsgrunnlagView.inspektør get() = InntektsgrunnlagInspektør(this)

internal class InntektsgrunnlagInspektør(view: InntektsgrunnlagView) {
    val sykepengegrunnlag: Inntekt = view.sykepengegrunnlag
    val beregningsgrunnlag = view.beregningsgrunnlag
    val omregnetÅrsinntekt = view.omregnetÅrsinntekt
    val `6G` = view.`6G`
    val deaktiverteArbeidsforhold = view.deaktiverteArbeidsforhold
    val arbeidsgiverInntektsopplysningerPerArbeidsgiver = view.arbeidsgiverInntektsopplysninger.associateBy { it.orgnummer }
    val arbeidsgiverInntektsopplysninger = view.arbeidsgiverInntektsopplysninger
}
