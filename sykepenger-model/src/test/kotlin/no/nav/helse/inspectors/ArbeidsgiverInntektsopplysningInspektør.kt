package no.nav.helse.inspectors

import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning

internal val ArbeidsgiverInntektsopplysning.inspektør get() = ArbeidsgiverInntektsopplysningInspektør(this)

internal class ArbeidsgiverInntektsopplysningInspektør(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning) {
    val orgnummer = arbeidsgiverInntektsopplysning.orgnummer
    val inntektsopplysning = arbeidsgiverInntektsopplysning.inntektsopplysning
    val gjelder = arbeidsgiverInntektsopplysning.gjelder
    val refusjonsopplysninger = arbeidsgiverInntektsopplysning.refusjonsopplysninger.validerteRefusjonsopplysninger
}