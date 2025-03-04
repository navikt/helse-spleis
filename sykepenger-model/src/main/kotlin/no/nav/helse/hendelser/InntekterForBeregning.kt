package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.økonomi.Inntekt

class InntekterForBeregning(val inntektsperioder: List<Inntektsperiode>) {

    data class Inntektsperiode(val inntektskilde: String, val fom: LocalDate, val tom: LocalDate?, val inntekt: Inntekt) {}
}
