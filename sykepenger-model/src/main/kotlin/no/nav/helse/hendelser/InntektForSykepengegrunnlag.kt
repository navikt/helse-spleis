package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder

data class InntektForSykepengegrunnlag(val inntekter: List<ArbeidsgiverInntekt>) {
    init {
        require(inntekter.antallMåneder() <= 3L) { "Forventer maks 3 inntektsmåneder" }
    }
}