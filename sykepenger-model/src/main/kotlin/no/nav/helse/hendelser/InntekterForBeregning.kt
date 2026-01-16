package no.nav.helse.hendelser

import no.nav.helse.økonomi.Inntekt

data class InntekterForBeregning(val inntektsperioder: List<Inntektsperiode>) {
    data class Inntektsperiode(val inntektskilde: String, val periode: Periode, val beløp: Inntekt)
}
