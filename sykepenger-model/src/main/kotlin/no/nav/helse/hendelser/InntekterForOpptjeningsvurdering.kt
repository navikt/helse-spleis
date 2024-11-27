package no.nav.helse.hendelser

import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.antallMåneder
import no.nav.helse.hendelser.ArbeidsgiverInntekt.Companion.harInntektI
import java.time.YearMonth

class InntekterForOpptjeningsvurdering(
    private val inntekter: List<ArbeidsgiverInntekt>,
) {
    init {
        require(inntekter.antallMåneder() <= 1) { "Forventer maks 1 inntektsmåned" }
    }

    internal fun harInntektI(måned: YearMonth): Boolean = inntekter.harInntektI(måned)
}
