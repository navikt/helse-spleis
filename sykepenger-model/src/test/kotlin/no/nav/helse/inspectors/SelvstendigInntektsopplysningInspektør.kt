package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.SelvstendigInntektsopplysning

internal val SelvstendigInntektsopplysning.inspektør get() = SelvstendigInntektsopplysningInspektør(this)

internal class SelvstendigInntektsopplysningInspektør(arbeidsgiverInntektsopplysning: SelvstendigInntektsopplysning) {
    val faktaavklartInntekt = arbeidsgiverInntektsopplysning.faktaavklartInntekt
    val omregnetÅrsinntekt = arbeidsgiverInntektsopplysning.inntektsgrunnlag.beløp
    val fastsattÅrsinntekt = arbeidsgiverInntektsopplysning.fastsattÅrsinntekt
    val skjønnsmessigFastsatt = arbeidsgiverInntektsopplysning.skjønnsmessigFastsatt
}
