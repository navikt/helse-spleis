package no.nav.helse.person

import no.nav.helse.økonomi.Inntekt.Companion.summer

internal class ArbeidsgiverInntektsopplysning(private val orgnummer: String, private val inntektsopplysning: Inntektshistorikk.Inntektsopplysning) {
    companion object {
        internal fun List<ArbeidsgiverInntektsopplysning>.inntekt() = map { it.inntektsopplysning.grunnlagForSykepengegrunnlag() }.summer()
        internal fun List<ArbeidsgiverInntektsopplysning>.inntektsopplysningPerArbeidsgiver() = associate { it.orgnummer to it.inntektsopplysning }
    }
}
