package no.nav.helse.hendelser

import java.time.YearMonth
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.harInntektI

class InntekterForOpptjeningsvurdering(
    private val inntekter: List<ArbeidsgiverInntekt>,
) {
    init {
        require(inntekter.antallMåneder() <= 1) { "Forventer maks 1 inntektsmåned" }
    }

    internal fun harInntektI(måned: YearMonth): Boolean {
        return inntekter.harInntektI(måned)
    }
}
